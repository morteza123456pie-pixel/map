package com.aimaps.app.map

import com.aimaps.app.domain.model.CameraPosition
import com.aimaps.app.domain.model.GeoPoint
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.camera.CameraPosition as MapLibreCameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Adapters between MapLibre's callback API and the coroutine/domain world.
 *
 * Confining every MapLibre type to this file and [UserLocationLayer] keeps the rest of the
 * app renderer-agnostic: swapping the map engine would not reach the ViewModel or UI.
 */

internal fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

internal fun LatLng.toGeoPoint(): GeoPoint = GeoPoint.normalized(latitude, longitude)

internal fun CameraPosition.toMapLibre(): MapLibreCameraPosition = MapLibreCameraPosition.Builder()
    .target(target.toLatLng())
    .zoom(zoom)
    .bearing(bearingDegrees)
    .tilt(tiltDegrees)
    .build()

internal fun MapLibreCameraPosition.toDomain(): CameraPosition = CameraPosition(
    target = (target ?: LatLng(0.0, 0.0)).toGeoPoint(),
    zoom = zoom,
    bearingDegrees = bearing,
    tiltDegrees = tilt,
)

/** Suspends until MapLibre hands back the map instance. */
internal suspend fun MapView.awaitMap(): MapLibreMap = suspendCancellableCoroutine { continuation ->
    getMapAsync { map ->
        if (continuation.isActive) continuation.resume(map)
    }
}

/**
 * Applies [styleUrl] and suspends until the style document is parsed.
 *
 * MapLibre only invokes this callback on success. A style that never loads therefore
 * leaves the caller suspended, which is why callers wrap it in a timeout and report
 * [MapError.StyleUnavailable] rather than waiting forever.
 */
internal suspend fun MapLibreMap.awaitStyle(styleUrl: String): Style =
    suspendCancellableCoroutine { continuation ->
        setStyle(Style.Builder().fromUri(styleUrl)) { style ->
            if (continuation.isActive) continuation.resume(style)
        }
    }
