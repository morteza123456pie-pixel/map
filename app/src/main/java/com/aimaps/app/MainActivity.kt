package com.aimaps.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aimaps.app.ui.map.MapScreen
import com.aimaps.app.ui.theme.AiMapsTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app's single Activity.
 *
 * Edge-to-edge is enabled here rather than in the theme so that the map can draw beneath
 * the status and navigation bars while the overlay controls stay inside the safe area.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AiMapsTheme {
                MapScreen()
            }
        }
    }
}
