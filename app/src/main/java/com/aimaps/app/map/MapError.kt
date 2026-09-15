package com.aimaps.app.map

/**
 * Recoverable map failures the UI should explain.
 *
 * These are the Phase 1 subset of map problems a user can actually act on; the
 * render-time tile failures MapLibre reports internally are not surfaced individually
 * because a key-less tile host may drop individual tiles without the map becoming
 * unusable, and an error toast per tile would be noise. A style that fails to load at all
 * is fatal to the map and is reported here.
 */
sealed interface MapError {

    /** The style document could not be downloaded, usually no connection on first run. */
    data object StyleUnavailable : MapError

    data object Unknown : MapError
}
