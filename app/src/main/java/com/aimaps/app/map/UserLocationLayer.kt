package com.aimaps.app.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.aimaps.app.domain.model.UserLocation
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.maps.Style
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

/**
 * Draws the "you are here" indicator as three stacked circle layers: a translucent
 * accuracy disc, a white contrast ring, and the accent dot.
 *
 * MapLibre's built-in `LocationComponent` is deliberately not used. It owns its own
 * location engine and permission handling, which would duplicate — and could silently
 * contradict — the repository that is already the single source of truth for position.
 * Driving plain style layers keeps that ownership in one place.
 */
internal class UserLocationLayer(private val accentColor: Color) {

    /**
     * Installs the source and layers into [style].
     *
     * Safe to call again after a style reload (a theme switch swaps the whole style
     * document), because MapLibre drops sources and layers along with the old style.
     */
    fun attachTo(style: Style) {
        if (style.getSource(SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(SOURCE_ID))
        }

        if (style.getLayer(ACCURACY_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(ACCURACY_LAYER_ID, SOURCE_ID).withProperties(
                    PropertyFactory.circleColor(accentColor.toArgb()),
                    PropertyFactory.circleOpacity(ACCURACY_FILL_OPACITY),
                    PropertyFactory.circleStrokeColor(accentColor.toArgb()),
                    PropertyFactory.circleStrokeWidth(1f),
                    PropertyFactory.circleStrokeOpacity(ACCURACY_STROKE_OPACITY),
                    PropertyFactory.circleRadius(0f),
                ),
            )
        }

        if (style.getLayer(HALO_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(HALO_LAYER_ID, SOURCE_ID).withProperties(
                    PropertyFactory.circleColor(Color.White.toArgb()),
                    PropertyFactory.circleRadius(DOT_RADIUS + HALO_WIDTH),
                ),
            )
        }

        if (style.getLayer(DOT_LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(DOT_LAYER_ID, SOURCE_ID).withProperties(
                    PropertyFactory.circleColor(accentColor.toArgb()),
                    PropertyFactory.circleRadius(DOT_RADIUS),
                ),
            )
        }
    }

    /**
     * Moves the indicator to [location], or hides it when null.
     *
     * [zoom] is needed because the accuracy disc is a real-world radius that has to be
     * re-scaled whenever the camera zooms.
     */
    fun update(style: Style, location: UserLocation?, zoom: Double) {
        val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return

        if (location == null) {
            source.setGeoJson(EMPTY_FEATURE_COLLECTION)
            return
        }

        source.setGeoJson(
            Feature.fromGeometry(
                // GeoJSON orders coordinates longitude-first.
                Point.fromLngLat(location.point.longitude, location.point.latitude),
            ),
        )

        val accuracyRadius = location.accuracyMetres?.let { accuracy ->
            CameraMath.accuracyRadiusDp(
                accuracyMetres = accuracy,
                latitudeDegrees = location.point.latitude,
                zoom = zoom,
            )
        } ?: 0f

        // Hide the disc entirely when it would sit inside the dot; a 3 px halo around a
        // 7 px dot reads as a rendering artefact rather than as accuracy information.
        val visibleRadius = if (accuracyRadius <= DOT_RADIUS + HALO_WIDTH) 0f else accuracyRadius

        style.getLayerAs<CircleLayer>(ACCURACY_LAYER_ID)
            ?.setProperties(PropertyFactory.circleRadius(visibleRadius))
    }

    private companion object {
        const val SOURCE_ID = "user-location-source"
        const val ACCURACY_LAYER_ID = "user-location-accuracy"
        const val HALO_LAYER_ID = "user-location-halo"
        const val DOT_LAYER_ID = "user-location-dot"

        const val DOT_RADIUS = 7f
        const val HALO_WIDTH = 3f
        const val ACCURACY_FILL_OPACITY = 0.14f
        const val ACCURACY_STROKE_OPACITY = 0.35f

        /** An empty collection clears the source without removing the layers. */
        const val EMPTY_FEATURE_COLLECTION = """{"type":"FeatureCollection","features":[]}"""
    }
}
