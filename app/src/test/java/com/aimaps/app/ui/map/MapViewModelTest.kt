package com.aimaps.app.ui.map

import app.cash.turbine.test
import com.aimaps.app.core.common.AppError
import com.aimaps.app.core.common.AppResult
import com.aimaps.app.domain.model.CameraPosition
import com.aimaps.app.domain.model.GeoPoint
import com.aimaps.app.domain.model.LocationServiceStatus
import com.aimaps.app.domain.model.MapCameraCommand
import com.aimaps.app.domain.model.PermissionStatus
import com.aimaps.app.domain.model.UserLocation
import com.aimaps.app.map.MapError
import com.aimaps.app.testing.FakeLocationRepository
import com.aimaps.app.testing.FakeNetworkMonitor
import com.aimaps.app.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeLocationRepository()
    private val networkMonitor = FakeNetworkMonitor()

    private fun viewModel() = MapViewModel(repository, networkMonitor)

    // --- Initial state ------------------------------------------------------------

    @Test
    fun `starts with no location and an overview camera`() = runTest(mainDispatcherRule.dispatcher) {
        val state = viewModel().uiState.value

        assertNull(state.userLocation)
        assertFalse(state.showUserLocation)
        assertEquals(GeoPoint.DEFAULT, state.camera.target)
        assertEquals(CameraPosition.OVERVIEW_ZOOM, state.camera.zoom, 0.0)
    }

    @Test
    fun `an ungranted permission is not reported as denied before it is asked for`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.permissionGranted = false

            // UNKNOWN is what lets the UI ask exactly once; DENIED would suppress the prompt.
            assertEquals(PermissionStatus.UNKNOWN, viewModel().uiState.value.permissionStatus)
        }

    // --- Permission handling ------------------------------------------------------

    @Test
    fun `a one off denial is recoverable and explains itself`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()

        viewModel.onPermissionResult(granted = false, showRationale = true)

        val state = viewModel.uiState.value
        assertEquals(PermissionStatus.DENIED, state.permissionStatus)
        assertEquals(UserMessage.PermissionDenied, state.message)
        assertFalse(state.canRecentre)
    }

    @Test
    fun `a permanent denial points at system settings instead of re-prompting`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            // The system reporting no rationale after a denial means "don't ask again".
            viewModel.onPermissionResult(granted = false, showRationale = false)

            val state = viewModel.uiState.value
            assertEquals(PermissionStatus.PERMANENTLY_DENIED, state.permissionStatus)
            assertEquals(UserMessage.PermissionPermanentlyDenied, state.message)
            assertEquals(MessageAction.OpenAppSettings, state.message?.action)
        }

    @Test
    fun `a granted permission clears the outstanding message`() = runTest(mainDispatcherRule.dispatcher) {
        val fix = fixAt(35.7, 51.4)
        repository.currentLocationResult = AppResult.Success(fix)
        val viewModel = viewModel()

        viewModel.onPermissionResult(granted = false, showRationale = true)
        assertEquals(UserMessage.PermissionDenied, viewModel.uiState.value.message)

        repository.permissionGranted = true
        viewModel.onPermissionResult(granted = true, showRationale = false)

        val state = viewModel.uiState.value
        assertEquals(PermissionStatus.GRANTED, state.permissionStatus)
        assertNull(state.message)
    }

    @Test
    fun `refreshing never downgrades a known denial back to unknown`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            viewModel.onPermissionResult(granted = false, showRationale = false)

            // Simulates returning from the background with the grant still withheld.
            viewModel.refreshPermissionStatus()

            assertEquals(
                PermissionStatus.PERMANENTLY_DENIED,
                viewModel.uiState.value.permissionStatus,
            )
        }

    // --- Recentring ---------------------------------------------------------------

    @Test
    fun `recentring without permission asks for it rather than failing quietly`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.permissionGranted = false
            val viewModel = viewModel()

            viewModel.events.test {
                viewModel.recentreOnUser()

                assertEquals(MapEvent.RequestLocationPermission, awaitItem())
            }

            assertEquals(0, repository.currentLocationRequests)
        }

    @Test
    fun `recentring moves the camera onto the reported fix`() = runTest(mainDispatcherRule.dispatcher) {
        val fix = fixAt(35.7, 51.4)
        repository.permissionGranted = true
        repository.currentLocationResult = AppResult.Success(fix)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.recentreOnUser()

            val event = awaitItem()
            assertTrue("expected a camera move, got $event", event is MapEvent.MoveCamera)

            val command = (event as MapEvent.MoveCamera).command
            assertTrue("expected a MoveTo, got $command", command is MapCameraCommand.MoveTo)

            val position = (command as MapCameraCommand.MoveTo).position
            assertEquals(fix.point, position.target)
            assertEquals(CameraPosition.FOLLOW_ZOOM, position.zoom, 0.0)
        }

        val state = viewModel.uiState.value
        assertEquals(fix, state.userLocation)
        assertTrue(state.showUserLocation)
        assertFalse("the spinner must stop once the fix arrives", state.isLocatingUser)
    }

    @Test
    fun `recentring with location services off explains the switch instead of hanging`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.permissionGranted = true
            repository.serviceEnabled = false
            val viewModel = viewModel()

            viewModel.recentreOnUser()

            val state = viewModel.uiState.value
            assertEquals(UserMessage.LocationServicesDisabled, state.message)
            assertEquals(MessageAction.OpenLocationSettings, state.message?.action)
            assertEquals(
                "the repository should not be asked for a fix it cannot produce",
                0,
                repository.currentLocationRequests,
            )
        }

    @Test
    fun `a failed fix surfaces the matching message and stops the spinner`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.permissionGranted = true
            repository.currentLocationResult = AppResult.Failure(AppError.Timeout)
            val viewModel = viewModel()

            viewModel.recentreOnUser()

            val state = viewModel.uiState.value
            assertEquals(UserMessage.LocationTimedOut, state.message)
            assertFalse(state.isLocatingUser)
            assertNull(state.userLocation)
        }

    @Test
    fun `an unobtainable fix is reported without crashing`() = runTest(mainDispatcherRule.dispatcher) {
        repository.permissionGranted = true
        repository.currentLocationResult = AppResult.Failure(AppError.Unavailable)
        val viewModel = viewModel()

        viewModel.recentreOnUser()

        assertEquals(UserMessage.LocationUnavailable, viewModel.uiState.value.message)
    }

    // --- Location stream ----------------------------------------------------------

    @Test
    fun `the camera follows the first fix only, then leaves the user in control`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.permissionGranted = true
            val viewModel = viewModel()

            viewModel.events.test {
                repository.updates.emit(AppResult.Success(fixAt(35.7, 51.4)))

                val event = awaitItem()
                assertTrue("expected a camera move, got $event", event is MapEvent.MoveCamera)

                val position = ((event as MapEvent.MoveCamera).command as MapCameraCommand.MoveTo)
                    .position
                assertEquals(CameraPosition.DEFAULT_ZOOM, position.zoom, 0.0)

                val second = fixAt(35.8, 51.5)
                repository.updates.emit(AppResult.Success(second))

                expectNoEvents()
                assertEquals(second, viewModel.uiState.value.userLocation)
            }
        }

    @Test
    fun `a failed update keeps the last known position on screen`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.permissionGranted = true
            val viewModel = viewModel()
            val fix = fixAt(35.7, 51.4)

            repository.updates.emit(AppResult.Success(fix))
            repository.updates.emit(AppResult.Failure(AppError.Unavailable))

            val state = viewModel.uiState.value
            assertEquals("a stale dot beats no dot at all", fix, state.userLocation)
            assertEquals(UserMessage.LocationUnavailable, state.message)
        }

    @Test
    fun `turning location services off then on clears its own message`() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.permissionGranted = true
            val viewModel = viewModel()

            repository.serviceStatus.value = LocationServiceStatus.DISABLED
            assertEquals(UserMessage.LocationServicesDisabled, viewModel.uiState.value.message)
            assertFalse(viewModel.uiState.value.canRecentre)

            repository.serviceStatus.value = LocationServiceStatus.ENABLED

            assertNull(viewModel.uiState.value.message)
            assertTrue(viewModel.uiState.value.canRecentre)
        }

    // --- Map and connectivity -----------------------------------------------------

    @Test
    fun `unreachable map tiles are reported and then cleared on recovery`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.onMapError(MapError.StyleUnavailable)
            assertEquals(UserMessage.MapStyleUnavailable, viewModel.uiState.value.message)

            viewModel.onMapError(null)
            assertNull(viewModel.uiState.value.message)
        }

    @Test
    fun `clearing a map error leaves an unrelated message alone`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            viewModel.onPermissionResult(granted = false, showRationale = true)

            viewModel.onMapError(null)

            assertEquals(UserMessage.PermissionDenied, viewModel.uiState.value.message)
        }

    @Test
    fun `losing connectivity raises the offline flag`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        assertFalse(viewModel.uiState.value.isOffline)

        networkMonitor.online.value = false
        assertTrue(viewModel.uiState.value.isOffline)

        networkMonitor.online.value = true
        assertFalse(viewModel.uiState.value.isOffline)
    }

    // --- Search bar ---------------------------------------------------------------

    @Test
    fun `the search query is held in state and can be cleared`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()

            viewModel.onSearchQueryChanged("Tehran")
            assertEquals("Tehran", viewModel.uiState.value.searchQuery)

            viewModel.onSearchQueryCleared()
            assertEquals("", viewModel.uiState.value.searchQuery)
        }

    // --- Camera persistence -------------------------------------------------------

    @Test
    fun `the settled camera is recorded so it can survive recreation`() =
        runTest(mainDispatcherRule.dispatcher) {
            val viewModel = viewModel()
            val camera = CameraPosition(target = GeoPoint(48.8566, 2.3522), zoom = 17.25)

            viewModel.onCameraChanged(camera)

            assertEquals(camera, viewModel.uiState.value.camera)
        }

    @Test
    fun `dismissing a message removes it`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = viewModel()
        viewModel.onPermissionResult(granted = false, showRationale = true)

        viewModel.onMessageDismissed()

        assertNull(viewModel.uiState.value.message)
    }

    private fun fixAt(latitude: Double, longitude: Double) = UserLocation(
        point = GeoPoint(latitude, longitude),
        accuracyMetres = 12f,
        bearingDegrees = null,
    )
}
