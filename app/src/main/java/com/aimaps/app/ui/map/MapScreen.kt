package com.aimaps.app.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aimaps.app.domain.model.MapCameraCommand
import com.aimaps.app.domain.model.PermissionStatus
import com.aimaps.app.map.MapError
import com.aimaps.app.map.MapLibreMapView
import com.aimaps.app.map.style.MapStyleProvider
import com.aimaps.app.ui.map.components.MapSearchBar
import com.aimaps.app.ui.map.components.MyLocationButton
import com.aimaps.app.ui.map.components.OfflineBanner
import com.aimaps.app.ui.theme.AppSpacing
import com.aimaps.app.ui.util.findActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The app's only screen: a full-bleed map with floating overlay controls.
 *
 * Permission handling lives here rather than in the ViewModel because launching the system
 * dialog needs an Activity-scoped launcher. The ViewModel still owns the *state* of that
 * permission and decides when a request is warranted.
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val styleSpec = remember(darkTheme) { MapStyleProvider.resolve(isDark = darkTheme) }

    val snackbarHostState = remember { SnackbarHostState() }

    // A channel rather than a shared flow: camera commands must be buffered so that a
    // location fix arriving before the map is ready is delivered once it is, not dropped.
    val cameraCommandChannel = remember { Channel<MapCameraCommand>(Channel.BUFFERED) }
    val cameraCommands = remember(cameraCommandChannel) { cameraCommandChannel.receiveAsFlow() }

    var hasRequestedPermissionThisSession by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants.values.any { it }
        val showRationale = context.findActivity()
            ?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            ?: false

        viewModel.onPermissionResult(granted = granted, showRationale = showRationale)
    }

    // Ask once, on first display. A denial is handled gracefully rather than re-prompted.
    LaunchedEffect(uiState.permissionStatus) {
        val shouldAsk = uiState.permissionStatus == PermissionStatus.UNKNOWN &&
            !hasRequestedPermissionThisSession

        if (shouldAsk) {
            hasRequestedPermissionThisSession = true
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    // Re-read the grant whenever the screen resumes: the user may have changed it in system
    // settings while the app was in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissionStatus()
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MapEvent.MoveCamera -> cameraCommandChannel.send(event.command)

                MapEvent.RequestLocationPermission -> permissionLauncher.launch(LOCATION_PERMISSIONS)
            }
        }
    }

    LaunchedEffect(uiState.message, snackbarHostState) {
        val message = uiState.message ?: return@LaunchedEffect
        val action = message.action

        val result = snackbarHostState.showSnackbar(
            message = context.getString(message.textRes()),
            actionLabel = action?.let { context.getString(it.labelRes()) },
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )

        when (result) {
            SnackbarResult.ActionPerformed -> if (action != null) {
                handleMessageAction(
                    action = action,
                    context = context,
                    requestPermission = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                    retry = viewModel::onRetry,
                )
            }

            SnackbarResult.Dismissed -> viewModel.onMessageDismissed()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // The map owns the whole window; insets are applied to the overlays instead.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = AppSpacing.ControlButtonSize + AppSpacing.ExtraLarge),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            MapLibreMapView(
                styleSpec = styleSpec,
                initialCamera = uiState.camera,
                userLocation = uiState.userLocation.takeIf { uiState.showUserLocation },
                cameraCommands = cameraCommands,
                onCameraChanged = viewModel::onCameraChanged,
                onMapError = { error: MapError? -> viewModel.onMapError(error) },
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        horizontal = AppSpacing.OverlayInset,
                        vertical = AppSpacing.OverlayInset,
                    ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.Small),
            ) {
                MapSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onClear = viewModel::onSearchQueryCleared,
                    // Phase 2 routes this to SearchRepository.
                    onSearch = { },
                )

                if (uiState.isOffline) {
                    OfflineBanner(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }

            // The spec pins this control to the physical bottom-right corner. Layout
            // direction is forced to LTR for this subtree only, so the button keeps its
            // corner in RTL locales while the search field still mirrors correctly.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MyLocationButton(
                    isActive = uiState.canRecentre,
                    isLocating = uiState.isLocatingUser,
                    onClick = viewModel::recentreOnUser,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(AppSpacing.OverlayInset),
                )
            }

            // Attribution is a licence obligation for OpenStreetMap-derived tiles.
            Text(
                text = styleSpec.attribution,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(
                        start = AppSpacing.OverlayInset,
                        bottom = AppSpacing.OverlayInset,
                        end = AppSpacing.ControlButtonSize + AppSpacing.OverlayInset * 2,
                    ),
            )
        }
    }
}

/** Performs the single remedy attached to a message. */
private fun handleMessageAction(
    action: MessageAction,
    context: Context,
    requestPermission: () -> Unit,
    retry: () -> Unit,
) {
    when (action) {
        MessageAction.RequestPermission -> requestPermission()

        MessageAction.OpenAppSettings -> context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        MessageAction.OpenLocationSettings -> context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        MessageAction.Retry -> retry()
    }
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)
