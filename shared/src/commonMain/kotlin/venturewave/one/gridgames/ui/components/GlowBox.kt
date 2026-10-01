package venturewave.one.gridgames.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A composable that creates a glowing effect around its content
 *
 * Creates a multi-layered glow effect using:
 * - Multiple shadow layers with decreasing alpha
 * - Blur effect for soft glow
 * - Background tint for inner glow
 *
 * @param color The glow color (typically neon cyan or blue)
 * @param modifier Modifier for the container
 * @param shape Shape of the glow container (default: CircleShape)
 * @param glowIntensity Intensity multiplier for glow (0f to 1f, default 1f)
 * @param content Content to be wrapped with glow effect
 */
@Composable
fun GlowBox(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    glowIntensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            // Outer glow layer - soft and wide
            .shadow(
                elevation = (24.dp * glowIntensity),
                shape = shape,
                spotColor = color.copy(alpha = 0.6f * glowIntensity),
                ambientColor = color.copy(alpha = 0.4f * glowIntensity)
            )
            // Inner glow layer - brighter and tighter
            .shadow(
                elevation = (12.dp * glowIntensity),
                shape = shape,
                spotColor = color.copy(alpha = 0.8f * glowIntensity),
                ambientColor = color.copy(alpha = 0.6f * glowIntensity)
            )
            // Optional subtle background tint
            .background(
                color = color.copy(alpha = 0.1f * glowIntensity),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * A composable that creates a pulsing glow effect (future enhancement)
 *
 * TODO: Implement animated pulsing effect using infiniteTransition
 * This would cycle the glowIntensity between min and max values
 */
@Composable
fun PulsingGlowBox(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    content: @Composable BoxScope.() -> Unit
) {
    // For now, just use static GlowBox
    // Future: Add infiniteTransition for pulsing effect
    GlowBox(
        color = color,
        modifier = modifier,
        shape = shape,
        content = content
    )
}
