package com.aimaps.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aimaps.app.core.common.AppError
import com.aimaps.app.core.common.AppResult
import com.aimaps.app.data.network.NetworkMonitor
import com.aimaps.app.domain.model.CameraPosition
import com.aimaps.app.domain.model.LocationServiceStatus
import com.aimaps.app.domain.model.MapCameraCommand
import com.aimaps.app.domain.model.PermissionStatus
import com.aimaps.app.domain.repository.LocationRepository
import com.aimaps.app.map.MapError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns all state for the map screen.
 *
 * Deliberately free of Android and MapLibre types: the composable resolves
 * [UserMessage]s to strings and owns the permission launcher, which keeps this class
 * unit-testable on the JVM.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** Buffered so an event emitted before the UI subscribes is still delivered. */
    private val eventChannel = Channel<MapEvent>(Channel.BUFFERED)
    val events: Flow<MapEvent> = eventChannel.receiveAsFlow()

    private var locationUpdatesJob: Job? = null

    /** The camera follows the first fix only; after that the user is in charge. */
    private var hasCentredOnFirstFix = false

    init {
        observeLocationServices()
        observeConnectivity(networkMonitor)
        refreshPermissionStatus()
    }

    // --- UI actions ---------------------------------------------------------------

    /** Called when the screen appears and whenever it resumes, since the user may have
     *  changed the grant in system settings while away. */
    fun refreshPermissionStatus() {
        val granted = locationRepository.isPermissionGranted()

        _uiState.update { state ->
            when {
                granted -> state.copy(permissionStatus = PermissionStatus.GRANTED)
                // Never downgrade a known denial to UNKNOWN: that would make the UI
                // re-prompt for a permission the system will silently refuse to show.
                state.permissionStatus == PermissionStatus.UNKNOWN -> state
                else -> state.copy(permissionStatus = PermissionStatus.DENIED)
            }
        }

        if (granted) startLocationUpdates()
    }

    /**
     * Result of the system permission dialog.
     *
     * @param showRationale what the system reports *after* the denial. False following a
     *   denial means the user chose "don't ask again", so only app settings can undo it.
     */
    fun onPermissionResult(granted: Boolean, showRationale: Boolean) {
        if (granted) {
            _uiState.update { it.copy(permissionStatus = PermissionStatus.GRANTED, message = null) }
            startLocationUpdates()
            recentreOnUser()
            return
        }

        val status = if (showRationale) {
            PermissionStatus.DENIED
        } else {
            PermissionStatus.PERMANENTLY_DENIED
        }

        _uiState.update { state ->
            state.copy(
                permissionStatus = status,
                userLocation = null,
                isLocatingUser = false,
                message = if (status == PermissionStatus.PERMANENTLY_DENIED) {
                    UserMessage.PermissionPermanentlyDenied
                } else {
                    UserMessage.PermissionDenied
                },
            )
        }
    }

    /** The recentre button. Prompts for permission when that is what is missing. */
    fun recentreOnUser() {
        val state = _uiState.value

        when {
            !state.permissionStatus.isGranted -> {
                if (state.permissionStatus == PermissionStatus.PERMANENTLY_DENIED) {
                    _uiState.update { it.copy(message = UserMessage.PermissionPermanentlyDenied) }
                } else {
                    viewModelScope.launch { eventChannel.send(MapEvent.RequestLocationPermission) }
                }
            }

            !locationRepository.isLocationServiceEnabled() -> {
                _uiState.update { it.copy(message = UserMessage.LocationServicesDisabled) }
            }

            else -> fetchCurrentLocation(moveCamera = true)
        }
    }

    fun onSearchQueryChanged(query: String) {
        // Phase 1 keeps the query in state only. Phase 2 debounces it here and calls
        // SearchRepository; the UI contract does not change when it does.
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchQueryCleared() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    /** Records the settled camera so it survives a configuration change. */
    fun onCameraChanged(camera: CameraPosition) {
        _uiState.update { it.copy(camera = camera) }
    }

    fun onMapError(error: MapError?) {
        _uiState.update { state ->
            when (error) {
                MapError.StyleUnavailable -> state.copy(message = UserMessage.MapStyleUnavailable)
                MapError.Unknown -> state.copy(message = UserMessage.Unknown)
                null -> if (state.message == UserMessage.MapStyleUnavailable) {
                    state.copy(message = null)
                } else {
                    state
                }
            }
        }
    }

    fun onMessageDismissed() {
        _uiState.update { it.copy(message = null) }
    }

    /** The action button on a retryable message. */
    fun onRetry() {
        _uiState.update { it.copy(message = null) }
        recentreOnUser()
    }

    // --- Internals ----------------------------------------------------------------

    private fun fetchCurrentLocation(moveCamera: Boolean) {
        _uiState.update { it.copy(isLocatingUser = true) }

        viewModelScope.launch {
            when (val result = locationRepository.getCurrentLocation()) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            userLocation = result.data,
                            isLocatingUser = false,
                            message = null,
                        )
                    }

                    if (moveCamera) {
                        hasCentredOnFirstFix = true
                        eventChannel.send(
                            MapEvent.MoveCamera(
                                MapCameraCommand.MoveTo(
                                    CameraPosition.at(result.data.point, CameraPosition.FOLLOW_ZOOM),
                                ),
                            ),
                        )
                    }
                }

                is AppResult.Failure -> _uiState.update { state ->
                    state.copy(isLocatingUser = false, message = result.error.toUserMessage())
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (locationUpdatesJob?.isActive == true) return

        locationUpdatesJob = viewModelScope.launch {
            locationRepository.locationUpdates().collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        _uiState.update { it.copy(userLocation = result.data) }

                        // Centre on the very first fix so the app opens somewhere useful,
                        // then leave the camera alone.
                        if (!hasCentredOnFirstFix) {
                            hasCentredOnFirstFix = true
                            eventChannel.send(
                                MapEvent.MoveCamera(
                                    MapCameraCommand.MoveTo(
                                        CameraPosition.at(result.data.point, CameraPosition.DEFAULT_ZOOM),
                                    ),
                                ),
                            )
                        }
                    }

                    is AppResult.Failure -> _uiState.update { state ->
                        // A failed update stream should not wipe the last known position;
                        // a slightly stale dot beats no dot at all.
                        state.copy(message = result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun observeLocationServices() {
        viewModelScope.launch {
            locationRepository.observeLocationServiceStatus().collect { status ->
                _uiState.update { state ->
                    state.copy(
                        locationServiceStatus = status,
                        message = when {
                            status == LocationServiceStatus.DISABLED && state.permissionStatus.isGranted ->
                                UserMessage.LocationServicesDisabled

                            // Clear the "turn GPS on" prompt once it has been acted upon.
                            status == LocationServiceStatus.ENABLED &&
                                state.message == UserMessage.LocationServicesDisabled -> null

                            else -> state.message
                        },
                    )
                }

                if (status == LocationServiceStatus.ENABLED && _uiState.value.permissionStatus.isGranted) {
                    startLocationUpdates()
                }
            }
        }
    }

    private fun observeConnectivity(networkMonitor: NetworkMonitor) {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
    }

    private fun AppError.toUserMessage(): UserMessage = when (this) {
        AppError.PermissionDenied -> UserMessage.PermissionDenied
        AppError.ServiceDisabled -> UserMessage.LocationServicesDisabled
        AppError.Unavailable -> UserMessage.LocationUnavailable
        AppError.Timeout -> UserMessage.LocationTimedOut
        AppError.NoConnection -> UserMessage.LocationUnavailable
        is AppError.Unexpected -> UserMessage.Unknown
    }
}
