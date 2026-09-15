package com.aimaps.app.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aimaps.app.domain.model.CameraPosition
import com.aimaps.app.domain.model.MapCameraCommand
import com.aimaps.app.domain.model.UserLocation
import com.aimaps.app.map.style.MapStyleProvider
import com.aimaps.app.map.style.MapStyleSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Full-screen MapLibre map hosted inside Compose.
 *
 * All MapLibre state stays behind this composable. The caller supplies plain domain values
 * and receives plain domain callbacks, which is what keeps the ViewModel free of Android
 * and MapLibre types and therefore unit-testable on the JVM.
 *
 * @param initialCamera applied once, on first composition. The camera then belongs to the
 *   user; recentring happens only through [cameraCommands].
 * @param cameraCommands one-shot instructions, consumed as events so a recentre animation
 *   is not replayed after a configuration change.
 * @param onCameraChanged reports the settled camera so it can be restored later.
 */
@Composable
fun MapLibreMapView(
    styleSpec: MapStyleSpec,
    initialCamera: CameraPosition,
    userLocation: UserLocation?,
    cameraCommands: Flow<MapCameraCommand>,
    onCameraChanged: (CameraPosition) -> Unit,
    onMapError: (MapError?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapView = rememberMapViewWithLifecycle()

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }

    val userLocationLayer = remember(styleSpec.isDark) {
        UserLocationLayer(accentColor = MapStyleProvider.userLocationColor(styleSpec.isDark))
    }

    // Captured once: re-applying this on every recomposition would yank the camera back
    // while the user is panning.
    val firstCamera = remember { initialCamera }

    // These are read from listeners that outlive individual recompositions, so they are
    // routed through rememberUpdatedState to avoid capturing a stale lambda.
    val currentOnCameraChanged by rememberUpdatedState(onCameraChanged)
    val currentOnMapError by rememberUpdatedState(onMapError)
    val currentUserLocation by rememberUpdatedState(userLocation)
    val currentStyle by rememberUpdatedState(loadedStyle)

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )

    LaunchedEffect(mapView) {
        val map = mapView.awaitMap()

        map.uiSettings.apply {
            // Free panning, pinch/double-tap zoom, rotation and tilt.
            isScrollGesturesEnabled = true
            isZoomGesturesEnabled = true
            isDoubleTapGesturesEnabled = true
            isRotateGesturesEnabled = true
            isTiltGesturesEnabled = true
            isQuickZoomGesturesEnabled = true

            isCompassEnabled = true

            // Attribution is drawn by the Compose overlay instead, so MapLibre's own
            // widgets are switched off to keep the map surface clean.
            isAttributionEnabled = false
            isLogoEnabled = false
        }

        map.setMinZoomPreference(CameraPosition.MIN_ZOOM)
        map.setMaxZoomPreference(CameraPosition.MAX_ZOOM)

        // Position the camera before the style resolves, so the first rendered frame is
        // already in the right place instead of jumping up from [0, 0].
        map.moveCamera(CameraUpdateFactory.newCameraPosition(firstCamera.toMapLibre()))

        mapLibreMap = map
    }

    // Reloading the style is how a light/dark switch is applied. MapLibre discards the old
    // style's sources and layers along with it, so the location layer is re-attached below.
    LaunchedEffect(mapLibreMap, styleSpec.styleUrl) {
        val map = mapLibreMap ?: return@LaunchedEffect
        loadedStyle = null

        val style = withTimeoutOrNull(STYLE_LOAD_TIMEOUT_MILLIS) {
            map.awaitStyle(styleSpec.styleUrl)
        }

        if (style == null) {
            // MapLibre reports only success, so a timeout is what "the style never arrived"
            // looks like — nearly always a missing connection on a cold start.
            currentOnMapError(MapError.StyleUnavailable)
            return@LaunchedEffect
        }

        userLocationLayer.attachTo(style)
        loadedStyle = style
        currentOnMapError(null)
    }

    LaunchedEffect(loadedStyle, userLocation) {
        val style = loadedStyle ?: return@LaunchedEffect
        val zoom = mapLibreMap?.cameraPosition?.zoom ?: firstCamera.zoom

        userLocationLayer.update(style = style, location = userLocation, zoom = zoom)
    }

    LaunchedEffect(mapLibreMap, cameraCommands) {
        val map = mapLibreMap ?: return@LaunchedEffect

        cameraCommands.collect { command ->
            when (command) {
                is MapCameraCommand.MoveTo -> {
                    val update = CameraUpdateFactory.newCameraPosition(command.position.toMapLibre())

                    if (command.animated) {
                        map.animateCamera(update, CAMERA_ANIMATION_MILLIS)
                    } else {
                        map.moveCamera(update)
                    }
                }
            }
        }
    }

    DisposableEffect(mapLibreMap) {
        val map = mapLibreMap

        val listener = MapLibreMap.OnCameraIdleListener {
            map?.cameraPosition?.let { position -> currentOnCameraChanged(position.toDomain()) }

            // The accuracy disc is a real-world radius, so it has to be rescaled whenever
            // the zoom settles at a new level.
            val style = currentStyle
            if (style != null && map != null) {
                userLocationLayer.update(style, currentUserLocation, map.cameraPosition.zoom)
            }
        }

        map?.addOnCameraIdleListener(listener)

        onDispose { map?.removeOnCameraIdleListener(listener) }
    }
}

/**
 * Creates a [MapView] and drives its manual lifecycle from the host composable.
 *
 * MapLibre's `MapView` predates lifecycle-aware views and requires each callback to be
 * forwarded by hand. `onCreate` must run exactly once, so it is called eagerly in the
 * factory rather than from the observer, which would fire again after every stop/start.
 */
@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycle = lifecycleOwner.lifecycle

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }

        lifecycle.addObserver(observer)

        // Composition can begin when the host is already started or resumed — on a
        // configuration change, for instance — in which case the corresponding lifecycle
        // events have already been delivered and would never reach the new MapView.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()

        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    return mapView
}

/** Long enough for a slow mobile connection, short enough to report a problem promptly. */
private const val STYLE_LOAD_TIMEOUT_MILLIS = 15_000L

private const val CAMERA_ANIMATION_MILLIS = 900
