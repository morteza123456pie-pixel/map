package com.aimaps.app.data.location

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import androidx.core.os.CancellationSignal
import com.aimaps.app.domain.model.LocationServiceStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [LocationDataSource] implemented directly on the platform [LocationManager].
 *
 * Google's fused provider is intentionally not used: it would add a Play Services
 * dependency that is unavailable or unreliable on a large share of devices, and the
 * platform manager is sufficient for the foreground, city-scale use of Phase 1. If a
 * fused provider is wanted later it only has to implement [LocationDataSource].
 */
@Singleton
class AndroidLocationDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationDataSource {

    private val locationManager: LocationManager? =
        ContextCompat.getSystemService(context, LocationManager::class.java)

    override fun hasLocationPermission(): Boolean =
        isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    override fun isLocationServiceEnabled(): Boolean =
        locationManager?.let(LocationManagerCompat::isLocationEnabled) ?: false

    override fun observeLocationServiceStatus(): Flow<LocationServiceStatus> =
        if (locationManager == null) flowOf(LocationServiceStatus.UNKNOWN) else serviceStatusFlow()

    override suspend fun awaitCurrentLocation(): Location {
        val manager = locationManager ?: throw LocationDataSourceException.NoFix()
        if (!isLocationServiceEnabled()) throw LocationDataSourceException.ServicesDisabled()
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission has not been granted")
        }

        val provider = preferredProvider(manager)
        val cancellationSignal = CancellationSignal()

        return try {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { cancellationSignal.cancel() }

                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    if (!continuation.isActive) return@getCurrentLocation

                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        continuation.resumeWithException(LocationDataSourceException.NoFix())
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            // Must propagate untouched, otherwise structured concurrency breaks.
            throw cancellation
        } catch (security: SecurityException) {
            throw security
        } catch (expected: LocationDataSourceException) {
            throw expected
        } catch (unexpected: Exception) {
            throw LocationDataSourceException.NoFix()
        }
    }

    override fun locationUpdates(): Flow<Location> = callbackFlow {
        val manager = locationManager ?: throw LocationDataSourceException.NoFix()
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission has not been granted")
        }

        val provider = preferredProvider(manager)

        val listener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }
        }

        LocationManagerCompat.requestLocationUpdates(
            manager,
            provider,
            LocationRequestCompat.Builder(UPDATE_INTERVAL_MILLIS)
                .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MILLIS)
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .build(),
            ContextCompat.getMainExecutor(context),
            listener,
        )

        awaitClose { LocationManagerCompat.removeUpdates(manager, listener) }
    }

    private fun serviceStatusFlow(): Flow<LocationServiceStatus> = callbackFlow {
        trySend(currentServiceStatus())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                trySend(currentServiceStatus())
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }.distinctUntilChanged()

    private fun currentServiceStatus(): LocationServiceStatus =
        if (isLocationServiceEnabled()) LocationServiceStatus.ENABLED else LocationServiceStatus.DISABLED

    private fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Picks the best provider that is actually enabled on this device.
     *
     * The platform fused provider only exists from API 31, and asking for a provider the
     * device does not expose throws, so the candidates are probed in preference order.
     */
    private fun preferredProvider(manager: LocationManager): String {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }

        val enabledProviders = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())

        return candidates.firstOrNull { it in enabledProviders }
            ?: throw LocationDataSourceException.NoFix()
    }

    private companion object {
        /**
         * A 5 s cadence with a 2 s floor keeps the blue dot responsive at street-level zoom
         * without the battery cost of continuous high-rate GPS polling.
         */
        const val UPDATE_INTERVAL_MILLIS = 5_000L
        const val MIN_UPDATE_INTERVAL_MILLIS = 2_000L
    }
}
