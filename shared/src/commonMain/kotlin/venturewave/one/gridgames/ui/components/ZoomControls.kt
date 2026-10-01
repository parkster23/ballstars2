package venturewave.one.gridgames.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.ui.theme.GridGamesColors

/**
 * Zoom control buttons for camera
 *
 * Features:
 * - Circular +/- buttons with neon cyan borders
 * - Current zoom percentage display
 * - BallStars-inspired styling
 * - Vertical layout (stacked buttons)
 *
 * @param currentZoom Current zoom ratio (1.0 = no zoom, 5.0 = max zoom)
 * @param onZoomIn Callback when zoom in button is clicked
 * @param onZoomOut Callback when zoom out button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun ZoomControls(
    currentZoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom In button
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onZoomIn),
            shape = CircleShape,
            color = GridGamesColors.DeepBlue.copy(alpha = 0.8f),
            border = BorderStroke(2.dp, GridGamesColors.NeonCyan),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GridGamesColors.NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Zoom level indicator
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = GridGamesColors.DeepBlue.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, GridGamesColors.NeonBlue)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${(currentZoom * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = GridGamesColors.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Zoom Out button
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onZoomOut),
            shape = CircleShape,
            color = GridGamesColors.DeepBlue.copy(alpha = 0.8f),
            border = BorderStroke(2.dp, GridGamesColors.NeonCyan),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.headlineSmall,
                    color = GridGamesColors.NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
