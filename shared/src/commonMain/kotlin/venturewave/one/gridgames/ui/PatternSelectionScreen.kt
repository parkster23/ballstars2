package venturewave.one.gridgames.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.model.TrainingPattern
import venturewave.one.gridgames.ui.theme.GridGamesColors
import venturewave.one.gridgames.ui.theme.GridGamesShapes

/**
 * Pattern selection screen with BallStars-inspired design
 *
 * Features:
 * - Dark navy background
 * - Neon blue bordered pattern cards
 * - Glowing difficulty indicators
 * - Back button to return to home
 *
 * @param onPatternSelected Callback when user selects a pattern
 * @param onBack Callback to navigate back to home
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternSelectionScreen(
    onPatternSelected: (TrainingPattern) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val patterns = TrainingPattern.getBundledPatterns()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Choose Pattern",
                        color = GridGamesColors.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GridGamesColors.NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GridGamesColors.DeepBlue,
                    titleContentColor = GridGamesColors.White
                )
            )
        },
        containerColor = GridGamesColors.DarkNavy,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Select Training Pattern",
                        style = MaterialTheme.typography.headlineSmall,
                        color = GridGamesColors.NeonCyan,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(
                    items = patterns,
                    key = { it.id }
                ) { pattern ->
                    PatternCard(
                        pattern = pattern,
                        onClick = { onPatternSelected(pattern) }
                    )
                }
            }
        }
    }
}

/**
 * Pattern card with BallStars neon styling
 *
 * @param pattern Training pattern to display
 * @param onClick Callback when card is clicked
 * @param modifier Optional modifier
 */
@Composable
private fun PatternCard(
    pattern: TrainingPattern,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = GridGamesShapes.Card,
        color = GridGamesColors.DeepBlue,
        border = BorderStroke(2.dp, GridGamesColors.NeonBlue),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Pattern info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pattern name
                Text(
                    text = pattern.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = GridGamesColors.White,
                    fontWeight = FontWeight.Bold
                )

                // Description
                Text(
                    text = pattern.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GridGamesColors.WhiteAlpha70,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Difficulty and duration
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Difficulty with neon stars
                    Text(
                        text = "${"★".repeat(pattern.difficultyLevel)}${"☆".repeat(5 - pattern.difficultyLevel)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GridGamesColors.GlowYellow
                    )

                    // Duration
                    Text(
                        text = "${pattern.estimatedDuration}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GridGamesColors.NeonCyan
                    )
                }
            }

            // Right side: Target count badge
            Surface(
                shape = GridGamesShapes.SmallRounded,
                color = GridGamesColors.NeonCyan.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, GridGamesColors.NeonCyan),
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = "${pattern.targetSequence.size}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = GridGamesColors.NeonCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}
