package venturewave.one.gridgames.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.onboarding_1
import grid_games_mobile.shared.generated.resources.onboarding_2
import grid_games_mobile.shared.generated.resources.onboarding_3
import grid_games_mobile.shared.generated.resources.onboarding_4
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.ui.components.buttons.PrimaryButton
import venturewave.one.gridgames.ui.components.effects.BurstBackground
import venturewave.one.gridgames.ui.components.grid.NeonGrid
import venturewave.one.gridgames.ui.components.navigation.FloatingNavIcons
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing
import venturewave.one.gridgames.platform.exitApp

/**
 * Onboarding Screen - 4-page introduction to BallStars
 *
 * Pages:
 * 1. "BallStars / Real moves. Tracked. Rewarded." - [Get Started]
 * 2. "Move your way / Do real street-football moves..." - [Next]
 * 3. "Level up / Hit targets, earn stars..." - [Let's Play]
 * 4. "Better together / Compete, share..." - [Join the BallStars]
 *
 * Design reference: Screenshots 2-5 from ballstars-reference.png
 *
 * Using real street football illustrations from BallStars Game Asset Pack
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onHome: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    illustration = Res.drawable.onboarding_1,
                    headline = "BallStars",
                    headlineColor = BallStarsColor.GlowCyan,
                    subtext = "Real moves. Tracked. Rewarded.",
                    buttonText = "Get Started",
                    onButtonClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
                1 -> OnboardingPage(
                    illustration = Res.drawable.onboarding_2,
                    headline = "Move your way",
                    headlineColor = BallStarsColor.GlowCyan,
                    subtext = "Do real street-football moves, and get them tracked by the camera.",
                    buttonText = "Next",
                    onButtonClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    }
                )
                2 -> OnboardingPage(
                    illustration = Res.drawable.onboarding_3,
                    headline = "Level up",
                    headlineColor = BallStarsColor.GlowCyan,
                    subtext = "Hit targets, earn stars, and unlock new moves.",
                    buttonText = "Let's Play",
                    onButtonClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(3)
                        }
                    }
                )
                3 -> OnboardingPage(
                    illustration = Res.drawable.onboarding_4,
                    headline = "Better together",
                    headlineColor = BallStarsColor.GlowCyan,
                    subtext = "Compete, share, and show off your skills.",
                    buttonText = "Join the BallStars",
                    onButtonClick = onComplete,
                    isLastPage = true
                )
            }
        }

        // Page indicator dots
        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { iteration ->
                val color = if (pagerState.currentPage == iteration) {
                    BallStarsColor.Primary
                } else {
                    BallStarsColor.Outline
                }
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }

        // Floating navigation icons (top-right)
        FloatingNavIcons(
            onHomeClick = onHome,
            onProfileClick = onProfile,
            onExitClick = { exitApp() },
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

/**
 * Individual onboarding page with real illustration
 */
@Composable
private fun OnboardingPage(
    illustration: DrawableResource,
    headline: String,
    headlineColor: Color,
    subtext: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    isLastPage: Boolean = false
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Real street football illustration fills entire screen
        // Illustration has dark blue gradient at bottom for text/button placement
        Image(
            painter = painterResource(illustration),
            contentDescription = "Onboarding illustration",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Text and button positioned at bottom (on dark gradient area of artwork)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = BallStarsSpacing.xl)
                .padding(bottom = BallStarsSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Headline and subtext
            Text(
                text = headline,
                style = MaterialTheme.typography.displayMedium,
                color = headlineColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            Text(
                text = subtext,
                style = MaterialTheme.typography.bodyLarge,
                color = BallStarsColor.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = BallStarsSpacing.md)
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.xl))

            // Button
            PrimaryButton(
                text = buttonText,
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}
