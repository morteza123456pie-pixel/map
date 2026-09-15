package com.aimaps.app.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Walks the context chain to find the hosting [Activity].
 *
 * Needed because `shouldShowRequestPermissionRationale` is an `Activity` API, while Compose
 * hands out a `Context` that may be wrapped (for example by a `ContextThemeWrapper`).
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
