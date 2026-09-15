package com.aimaps.app.map.style

import com.aimaps.app.BuildConfig
import com.aimaps.app.ui.theme.AppColors

/**
 * Resolved map style plus the attribution that must accompany it.
 *
 * Attribution travels with the style rather than living in the UI layer because the
 * required credit depends on which provider supplies the tiles.
 */
data class MapStyleSpec(
    val styleUrl: String,
    val attribution: String,
    val isDark: Boolean,
)

/**
 * Chooses the vector style URL to hand to MapLibre.
 *
 * Phase 1 is key-less by design: the default styles need no account, no token and no
 * billing, so a fresh clone renders a real map on first run. If a `MAPTILER_API_KEY` is
 * supplied (see `local.properties` / the README) the higher-quality MapTiler styles are
 * used instead — the key is injected through `BuildConfig` and is never committed.
 *
 * To point the app at a different provider, change the URL constants here; nothing else
 * in the codebase knows where tiles come from.
 */
object MapStyleProvider {

    // --- Key-less defaults --------------------------------------------------------
    // OpenFreeMap serves OpenStreetMap-derived vector tiles with no key and no quota.
    private const val OPEN_FREE_MAP_LIGHT = "https://tiles.openfreemap.org/styles/liberty"

    // OpenFreeMap publishes no true dark basemap, so CARTO's key-less dark-matter style is
    // used for dark mode; it is also built from OpenStreetMap data.
    private const val CARTO_DARK = "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"

    private const val ATTRIBUTION_OPEN_FREE_MAP = "© OpenStreetMap contributors · OpenFreeMap"
    private const val ATTRIBUTION_CARTO = "© OpenStreetMap contributors · CARTO"

    // --- Optional MapTiler styles -------------------------------------------------
    private const val MAP_TILER_LIGHT =
        "https://api.maptiler.com/maps/streets-v2/style.json?key="
    private const val MAP_TILER_DARK =
        "https://api.maptiler.com/maps/streets-v2-dark/style.json?key="
    private const val ATTRIBUTION_MAP_TILER = "© MapTiler · © OpenStreetMap contributors"

    /** True when a MapTiler key was supplied at build time. */
    val hasCommercialKey: Boolean get() = BuildConfig.MAPTILER_API_KEY.isNotBlank()

    fun resolve(isDark: Boolean): MapStyleSpec = when {
        hasCommercialKey && isDark -> MapStyleSpec(
            styleUrl = MAP_TILER_DARK + BuildConfig.MAPTILER_API_KEY,
            attribution = ATTRIBUTION_MAP_TILER,
            isDark = true,
        )

        hasCommercialKey -> MapStyleSpec(
            styleUrl = MAP_TILER_LIGHT + BuildConfig.MAPTILER_API_KEY,
            attribution = ATTRIBUTION_MAP_TILER,
            isDark = false,
        )

        isDark -> MapStyleSpec(
            styleUrl = CARTO_DARK,
            attribution = ATTRIBUTION_CARTO,
            isDark = true,
        )

        else -> MapStyleSpec(
            styleUrl = OPEN_FREE_MAP_LIGHT,
            attribution = ATTRIBUTION_OPEN_FREE_MAP,
            isDark = false,
        )
    }

    /**
     * Accent colour used for the user-location dot, matched to the active scheme so the
     * dot keeps contrast against both tile palettes.
     */
    fun userLocationColor(isDark: Boolean) = if (isDark) AppColors.AccentDark else AppColors.Accent
}
