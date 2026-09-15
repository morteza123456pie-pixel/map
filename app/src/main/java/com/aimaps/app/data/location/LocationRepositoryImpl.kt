package com.aimaps.app.data.location

import com.aimaps.app.core.common.AppError
import com.aimaps.app.core.common.AppResult
import com.aimaps.app.core.di.IoDispatcher
import com.aimaps.app.domain.model.LocationServiceStatus
import com.aimaps.app.domain.model.UserLocation
import com.aimaps.app.domain.repository.LocationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * [LocationRepository] backed by [LocationDataSource].
 *
 * The job here is orchestration only: pick the dispatcher, convert platform fixes into
 * [AppResult], and guarantee that no throwable escapes towards the UI layer.
 */
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val dataSource: LocationDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LocationRepository {

    override fun isPermissionGranted(): Boolean = dataSource.hasLocationPermission()

    override fun isLocationServiceEnabled(): Boolean = dataSource.isLocationServiceEnabled()

    override fun observeLocationServiceStatus(): Flow<LocationServiceStatus> =
        dataSource.observeLocationServiceStatus().flowOn(ioDispatcher)

    override suspend fun getCurrentLocation(): AppResult<UserLocation> = withContext(ioDispatcher) {
        if (!dataSource.hasLocationPermission()) {
            return@withContext AppResult.Failure(AppError.PermissionDenied)
        }
        if (!dataSource.isLocationServiceEnabled()) {
            return@withContext AppResult.Failure(AppError.ServiceDisabled)
        }

        runCatching { dataSource.awaitCurrentLocation().toUserLocation() }
            .fold(
                onSuccess = { fix -> AppResult.Success(fix) },
                onFailure = { error -> AppResult.Failure(error.toAppError()) },
            )
    }

    override fun locationUpdates(): Flow<AppResult<UserLocation>> =
        dataSource.locationUpdates()
            .map<_, AppResult<UserLocation>> { location -> AppResult.Success(location.toUserLocation()) }
            .catch { throwable -> emit(AppResult.Failure(throwable.toAppError())) }
            .flowOn(ioDispatcher)

    private fun Throwable.toAppError(): AppError = when (this) {
        is SecurityException -> AppError.PermissionDenied
        is LocationDataSourceException.ServicesDisabled -> AppError.ServiceDisabled
        is LocationDataSourceException.TimedOut -> AppError.Timeout
        is LocationDataSourceException.NoFix -> AppError.Unavailable
        else -> AppError.Unexpected(this)
    }
}

/** Errors the data source raises when it cannot produce a fix. */
sealed class LocationDataSourceException(message: String) : Exception(message) {

    class ServicesDisabled : LocationDataSourceException("Location services are disabled")

    class NoFix : LocationDataSourceException("No location provider returned a fix")

    class TimedOut : LocationDataSourceException("Timed out waiting for a location fix")
}
