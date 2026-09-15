package com.aimaps.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

/**
 * Application entry point.
 *
 * MapLibre requires `getInstance` to run before any `MapView` is inflated; doing it here
 * guarantees the ordering regardless of which screen creates the first map.
 */
@HiltAndroidApp
class AiMapsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // A no-op on repeat calls, so it is safe even if a future phase re-initialises.
        MapLibre.getInstance(this)
    }
}
