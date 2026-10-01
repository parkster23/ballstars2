package venturewave.one.gridgames.ui.components.pattern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * Displays difficulty rating as filled and empty stars
 *
 * @param rating Current difficulty level (1-5)
 * @param maxRating Maximum rating (default 5)
 * @param filledColor Color for filled stars
 * @param emptyColor Color for empty stars
 * @param modifier Optional modifier
 */
@Composable
fun DifficultyStars(
    rating: Int,
    maxRating: Int = 5,
    filledColor: Color = BallStarsColor.Gold,
    emptyColor: Color = BallStarsColor.TextSecondary.copy(alpha = 0.3f),
    modifier: Modifier = Modifier
) {
    val clampedRating = rating.coerceIn(0, maxRating)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Filled stars
        repeat(clampedRating) {
            Text(
                text = "★",
                style = MaterialTheme.typography.bodySmall,
                color = filledColor
            )
        }

        // Empty stars
        repeat(maxRating - clampedRating) {
            Text(
                text = "☆",
                style = MaterialTheme.typography.bodySmall,
                color = emptyColor
            )
        }
    }
}
