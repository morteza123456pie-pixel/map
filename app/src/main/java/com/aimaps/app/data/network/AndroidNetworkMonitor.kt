package com.aimaps.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/** [NetworkMonitor] backed by the platform [ConnectivityManager]. */
@Singleton
class AndroidNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkMonitor {

    private val connectivityManager: ConnectivityManager? =
        ContextCompat.getSystemService(context, ConnectivityManager::class.java)

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = connectivityManager
        if (manager == null) {
            // No connectivity service at all: report offline rather than crashing, and keep
            // the flow alive so the collector does not have to special-case completion.
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(manager.hasUsableInternet())
            }

            override fun onLost(network: Network) {
                trySend(manager.hasUsableInternet())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(networkCapabilities.hasUsableInternet())
            }
        }

        // Seed the current value so a collector that starts while already offline is told so
        // immediately instead of waiting for the next transition.
        trySend(manager.hasUsableInternet())

        // Registration can throw if the process is being torn down or the caller is
        // restricted; offline is the safe reading in that case.
        val registered = runCatching { manager.registerDefaultNetworkCallback(callback) }
            .isSuccess

        if (!registered) trySend(false)

        awaitClose {
            if (registered) runCatching { manager.unregisterNetworkCallback(callback) }
        }
    }
        .distinctUntilChanged()
        .conflate()

    private fun ConnectivityManager.hasUsableInternet(): Boolean =
        getNetworkCapabilities(activeNetwork)?.hasUsableInternet() ?: false

    /**
     * `VALIDATED` is required as well as `INTERNET`: a captive-portal Wi-Fi network claims
     * the latter while dropping every tile request, which is exactly the case the banner
     * exists to explain.
     */
    private fun NetworkCapabilities.hasUsableInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
