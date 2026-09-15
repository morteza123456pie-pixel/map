package com.aimaps.app.domain.repository

import com.aimaps.app.core.common.AppResult
import com.aimaps.app.domain.model.LocationServiceStatus
import com.aimaps.app.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * The app's single source of truth for the user's position.
 *
 * Implementations translate platform exceptions into
 * [com.aimaps.app.core.common.AppError] values, so callers never have to handle a
 * `SecurityException` or a null provider.
 */
interface LocationRepository {

    /** Whether the app currently holds a usable location permission. */
    fun isPermissionGranted(): Boolean

    /** Whether location services are switched on at the device level. */
    fun isLocationServiceEnabled(): Boolean

    /** Emits on every change of the device location switch. */
    fun observeLocationServiceStatus(): Flow<LocationServiceStatus>

    /** A single fresh fix, or an [com.aimaps.app.core.common.AppError] if one cannot be obtained. */
    suspend fun getCurrentLocation(): AppResult<UserLocation>

    /** Continuous fixes. Errors arrive in-band so the UI can report them and keep working. */
    fun locationUpdates(): Flow<AppResult<UserLocation>>
}
