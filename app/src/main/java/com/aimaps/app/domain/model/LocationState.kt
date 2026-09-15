package com.aimaps.app.domain.model

/**
 * Permission state for a runtime permission the app cares about.
 *
 * Modelled as a three-way state rather than a boolean because "permanently denied" (the
 * user chose "Don't allow" twice, or revoked it in system settings) needs a different
 * affordance: a link to app settings instead of another in-app prompt that the system
 * would silently suppress.
 */
enum class PermissionStatus {
    /** Not asked yet in this install. */
    UNKNOWN,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    ;

    val isGranted: Boolean get() = this == GRANTED
}

/** Device-level location switch, exposed so the UI can prompt the user to switch GPS on. */
enum class LocationServiceStatus {
    ENABLED,
    DISABLED,
    UNKNOWN,
}
