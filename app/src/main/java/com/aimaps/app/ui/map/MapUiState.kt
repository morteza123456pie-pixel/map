package com.aimaps.app.ui.map

import com.aimaps.app.domain.model.CameraPosition
import com.aimaps.app.domain.model.GeoPoint
import com.aimaps.app.domain.model.LocationServiceStatus
import com.aimaps.app.domain.model.MapCameraCommand
import com.aimaps.app.domain.model.PermissionStatus
import com.aimaps.app.domain.model.UserLocation

/**
 * Everything the map screen renders, in one immutable snapshot.
 *
 * A single state object is used rather than several `MutableState` fields so that the UI
 * can never observe a half-applied update — for example a granted permission while the
 * location is still the stale one from before the grant.
 */
data class MapUiState(
    val permissionStatus: PermissionStatus = PermissionStatus.UNKNOWN,
    val locationServiceStatus: LocationServiceStatus = LocationServiceStatus.UNKNOWN,
    val userLocation: UserLocation? = null,
    val camera: CameraPosition = CameraPosition.at(GeoPoint.DEFAULT, CameraPosition.OVERVIEW_ZOOM),
    val searchQuery: String = "",
    /** True while a one-shot location fix is in flight, so the recentre button can spin. */
    val isLocatingUser: Boolean = false,
    val isOffline: Boolean = false,
    val message: UserMessage? = null,
) {

    /** The dot is only meaningful when a fix exists and the user still permits showing it. */
    val showUserLocation: Boolean get() = userLocation != null && permissionStatus.isGranted

    /** Drives the recentre button's appearance: filled once we can actually recentre. */
    val canRecentre: Boolean get() = permissionStatus.isGranted &&
        locationServiceStatus != LocationServiceStatus.DISABLED
}

/**
 * A message to show the user.
 *
 * Modelled as a type rather than a `String` so the ViewModel stays free of Android
 * resources and remains unit-testable; the composable resolves each case to a string
 * resource. [action] lets a message offer the one remedy that fixes it.
 */
sealed interface UserMessage {

    val action: MessageAction?

    /** Permission was declined once; explain the consequence and allow a retry. */
    data object PermissionDenied : UserMessage {
        override val action: MessageAction get() = MessageAction.RequestPermission
    }

    /** Declined permanently, so only the system settings screen can undo it. */
    data object PermissionPermanentlyDenied : UserMessage {
        override val action: MessageAction get() = MessageAction.OpenAppSettings
    }

    /** The device location switch is off. */
    data object LocationServicesDisabled : UserMessage {
        override val action: MessageAction get() = MessageAction.OpenLocationSettings
    }

    /** Providers are on but produced no fix — typically indoors. */
    data object LocationUnavailable : UserMessage {
        override val action: MessageAction get() = MessageAction.Retry
    }

    data object LocationTimedOut : UserMessage {
        override val action: MessageAction get() = MessageAction.Retry
    }

    /** Tiles could not be fetched; the map may be blank or partly stale. */
    data object MapStyleUnavailable : UserMessage {
        override val action: MessageAction get() = MessageAction.Retry
    }

    data object Unknown : UserMessage {
        override val action: MessageAction? get() = null
    }
}

/** The single remedy a [UserMessage] can offer. */
enum class MessageAction {
    RequestPermission,
    OpenAppSettings,
    OpenLocationSettings,
    Retry,
}

/**
 * One-shot instructions from the ViewModel to the UI.
 *
 * These cannot live in [MapUiState]: replaying a camera animation or a permission dialog
 * after a configuration change would be wrong, and state is by definition replayed.
 */
sealed interface MapEvent {

    data class MoveCamera(val command: MapCameraCommand) : MapEvent

    /** Only the composable can launch the system permission dialog. */
    data object RequestLocationPermission : MapEvent
}
