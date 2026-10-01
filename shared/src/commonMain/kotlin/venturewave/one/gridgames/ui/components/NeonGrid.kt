package venturewave.one.gridgames.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import venturewave.one.gridgames.ui.theme.GridGamesColors

/**
 * A composable that draws a 3x3 neon grid overlay
 *
 * Creates a glowing grid effect with:
 * - 3x3 grid lines (4 vertical, 4 horizontal)
 * - Neon glow effect on lines
 * - Semi-transparent for overlay use
 * - Rounded line caps for smoother appearance
 *
 * @param modifier Modifier for the canvas
 * @param gridColor Color of the grid lines (default: NeonCyan)
 * @param gridAlpha Alpha transparency for grid lines (0f to 1f, default 0.3f)
 * @param gridSize Number of cells in the grid (default: 3 for 3x3)
 * @param strokeWidth Width of grid lines in pixels (default: 2f)
 */
@Composable
fun NeonGrid(
    modifier: Modifier = Modifier,
    gridColor: Color = GridGamesColors.NeonCyan,
    gridAlpha: Float = 0.3f,
    gridSize: Int = 3,
    strokeWidth: Float = 2f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val cellWidth = size.width / gridSize
        val cellHeight = size.height / gridSize

        // Draw vertical lines
        for (i in 0..gridSize) {
            val x = i * cellWidth
            drawLine(
                color = gridColor.copy(alpha = gridAlpha),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Draw horizontal lines
        for (i in 0..gridSize) {
            val y = i * cellHeight
            drawLine(
                color = gridColor.copy(alpha = gridAlpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * A brighter variant of NeonGrid for emphasis
 *
 * Uses higher alpha and thicker lines for more prominent grid display
 * Useful for splash screens or backgrounds where grid should be more visible
 */
@Composable
fun BrightNeonGrid(
    modifier: Modifier = Modifier,
    gridColor: Color = GridGamesColors.NeonCyan,
    gridSize: Int = 3
) {
    NeonGrid(
        modifier = modifier,
        gridColor = gridColor,
        gridAlpha = 0.5f,
        gridSize = gridSize,
        strokeWidth = 3f
    )
}
