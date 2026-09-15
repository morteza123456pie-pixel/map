package com.aimaps.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii and spacing.
 *
 * Map overlays read as one family when they share a radius, so the search bar and the
 * floating controls both derive from [AppShapes.large] rather than each picking its own.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Spacing scale used by the overlay layout so paddings stay consistent. */
object AppSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp

    /** Standard inset between the screen edge and any floating overlay. */
    val OverlayInset = 12.dp

    /** Height of the floating search bar and of the circular control button. */
    val SearchBarHeight = 52.dp
    val ControlButtonSize = 48.dp
}

/** Elevation values for overlays. Kept in one place so shadows stay subtle and uniform. */
object AppElevation {
    val Overlay = 3.dp
    val Raised = 6.dp
}
