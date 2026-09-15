package com.aimaps.app.ui.map.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aimaps.app.R
import com.aimaps.app.ui.theme.AppElevation
import com.aimaps.app.ui.theme.AppSpacing

/**
 * Floating "recentre on me" control.
 *
 * Stays tappable even when location is unavailable: tapping is what surfaces the
 * explanation (grant permission, switch GPS on), and a disabled control would swallow
 * that request silently. [isActive] carries the state visually instead.
 */
@Composable
fun MyLocationButton(
    isActive: Boolean,
    isLocating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Drives a slight scale-down while a fix is in flight, so a slow GPS lock reads as
    // progress rather than as an unresponsive button.
    val locatingProgress by animateFloatAsState(
        targetValue = if (isLocating) 1f else 0f,
        label = "myLocationLocatingProgress",
    )

    Surface(
        modifier = modifier
            .size(AppSpacing.ControlButtonSize)
            .shadow(
                elevation = AppElevation.Raised,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = Color.Black.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.28f),
            ),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = AppElevation.Raised,
    ) {
        Box(contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onClick,
                shape = MaterialTheme.shapes.extraLarge,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .size(AppSpacing.ControlButtonSize)
                    .graphicsLayer {
                        val scale = 1f - (locatingProgress * 0.08f)
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_my_location),
                    contentDescription = stringResource(R.string.action_my_location),
                    tint = if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
