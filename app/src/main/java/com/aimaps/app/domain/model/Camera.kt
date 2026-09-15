package com.aimaps.app.domain.model

/** A location fix obtained from the platform, already normalised for the UI layer. */
data class UserLocation(
    val point: GeoPoint,
    /** Horizontal accuracy in metres, if the provider reported one. */
    val accuracyMetres: Float?,
    /** Bearing in degrees (0 = north), only set when the device is genuinely moving. */
    val bearingDegrees: Float?,
    /** True when the fix came from a provider reporting speed over ground. */
    val hasSpeed: Boolean = false,
)

/**
 * The map camera. Zoom follows MapLibre's convention where larger values zoom in.
 * [tiltDegrees] and [bearingDegrees] are carried from the start so a later navigation
 * phase can drive a tilted, rotating camera without changing this type.
 */
data class CameraPosition(
    val target: GeoPoint,
    val zoom: Double,
    val bearingDegrees: Double = 0.0,
    val tiltDegrees: Double = 0.0,
) {

    companion object {
        /** Street level: labels and building footprints are legible. */
        const val DEFAULT_ZOOM = 15.0

        /** Zoom applied when recentring on the user, slightly tighter than the default. */
        const val FOLLOW_ZOOM = 16.5

        /** Continent level, used before any location is known. */
        const val OVERVIEW_ZOOM = 11.0

        const val MIN_ZOOM = 2.0
        const val MAX_ZOOM = 20.0

        fun at(point: GeoPoint, zoom: Double = DEFAULT_ZOOM) = CameraPosition(target = point, zoom = zoom)
    }
}

/**
 * One-shot camera instructions.
 *
 * Delivered as events rather than held in state: replaying a "fly to me" animation after
 * every configuration change would fight the user for control of the camera.
 *
 * Later phases extend this with the cases they need — `FitBounds` for a search result set,
 * `FollowUser` for navigation — without touching existing call sites.
 */
sealed interface MapCameraCommand {

    data class MoveTo(val position: CameraPosition, val animated: Boolean = true) : MapCameraCommand
}
