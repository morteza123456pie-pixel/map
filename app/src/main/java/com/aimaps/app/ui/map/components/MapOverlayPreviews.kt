package com.aimaps.app.ui.map.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.aimaps.app.ui.theme.AiMapsTheme
import com.aimaps.app.ui.theme.AppSpacing

/**
 * Previews for the map overlays.
 *
 * The screen itself cannot be previewed — it needs a real MapLibre surface and a Hilt
 * ViewModel — so each floating control is previewed on its own instead. That covers the
 * parts where light and dark contrast actually has to be checked by eye.
 */
@Composable
private fun OverlayPreviewContent() {
    Surface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.Medium),
        ) {
            MapSearchBar(query = "", onQueryChange = {}, onClear = {}, onSearch = {})

            MapSearchBar(
                query = "Azadi Tower",
                onQueryChange = {},
                onClear = {},
                onSearch = {},
            )

            OfflineBanner()

            MyLocationButton(isActive = true, isLocating = false, onClick = {})

            MyLocationButton(isActive = false, isLocating = false, onClick = {})

            MyLocationButton(isActive = true, isLocating = true, onClick = {})
        }
    }
}

@Preview(name = "Overlays - light", showBackground = true)
@Composable
private fun OverlaysLightPreview() {
    AiMapsTheme(darkTheme = false) { OverlayPreviewContent() }
}

@Preview(name = "Overlays - dark", showBackground = true)
@Composable
private fun OverlaysDarkPreview() {
    AiMapsTheme(darkTheme = true) { OverlayPreviewContent() }
}

/** Guards against the search field breaking on a small or scaled-up display. */
@Preview(name = "Search bar - large font", showBackground = true, fontScale = 1.5f, widthDp = 320)
@Composable
private fun SearchBarLargeFontPreview() {
    AiMapsTheme {
        Surface {
            MapSearchBar(
                query = "Tehran Grand Bazaar",
                onQueryChange = {},
                onClear = {},
                onSearch = {},
                modifier = Modifier.padding(AppSpacing.Medium),
            )
        }
    }
}
