package com.aimaps.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AppColors.Accent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = AppColors.LightContainer,
    onPrimaryContainer = AppColors.LightOnContainer,
    secondary = AppColors.Secondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = AppColors.Tertiary,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = AppColors.LightSurface,
    onBackground = AppColors.LightOnSurface,
    surface = AppColors.LightSurface,
    onSurface = AppColors.LightOnSurface,
    surfaceVariant = AppColors.LightSurfaceVariant,
    onSurfaceVariant = AppColors.LightOnSurfaceVariant,
    outline = AppColors.LightOutline,
    outlineVariant = AppColors.LightOutlineVariant,
    error = AppColors.LightError,
    onError = AppColors.LightOnError,
    errorContainer = AppColors.LightErrorContainer,
    onErrorContainer = AppColors.LightOnErrorContainer,
)

private val DarkColors = darkColorScheme(
    primary = AppColors.AccentDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00344C),
    primaryContainer = AppColors.DarkContainer,
    onPrimaryContainer = AppColors.DarkOnContainer,
    secondary = AppColors.SecondaryDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF20333D),
    tertiary = AppColors.TertiaryDark,
    onTertiary = androidx.compose.ui.graphics.Color(0xFF2C2D4D),
    background = AppColors.DarkSurface,
    onBackground = AppColors.DarkOnSurface,
    surface = AppColors.DarkSurface,
    onSurface = AppColors.DarkOnSurface,
    surfaceVariant = AppColors.DarkSurfaceVariant,
    onSurfaceVariant = AppColors.DarkOnSurfaceVariant,
    outline = AppColors.DarkOutline,
    outlineVariant = AppColors.DarkOutlineVariant,
    error = AppColors.DarkError,
    onError = AppColors.DarkOnError,
    errorContainer = AppColors.DarkErrorContainer,
    onErrorContainer = AppColors.DarkOnErrorContainer,
)

/**
 * Applies the "AI Maps" design system.
 *
 * Light and dark are both defined from the start because the map has two distinct tile
 * palettes; the Material scheme is switched in lockstep with the map style so overlay
 * chrome never ends up light-on-light. Layout direction is deliberately **not** overridden
 * here — the overlay UI mirrors correctly in RTL locales, and the one control that must
 * stay physically bottom-right (the recentre button) opts out locally in `MapScreen`.
 */
@Composable
fun AiMapsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
