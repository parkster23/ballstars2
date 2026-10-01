package venturewave.one.gridgames.ui.screens.explainer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.grid_creator_hero
import grid_games_mobile.shared.generated.resources.explainer_number_bg
import grid_games_mobile.shared.generated.resources.grid_creator_heading
import grid_games_mobile.shared.generated.resources.grid_creator_icon_mark
import grid_games_mobile.shared.generated.resources.grid_creator_icon_phone
import grid_games_mobile.shared.generated.resources.grid_creator_icon_detect
import grid_games_mobile.shared.generated.resources.setup_button_next
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.platform.exitApp
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing
import venturewave.one.gridgames.ui.components.navigation.FloatingBackIcon
import venturewave.one.gridgames.ui.components.navigation.FloatingNavIcons

/**
 * Grid Creator Explainer Screen - Step 2 of explainer flow
 *
 * Layout:
 * - Top 50%: Hero image showing physical grid creation
 * - Bottom 50%: Dark navy background with:
 *   - Step number 2
 *   - Heading "Create Your Grid"
 *   - Instruction text
 *   - 3 feature icons with captions
 *   - Next button
 *
 * Teaches users that BallStars needs visible physical guides (tape, chalk, etc.)
 * on the floor so the camera can establish the virtual scoring grid.
 */
@Composable
fun GridCreatorExplainerScreen(
    onNext: () -> Unit,
    onBack: () -> Unit = {},
    onHome: () -> Unit = {},
    onProfile: () -> Unit = {},
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
            // Top 40%: Hero image (reduced from 50%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            ) {
                Image(
                    painter = painterResource(Res.drawable.grid_creator_hero),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Bottom 60%: Scrollable explanation area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(BallStarsColor.BgDeep)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = BallStarsSpacing.xl)
                    .padding(top = BallStarsSpacing.md, bottom = BallStarsSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Step number with native text overlay
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.explainer_number_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
                Text(
                    text = "2",
                    style = MaterialTheme.typography.titleLarge,
                    color = BallStarsColor.TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Heading image
            Image(
                painter = painterResource(Res.drawable.grid_creator_heading),
                contentDescription = "Create Your Grid",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .wrapContentHeight(),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Instruction text - native Compose text
            Text(
                text = "Use tape, chalk or anything clearly visible on the ground to mark your grid. These markings give you a guide for where your virtual scoring grid will be.",
                style = MaterialTheme.typography.bodyMedium,
                color = BallStarsColor.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.lg))

            // Three feature icons with captions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Icon 1: Mark your grid
                FeatureIcon(
                    icon = Res.drawable.grid_creator_icon_mark,
                    caption = "Mark your grid",
                    modifier = Modifier.weight(1f)
                )

                // Icon 2: Keep the grid in view
                FeatureIcon(
                    icon = Res.drawable.grid_creator_icon_phone,
                    caption = "Keep the grid in view",
                    modifier = Modifier.weight(1f)
                )

                // Icon 3: Detect 9 scoring zones
                FeatureIcon(
                    icon = Res.drawable.grid_creator_icon_detect,
                    caption = "Detect 9 scoring zones",
                    modifier = Modifier.weight(1f)
                )
            }

                Spacer(modifier = Modifier.height(BallStarsSpacing.xl))

                // Next button (reusing the same button from Setup)
                Image(
                    painter = painterResource(Res.drawable.setup_button_next),
                    contentDescription = "Next",
                    modifier = Modifier
                        .fillMaxWidth(0.98f) // 40% bigger than the original 0.7f
                        .height(84.dp) // 40% bigger than the original 60.dp
                        .clickable { onNext() },
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(BallStarsSpacing.lg))
            }
        }

        // Floating navigation icons (top-right)
        FloatingNavIcons(
            onHomeClick = onHome,
            onProfileClick = onProfile,
            onExitClick = { exitApp() },
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Floating back button (top-left) - this screen has no other back affordance
        FloatingBackIcon(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}

/**
 * Feature icon with caption component
 * Reused from SetupStartExplainerScreen
 */
@Composable
private fun FeatureIcon(
    icon: org.jetbrains.compose.resources.DrawableResource,
    caption: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = BallStarsSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = caption,
            modifier = Modifier
                .size(60.dp)
                .wrapContentHeight(),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(BallStarsSpacing.xs))

        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = BallStarsColor.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
