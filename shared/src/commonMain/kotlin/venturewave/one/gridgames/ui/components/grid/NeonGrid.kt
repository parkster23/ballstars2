package venturewave.one.gridgames.ui.components.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * NeonGrid - 3x3 grid with cyan glow effect
 *
 * Used for:
 * - Splash screen decoration
 * - Move detail pattern visualization
 * - Empty states
 *
 * Design reference: Splash screen, onboarding illustrations
 */
@Composable
fun NeonGrid(
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    gridColor: Color = BallStarsColor.GlowCyan,
    glowIntensity: Float = 0.6f
) {
    Canvas(modifier = modifier.size(size)) {
        val gridSize = this.size.width
        val cellSize = gridSize / 3f

        // Draw glow layers (outer to inner for proper blending)
        listOf(
            Triple(8f, glowIntensity * 0.3f, BlendMode.Screen),  // Outer glow
            Triple(4f, glowIntensity * 0.6f, BlendMode.Screen),  // Medium glow
            Triple(2f, glowIntensity, BlendMode.SrcOver)         // Core line
        ).forEach { (strokeWidth, alpha, blendMode) ->
            // Vertical lines
            for (i in 0..3) {
                val x = i * cellSize
                drawLine(
                    color = gridColor.copy(alpha = alpha),
                    start = Offset(x, 0f),
                    end = Offset(x, gridSize),
                    strokeWidth = strokeWidth,
                    blendMode = blendMode
                )
            }

            // Horizontal lines
            for (i in 0..3) {
                val y = i * cellSize
                drawLine(
                    color = gridColor.copy(alpha = alpha),
                    start = Offset(0f, y),
                    end = Offset(gridSize, y),
                    strokeWidth = strokeWidth,
                    blendMode = blendMode
                )
            }
        }
    }
}
