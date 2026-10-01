package venturewave.one.gridgames.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import venturewave.one.gridgames.ui.components.BrightNeonGrid
import venturewave.one.gridgames.ui.components.GlowBox
import venturewave.one.gridgames.ui.theme.GridGamesColors

/**
 * BallStars-style splash screen with Get Started button
 *
 * Features:
 * - Dark navy gradient background with neon grid overlay
 * - Large glowing soccer ball icon at top
 * - "BallStars" title with subtitle
 * - "Real moves. Tracked. Rewarded." tagline
 * - Green "Get Started" button that launches Scan Targets 2
 *
 * @param onNavigateToHome Callback when Get Started is clicked (navigates to ScanTargets2)
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
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
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background 3x3 neon grid
        BrightNeonGrid(
            modifier = Modifier.fillMaxSize(),
            gridColor = GridGamesColors.NeonCyan.copy(alpha = 0.3f)
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Top section: Ball icon + title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Glowing soccer ball icon
                GlowBox(
                    color = GridGamesColors.NeonCyan,
                    modifier = Modifier.size(200.dp),
                    shape = CircleShape,
                    glowIntensity = 1.5f
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        GridGamesColors.NeonCyan.copy(alpha = 0.4f),
                                        GridGamesColors.NeonBlue.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                // App title - BallStars style
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Ball" in white + "Stars" in cyan
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ball",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp
                            ),
                            color = GridGamesColors.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Stars",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp
                            ),
                            color = GridGamesColors.NeonCyan,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Tagline
                    Text(
                        text = "Real moves.\nTracked. Rewarded.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp
                        ),
                        color = GridGamesColors.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            // Bottom section: Get Started button
            Button(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D68F) // BallStars green
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Get Started",
                    color = GridGamesColors.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
