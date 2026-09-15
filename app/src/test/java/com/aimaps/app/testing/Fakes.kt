package com.aimaps.app.testing

import com.aimaps.app.core.common.AppError
import com.aimaps.app.core.common.AppResult
import com.aimaps.app.data.network.NetworkMonitor
import com.aimaps.app.domain.model.LocationServiceStatus
import com.aimaps.app.domain.model.UserLocation
import com.aimaps.app.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-written fake rather than a mocking framework: the interface is small, and a fake
 * keeps the tests readable without adding a mocking dependency to the build.
 */
class FakeLocationRepository : LocationRepository {

    var permissionGranted: Boolean = false
    var serviceEnabled: Boolean = true

    /** What the next one-shot fix resolves to. */
    var currentLocationResult: AppResult<UserLocation> = AppResult.Failure(AppError.Unavailable)

    /** Counts one-shot requests so tests can assert the repository was never even asked. */
    var currentLocationRequests: Int = 0
        private set

    val serviceStatus = MutableStateFlow(LocationServiceStatus.ENABLED)

    /** No replay, so a fix only arrives when a test emits one. */
    val updates = MutableSharedFlow<AppResult<UserLocation>>(extraBufferCapacity = 8)

    override fun isPermissionGranted(): Boolean = permissionGranted

    override fun isLocationServiceEnabled(): Boolean = serviceEnabled

    override fun observeLocationServiceStatus(): Flow<LocationServiceStatus> = serviceStatus

    override suspend fun getCurrentLocation(): AppResult<UserLocation> {
        currentLocationRequests++
        return currentLocationResult
    }

    override fun locationUpdates(): Flow<AppResult<UserLocation>> = updates
}

class FakeNetworkMonitor : NetworkMonitor {

    val online = MutableStateFlow(true)

    override val isOnline: Flow<Boolean> = online
}
