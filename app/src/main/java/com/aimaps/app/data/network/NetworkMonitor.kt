package com.aimaps.app.data.network

import kotlinx.coroutines.flow.Flow

/**
 * Reports whether the device currently has usable internet access.
 *
 * MapLibre caches tiles, so a dropped connection shows up as blank patches rather than an
 * error. Knowing about it up front lets the UI explain the blanks instead of leaving the
 * user staring at a grey grid.
 *
 * An interface rather than a concrete class so the map ViewModel can be exercised on the
 * JVM without a `Context`, mirroring the
 * [com.aimaps.app.data.location.LocationDataSource] pattern.
 */
interface NetworkMonitor {

    /** Emits the current state immediately on collection, then on every change. */
    val isOnline: Flow<Boolean>
}
