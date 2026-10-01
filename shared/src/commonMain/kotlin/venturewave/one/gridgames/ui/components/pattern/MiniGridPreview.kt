package venturewave.one.gridgames.ui.components.pattern

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * Mini 3x3 grid preview component that dynamically renders a pattern sequence
 *
 * Grid layout:
 * 1 2 3
 * 4 5 6
 * 7 8 9
 *
 * @param sequence List of target positions (1-9) in order
 * @param size Size of the grid preview
 * @param gridColor Color for grid lines
 * @param pathColor Color for the sequence path
 * @param nodeColor Color for sequence nodes
 * @param modifier Optional modifier
 */
@Composable
fun MiniGridPreview(
    sequence: List<Int>,
    size: Dp = 64.dp,
    gridColor: Color = BallStarsColor.GlowCyan.copy(alpha = 0.3f),
    pathColor: Color = BallStarsColor.Gold,
    nodeColor: Color = BallStarsColor.GlowCyan,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val gridSize = this.size
        val cellWidth = gridSize.width / 3f
        val cellHeight = gridSize.height / 3f

        // Draw grid lines
        val gridStroke = Stroke(width = 1.dp.toPx())

        // Vertical lines
        for (i in 1..2) {
            val x = i * cellWidth
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, gridSize.height),
                strokeWidth = gridStroke.width
            )
        }

        // Horizontal lines
        for (i in 1..2) {
            val y = i * cellHeight
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(gridSize.width, y),
                strokeWidth = gridStroke.width
            )
        }

        // Helper function to get cell center position from grid number (1-9)
        fun getCellCenter(gridNum: Int): Offset {
            val row = (gridNum - 1) / 3
            val col = (gridNum - 1) % 3
            return Offset(
                x = col * cellWidth + cellWidth / 2f,
                y = row * cellHeight + cellHeight / 2f
            )
        }

        // Draw path connecting sequence points
        if (sequence.isNotEmpty()) {
            val path = Path()
            val firstPoint = getCellCenter(sequence[0])
            path.moveTo(firstPoint.x, firstPoint.y)

            for (i in 1 until sequence.size) {
                val point = getCellCenter(sequence[i])
                path.lineTo(point.x, point.y)
            }

            // Draw path with dashed effect
            drawPath(
                path = path,
                color = pathColor.copy(alpha = 0.6f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            )
        }

        // Draw nodes at each sequence position
        val nodeRadius = 2.5.dp.toPx()
        sequence.forEach { gridNum ->
            val center = getCellCenter(gridNum)
            drawCircle(
                color = nodeColor,
                radius = nodeRadius,
                center = center
            )
        }

        // Draw start node (first in sequence) with larger circle
        if (sequence.isNotEmpty()) {
            val startCenter = getCellCenter(sequence[0])
            drawCircle(
                color = pathColor,
                radius = nodeRadius * 1.5f,
                center = startCenter
            )
        }
    }
}
