package com.aimaps.app.data.location

import android.location.Location
import com.aimaps.app.domain.model.LocationServiceStatus
import kotlinx.coroutines.flow.Flow

/**
 * Platform-facing location API.
 *
 * Kept separate from [com.aimaps.app.domain.repository.LocationRepository] for two
 * reasons: the repository speaks in `AppResult` and never throws, while this layer works
 * with raw platform types; and swapping the implementation (for example to a fused
 * provider backed by Play Services) then only requires a new binding in the DI module.
 */
interface LocationDataSource {

    /** True when the app currently holds a usable location permission. */
    fun hasLocationPermission(): Boolean

    /** Synchronous read of the device location switch. */
    fun isLocationServiceEnabled(): Boolean

    /** Emits on every change of the device location switch. */
    fun observeLocationServiceStatus(): Flow<LocationServiceStatus>

    /**
     * Suspends until a fix arrives. Throws [LocationDataSourceException] when no provider
     * can satisfy the request, or [SecurityException] when permission is missing.
     */
    suspend fun awaitCurrentLocation(): Location

    /** Continuous fixes from the best available provider. */
    fun locationUpdates(): Flow<Location>
}
