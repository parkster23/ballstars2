package venturewave.one.gridgames.ui.screens.training

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import venturewave.one.gridgames.model.TrainingPattern
import venturewave.one.gridgames.ui.components.pattern.DifficultyStars
import venturewave.one.gridgames.ui.components.pattern.MiniGridPreview
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing

/**
 * Pattern Explainer Screen
 *
 * Shows detailed information about a selected training pattern including:
 * - Pattern name and description
 * - Large grid preview showing the pattern sequence
 * - Difficulty rating
 * - Estimated duration
 * - Sequence visualization
 * - Instructions
 * - Start training button
 *
 * @param pattern The training pattern to explain
 * @param onStartTraining Callback when "Start Training" is tapped
 * @param onBack Callback to navigate back
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternExplainerScreen(
    pattern: TrainingPattern,
    onStartTraining: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BallStarsColor.BgDeep)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top navigation with back button
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BallStarsColor.GlowCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = BallStarsSpacing.lg)
                    .padding(bottom = BallStarsSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.lg)
            ) {
                // Pattern name
                Text(
                    text = pattern.name,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BallStarsColor.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Large grid preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BallStarsColor.Surface)
                        .border(
                            width = 2.dp,
                            color = BallStarsColor.GlowCyan.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(BallStarsSpacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    MiniGridPreview(
                        sequence = pattern.targetSequence,
                        size = 280.dp, // Large preview
                        gridColor = BallStarsColor.GlowCyan.copy(alpha = 0.4f),
                        pathColor = BallStarsColor.Gold,
                        nodeColor = BallStarsColor.GlowCyan
                    )
                }

                // Pattern info card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = BallStarsColor.Surface,
                    border = BorderStroke(1.dp, BallStarsColor.Outline)
                ) {
                    Column(
                        modifier = Modifier.padding(BallStarsSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.md)
                    ) {
                        // Description
                        InfoRow(
                            label = "Description",
                            value = pattern.description
                        )

                        Divider(color = BallStarsColor.Outline)

                        // Difficulty
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Difficulty",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = BallStarsColor.TextSecondary
                            )
                            DifficultyStars(rating = pattern.difficultyLevel)
                        }

                        Divider(color = BallStarsColor.Outline)

                        // Duration
                        InfoRow(
                            label = "Estimated Time",
                            value = "${pattern.estimatedDuration} seconds"
                        )

                        Divider(color = BallStarsColor.Outline)

                        // Sequence
                        InfoRow(
                            label = "Sequence",
                            value = pattern.targetSequence.joinToString(" → ")
                        )
                    }
                }

                // Instructions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = BallStarsColor.Surface,
                    border = BorderStroke(1.dp, BallStarsColor.Outline)
                ) {
                    Column(
                        modifier = Modifier.padding(BallStarsSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.sm)
                    ) {
                        Text(
                            text = "How to Play",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BallStarsColor.TextPrimary
                        )

                        Text(
                            text = "1. Position the ball at the starting target (first number in sequence)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BallStarsColor.TextSecondary
                        )

                        Text(
                            text = "2. Move the ball through each target in the sequence order",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BallStarsColor.TextSecondary
                        )

                        Text(
                            text = "3. Complete the full sequence as quickly and accurately as possible",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BallStarsColor.TextSecondary
                        )

                        Text(
                            text = "4. Repeat the pattern to improve your score and timing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BallStarsColor.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(BallStarsSpacing.md))

                // Start training button
                Button(
                    onClick = onStartTraining,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BallStarsColor.Primary,
                        contentColor = BallStarsColor.BgDeep
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Training",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Info row component for displaying label-value pairs
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = BallStarsColor.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = BallStarsColor.TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
