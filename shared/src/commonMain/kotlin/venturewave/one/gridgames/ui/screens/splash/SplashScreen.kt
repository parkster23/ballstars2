package venturewave.one.gridgames.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.splash_background
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.ui.components.buttons.PrimaryButton
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * Splash Screen - BallStars Entry Point
 *
 * Features:
 * - Real street football background artwork (01_Splash.png)
 * - BallStars logo with "Ball" in white, "Stars" in cyan
 * - Tagline: "Real moves. Tracked. Rewarded."
 * - Motion-blur football from actual artwork
 * - [Get Started] button
 *
 * Design reference: Screenshot 1 & 2 from ballstars-reference.png
 *
 * Auto-navigates after 1.2s OR shows button for first-time users
 */
@Composable
fun SplashScreen(
    onGetStarted: () -> Unit,
    showGetStarted: Boolean = true,
    autoNavigateDelay: Long = 1200L
) {
    // Auto-navigate for returning users
    if (!showGetStarted) {
        LaunchedEffect(Unit) {
            delay(autoNavigateDelay)
            onGetStarted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BallStarsColor.BgDeep)
    ) {
        // Image at top - full width, maintains aspect ratio
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(Res.drawable.splash_background),
                contentDescription = "BallStars splash background",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter
            )

            // Top gradient fade - bleed top 10% to transparency
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.1f)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BallStarsColor.BgDeep,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Remaining space below image - button centered here
        if (showGetStarted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(BallStarsColor.BgDeep),
                contentAlignment = Alignment.Center
            ) {
                PrimaryButton(
                    text = "Get Started",
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(horizontal = 32.dp)
                )
            }
        } else {
            // Fill remaining space when button not shown
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
