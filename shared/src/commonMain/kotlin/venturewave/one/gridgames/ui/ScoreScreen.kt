package venturewave.one.gridgames.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.model.GameResult
import venturewave.one.gridgames.ui.theme.GridGamesColors
import kotlin.math.round
import venturewave.one.gridgames.ui.theme.GridGamesShapes

/**
 * Score screen showing game results
 *
 * ADAPTIVE LAYOUT PRINCIPLE:
 * - Uses Spacer with weight for flexible spacing
 * - Column with weight-based distribution
 * - Minimal fixed dp values
 */
@Composable
fun ScoreScreen(
    result: GameResult,
    onPlayAgain: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GridGamesColors.DarkNavy,
                        GridGamesColors.DeepBlue
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section: Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(0.2f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Training Complete!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = GridGamesColors.NeonCyan,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = result.pattern.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = GridGamesColors.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Middle section: Stats (takes most space)
            // Scrollable so additional stat cards (or a small/short screen)
            // never get silently clipped by the fixed weight(0.6f) height —
            // content that fits just centers as before; content that
            // doesn't becomes scrollable instead of cut off.
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                // Score card
                ScoreCard(
                    label = "Score",
                    value = result.score.toString(),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                // Stats grid using Row with weights
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScoreCard(
                        label = "Time",
                        value = "${formatDecimal(result.totalTime / 1000f, 1)}s",
                        modifier = Modifier.weight(1f)
                    )

                    ScoreCard(
                        label = "Accuracy",
                        value = "${formatDecimal(result.accuracy, 0)}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScoreCard(
                        label = "Hits",
                        value = result.hitCount.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    ScoreCard(
                        label = "Misses",
                        value = result.missCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Full loops through the pattern's target sequence completed
                // this session (e.g. Triangles [5,7,4,5] hit once = 1).
                ScoreCard(
                    label = "Patterns Completed",
                    value = result.patternsCompleted.toString(),
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                // Bonus points earned from the 3x completion multiplier on
                // clean laps, and points lost to wrong-target hits.
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScoreCard(
                        label = "Bonus (3x)",
                        value = "+${result.bonusPoints}",
                        modifier = Modifier.weight(1f)
                    )

                    ScoreCard(
                        label = "Points Lost",
                        value = "-${result.penaltyPoints}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom section: Buttons
            Column(
                modifier = Modifier.weight(0.2f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)
            ) {
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GridGamesColors.BrightGreen,
                        contentColor = GridGamesColors.DarkNavy
                    ),
                    shape = GridGamesShapes.RoundedButton
                ) {
                    Text(
                        text = "Play Again",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GridGamesColors.NeonCyan
                    ),
                    border = BorderStroke(2.dp, GridGamesColors.NeonCyan),
                    shape = GridGamesShapes.RoundedButton
                ) {
                    Text(
                        text = "Back to Home",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Card displaying a single score stat
 * Adaptive sizing using fillMaxWidth with fraction or weight
 */
@Composable
private fun ScoreCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = GridGamesShapes.Card,
        color = GridGamesColors.DeepBlue,
        border = BorderStroke(2.dp, GridGamesColors.NeonBlue),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = GridGamesColors.WhiteAlpha70,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = GridGamesColors.NeonCyan,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Multiplatform-safe fixed-decimal formatting. `String.format`/`java.lang.String`
 * is JVM-only and doesn't compile for iOS; this assumes non-negative values,
 * which both call sites here (elapsed time, accuracy percentage) satisfy.
 */
private fun formatDecimal(value: Float, decimals: Int): String {
    val factor = (1..decimals).fold(1f) { acc, _ -> acc * 10f }
    val rounded = round(value * factor) / factor
    val whole = rounded.toLong()
    if (decimals == 0) return whole.toString()
    val fractionDigits = round((rounded - whole) * factor).toLong().toString().padStart(decimals, '0')
    return "$whole.$fractionDigits"
}
