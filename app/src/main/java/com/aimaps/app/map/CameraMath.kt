package com.aimaps.app.map

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

/**
 * Web Mercator helpers.
 *
 * MapLibre expresses circle radii in density-independent pixels, so converting a real-world
 * accuracy radius (metres) into something drawn on screen requires the map's current scale.
 * The formula is the standard Web Mercator resolution at a given latitude and zoom for a
 * 256 px tile grid.
 */
internal object CameraMath {

    private const val EARTH_CIRCUMFERENCE_METRES = 40_075_016.686
    private const val TILE_SIZE_PX = 256.0

    /** Ground distance covered by one screen pixel at [latitudeDegrees] and [zoom]. */
    fun metresPerPixel(latitudeDegrees: Double, zoom: Double): Double =
        EARTH_CIRCUMFERENCE_METRES * cos(latitudeDegrees * PI / 180.0) / (TILE_SIZE_PX * 2.0.pow(zoom))

    /**
     * Converts a horizontal accuracy in metres to an on-screen radius in dp, clamped so a
     * poor indoor fix cannot paint over the whole viewport.
     */
    fun accuracyRadiusDp(accuracyMetres: Float, latitudeDegrees: Double, zoom: Double): Float {
        val scale = metresPerPixel(latitudeDegrees, zoom)
        if (scale <= 0.0 || !scale.isFinite()) return MIN_ACCURACY_RADIUS_DP

        return (accuracyMetres / scale)
            .toFloat()
            .coerceIn(MIN_ACCURACY_RADIUS_DP, MAX_ACCURACY_RADIUS_DP)
    }

    private const val MIN_ACCURACY_RADIUS_DP = 14f
    private const val MAX_ACCURACY_RADIUS_DP = 1_200f
}
