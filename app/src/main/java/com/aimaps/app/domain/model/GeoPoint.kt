package com.aimaps.app.domain.model

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A geographic coordinate pair in the WGS84 datum — the coordinate system MapLibre and
 * OpenStreetMap both use.
 *
 * Latitude comes before longitude here, while GeoJSON geometries take them the other way
 * round. Having one named type stops the two doubles from being transposed at call sites.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {

    init {
        require(latitude in LATITUDE_RANGE) { "Latitude must be within $LATITUDE_RANGE, was $latitude" }
        require(longitude in LONGITUDE_RANGE) { "Longitude must be within $LONGITUDE_RANGE, was $longitude" }
    }

    /** Great-circle distance to [other] in metres (haversine, mean Earth radius). */
    fun distanceTo(other: GeoPoint): Double {
        val deltaLat = Math.toRadians(other.latitude - latitude)
        val deltaLon = Math.toRadians(other.longitude - longitude)
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)

        val a = sin(deltaLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(deltaLon / 2).let { it * it }

        return EARTH_RADIUS_METRES * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val EARTH_RADIUS_METRES = 6_371_000.0

        private val LATITUDE_RANGE = -90.0..90.0
        private val LONGITUDE_RANGE = -180.0..180.0

        /** Roughly the centre of Tehran; used only as a fallback before any fix is known. */
        val DEFAULT = GeoPoint(latitude = 35.6892, longitude = 51.3890)

        /**
         * Builds a point from values that may fall outside the valid ranges.
         *
         * The map camera legitimately reports longitudes beyond ±180 once the user pans
         * across the antimeridian, so readings coming back from MapLibre must be wrapped
         * rather than rejected. The strict [init] check is kept for values the app itself
         * constructs, where an out-of-range number is a bug.
         */
        fun normalized(latitude: Double, longitude: Double): GeoPoint = GeoPoint(
            latitude = latitude.coerceIn(LATITUDE_RANGE),
            longitude = wrapLongitude(longitude),
        )

        private fun wrapLongitude(longitude: Double): Double {
            if (longitude in LONGITUDE_RANGE) return longitude
            if (!longitude.isFinite()) return 0.0

            val wrapped = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
            return wrapped.coerceIn(LONGITUDE_RANGE)
        }
    }
}
