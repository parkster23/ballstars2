package venturewave.one.gridgames.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.splash_background
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * BallStars Splash / Start Screen
 *
 * Main game entry screen following BallStars urban/street-football visual language.
 * PLAY button navigates to the existing play-choice screen.
 *
 * Design: Full-screen urban court environment with prominent logo, hero football artwork,
 * and bright green PLAY button.
 */
@Composable
fun BallStarsSplashScreen(
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BallStarsColor.BgDeep)
    ) {
        // Infinite animations
        val infiniteTransition = rememberInfiniteTransition(label = "splash_animations")

        // PLAY button glow pulse
        val playGlowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "play_glow"
        )

        // Background - Full screen urban court environment
        Image(
            painter = painterResource(Res.drawable.splash_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // PLAY Button - Justified to bottom of screen (scaled down by 30%)
        PlayButton(
            onClick = onPlay,
            glowAlpha = playGlowAlpha,
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        )
    }
}

/**
 * Custom PLAY button matching BallStars reference design
 * - Green pill shape
 * - Dark/navy PLAY text and triangle
 * - Cyan/green glow
 */
@Composable
private fun PlayButton(
    onClick: () -> Unit,
    glowAlpha: Float,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press scale animation (~0.96 when pressed)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press_scale"
    )

    Box(
        modifier = modifier
            .semantics { contentDescription = "Play BallStars" }
            .scale(scale)
    ) {
        // Glow effect layer (behind button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(36.dp),
                    ambientColor = BallStarsColor.GlowCyan.copy(alpha = glowAlpha),
                    spotColor = BallStarsColor.Primary.copy(alpha = glowAlpha * 0.6f)
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BallStarsColor.Primary.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            BallStarsColor.Primary,
                            BallStarsColor.Primary.copy(green = 0.9f)
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                // Play triangle icon
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BallStarsColor.BgDeep,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // PLAY text
                Text(
                    text = "PLAY",
                    color = BallStarsColor.BgDeep,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// Helper to access system bars padding
@Composable
private fun Modifier.systemBarsPadding(): Modifier = this
    .statusBarsPadding()

@Composable
private fun Modifier.statusBarsPadding(): Modifier = this.then(
    WindowInsets.statusBars.asPaddingValues().let { padding ->
        Modifier.padding(padding)
    }
)

@Composable
private fun Modifier.navigationBarsPadding(): Modifier = this.then(
    WindowInsets.navigationBars.asPaddingValues().let { padding ->
        Modifier.padding(padding)
    }
)
