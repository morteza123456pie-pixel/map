package com.aimaps.app.ui.map

import androidx.annotation.StringRes
import com.aimaps.app.R

/** Maps a [UserMessage] to the text shown to the user. */
@StringRes
fun UserMessage.textRes(): Int = when (this) {
    UserMessage.PermissionDenied -> R.string.message_permission_denied
    UserMessage.PermissionPermanentlyDenied -> R.string.message_permission_permanently_denied
    UserMessage.LocationServicesDisabled -> R.string.message_location_services_disabled
    UserMessage.LocationUnavailable -> R.string.message_location_unavailable
    UserMessage.LocationTimedOut -> R.string.message_location_timed_out
    UserMessage.MapStyleUnavailable -> R.string.message_map_style_unavailable
    UserMessage.Unknown -> R.string.message_unknown
}

/** Maps a [MessageAction] to its button label. */
@StringRes
fun MessageAction.labelRes(): Int = when (this) {
    MessageAction.RequestPermission -> R.string.action_grant_permission
    MessageAction.OpenAppSettings -> R.string.action_open_settings
    MessageAction.OpenLocationSettings -> R.string.action_turn_on_location
    MessageAction.Retry -> R.string.action_retry
}
