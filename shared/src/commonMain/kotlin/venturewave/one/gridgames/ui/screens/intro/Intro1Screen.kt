package venturewave.one.gridgames.ui.screens.intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.intro1_hero_play
import grid_games_mobile.shared.generated.resources.ballstars_logo_overlay
import grid_games_mobile.shared.generated.resources.button_get_started
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * Intro1 Screen - First hero screen with street football player
 *
 * Layout:
 * - 90% hero image (02_Onboarding_Play.png)
 * - Top 20%: BallStars logo overlay (centered)
 * - Bottom 10%: Theme blue background with Get Started button
 */
@Composable
fun Intro1Screen(
    onGetStarted: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main hero image fills 90% of screen
        Image(
            painter = painterResource(Res.drawable.intro1_hero_play),
            contentDescription = "Street football player",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Crop
        )

        // BallStars logo overlay in top 20%
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.2f)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ballstars_logo_overlay),
                contentDescription = "BallStars logo",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .wrapContentHeight(),
                contentScale = ContentScale.FillWidth
            )
        }

        // Bottom 10% - Theme blue background with button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.1f)
                .align(Alignment.BottomCenter)
                .background(BallStarsColor.BgDeep),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.button_get_started),
                contentDescription = "Get Started",
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .wrapContentHeight()
                    .clickable { onGetStarted() },
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
