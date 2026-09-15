package com.aimaps.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens for the map-first UI.
 *
 * The palette is deliberately restrained: the vector map supplies almost all of the
 * colour on screen, and overlays must read as chrome floating above it rather than
 * competing with it. A blue-cyan accent is used instead of the default Material purple
 * because it stays legible against both the light OSM palette and the dark one.
 */
object AppColors {

    // --- Brand ramp ---------------------------------------------------------------
    val Accent = Color(0xFF00658F)
    val AccentDark = Color(0xFF7CD0F8)
    val Secondary = Color(0xFF4E616D)
    val SecondaryDark = Color(0xFFB6C9D6)
    val Tertiary = Color(0xFF5A5B7D)
    val TertiaryDark = Color(0xFFC3C3EB)

    // --- Light surfaces -----------------------------------------------------------
    val LightSurface = Color(0xFFFAFCFF)
    val LightSurfaceVariant = Color(0xFFDCE3E9)
    val LightOnSurface = Color(0xFF191C1E)
    val LightOnSurfaceVariant = Color(0xFF40484C)
    val LightOutline = Color(0xFF70787D)
    val LightOutlineVariant = Color(0xFFBFC8CD)
    val LightContainer = Color(0xFFC9E6FF)
    val LightOnContainer = Color(0xFF001E2E)
    val LightError = Color(0xFFBA1A1A)
    val LightOnError = Color(0xFFFFFFFF)
    val LightErrorContainer = Color(0xFFFFDAD6)
    val LightOnErrorContainer = Color(0xFF410002)

    // --- Dark surfaces ------------------------------------------------------------
    val DarkSurface = Color(0xFF111416)
    val DarkSurfaceVariant = Color(0xFF40484C)
    val DarkOnSurface = Color(0xFFE1E2E5)
    val DarkOnSurfaceVariant = Color(0xFFC0C8CC)
    val DarkOutline = Color(0xFF8A9297)
    val DarkOutlineVariant = Color(0xFF40484C)
    val DarkContainer = Color(0xFF004C6E)
    val DarkOnContainer = Color(0xFFC9E6FF)
    val DarkError = Color(0xFFFFB4AB)
    val DarkOnError = Color(0xFF690005)
    val DarkErrorContainer = Color(0xFF93000A)
    val DarkOnErrorContainer = Color(0xFFFFDAD6)

    // --- Map overlay chrome -------------------------------------------------------
    /**
     * Overlay surfaces are intentionally not fully opaque. The reference design language
     * for map apps keeps the map faintly visible through floating controls, which anchors
     * them to the map instead of making them look like separate panels.
     */
    const val LIGHT_OVERLAY_ALPHA = 0.94f
    const val DARK_OVERLAY_ALPHA = 0.88f
}
