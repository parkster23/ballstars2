package venturewave.one.gridgames.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.navigation.Screen
import venturewave.one.gridgames.ui.components.GlowBox
import venturewave.one.gridgames.ui.theme.GridGamesColors
import venturewave.one.gridgames.ui.theme.GridGamesShapes

/**
 * Home menu screen with main navigation options
 *
 * BallStars-inspired design with:
 * - Dark navy background
 * - Large glowing ball icon at top
 * - 3 menu cards (Scan Targets, Play Game, Best Scores)
 * - Neon cyan borders and accents
 *
 * @param onNavigate Callback when user selects a menu option
 */
@Composable
fun HomeMenuScreen(
    onNavigate: (Screen) -> Unit
) {
    Column(
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
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Glowing ball logo
        GlowBox(
            color = GridGamesColors.NeonCyan,
            modifier = Modifier.size(180.dp),
            shape = CircleShape,
            glowIntensity = 1.0f
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GridGamesColors.NeonCyan.copy(alpha = 0.3f),
                                GridGamesColors.NeonBlue.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // App title
        Text(
            text = "GridGames",
            style = MaterialTheme.typography.headlineLarge,
            color = GridGamesColors.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(0.5f))

        // Menu cards
        MenuCard(
            icon = Icons.Default.Settings,
            title = "Setup Zone",
            subtitle = "Build your grid",
            onClick = { onNavigate(Screen.ScanTargets2) }
        )

        MenuCard(
            icon = Icons.Default.Sports,
            title = "Play Game",
            onClick = { onNavigate(Screen.PatternSelection) }
        )

        MenuCard(
            icon = Icons.Default.EmojiEvents,
            title = "Best Scores",
            onClick = { onNavigate(Screen.BestScores) }
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Menu card component with icon and title
 *
 * @param icon Icon to display
 * @param title Card title text
 * @param subtitle Optional subtitle text
 * @param onClick Callback when card is clicked
 */
@Composable
private fun MenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = GridGamesShapes.Card,
        color = GridGamesColors.DeepBlue,
        border = BorderStroke(2.dp, GridGamesColors.NeonCyan),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon with glow effect
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = GridGamesColors.NeonCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title and subtitle
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = GridGamesColors.White,
                    fontWeight = FontWeight.Medium
                )

                subtitle?.let { sub ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GridGamesColors.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
