package venturewave.one.gridgames.ui.screens.explainer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.phone_stand
import grid_games_mobile.shared.generated.resources.setup_number_1
import grid_games_mobile.shared.generated.resources.setup_heading_setup_start
import grid_games_mobile.shared.generated.resources.setup_icon_find_area
import grid_games_mobile.shared.generated.resources.setup_icon_position_phone
import grid_games_mobile.shared.generated.resources.setup_icon_detect_zones
import grid_games_mobile.shared.generated.resources.setup_button_next
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.platform.exitApp
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing
import venturewave.one.gridgames.ui.components.navigation.FloatingBackIcon
import venturewave.one.gridgames.ui.components.navigation.FloatingNavIcons

/**
 * Setup & Start Explainer Screen - Step 1 of 3
 *
 * Layout:
 * - Top 50%: Hero image (01_Hero_Setup.png)
 * - Bottom 50%: Dark navy background with:
 *   - Step number (08_Number_1.png)
 *   - Heading (09_Heading_Setup_Start.png)
 *   - Instruction text
 *   - 3 feature icons with captions
 *   - Next button
 */
@Composable
fun SetupStartExplainerScreen(
    onNext: () -> Unit,
    onBack: () -> Unit = {},
    onHome: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BallStarsColor.BgDeep)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top 40%: Hero image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            ) {
                Image(
                    painter = painterResource(Res.drawable.phone_stand),
                    contentDescription = "Phone stand setup",
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
                    .padding(horizontal = BallStarsSpacing.xl)
                    .padding(top = BallStarsSpacing.md, bottom = BallStarsSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Step number
            Image(
                painter = painterResource(Res.drawable.setup_number_1),
                contentDescription = "Step 1",
                modifier = Modifier
                    .width(40.dp)
                    .wrapContentHeight(),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Heading image
            Image(
                painter = painterResource(Res.drawable.setup_heading_setup_start),
                contentDescription = "Set up & Start",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .wrapContentHeight(),
                contentScale = ContentScale.FillWidth
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Instruction text
            Text(
                text = "Find a flat area with good lighting and place your phone where it can see the ground.",
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
                // Icon 1: Find area
                FeatureIcon(
                    icon = Res.drawable.setup_icon_find_area,
                    caption = "Find a flat area",
                    modifier = Modifier.weight(1f)
                )

                // Icon 2: Position phone
                FeatureIcon(
                    icon = Res.drawable.setup_icon_position_phone,
                    caption = "Position phone",
                    modifier = Modifier.weight(1f)
                )

                // Icon 3: Detect zones
                FeatureIcon(
                    icon = Res.drawable.setup_icon_detect_zones,
                    caption = "Detect zones",
                    modifier = Modifier.weight(1f)
                )
            }

                Spacer(modifier = Modifier.height(BallStarsSpacing.xl))

                // Next button
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
