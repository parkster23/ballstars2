package venturewave.one.gridgames.ui.screens.choice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.play_choice_hero
import grid_games_mobile.shared.generated.resources.play_choice_icon_setup
import grid_games_mobile.shared.generated.resources.play_choice_icon_training
import grid_games_mobile.shared.generated.resources.play_choice_icon_matches
import grid_games_mobile.shared.generated.resources.play_choice_icon_star_scores
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing
import venturewave.one.gridgames.ui.components.navigation.FloatingBackIcon
import venturewave.one.gridgames.ui.components.navigation.FloatingNavIcons
import venturewave.one.gridgames.platform.exitApp

/**
 * Play Choice Screen
 *
 * Asks the player how they want to play:
 * 1. Setup Steps - go through grid setup and learn
 * 2. Training Sessions - practice skills
 * 3. Matches - challenge friends
 *
 * Layout for phone portrait (Pixel 9 ~20:9):
 * - Hero: ~34% (boy and girl with footballs)
 * - Heading + intro: ~13%
 * - Three choice cards: ~53% (stacked vertically)
 */
@Composable
fun PlayChoiceScreen(
    onSetupSteps: () -> Unit,
    onTraining: () -> Unit,
    onMatches: () -> Unit,
    onStarScores: () -> Unit,
    onBack: () -> Unit = {},
    onHome: () -> Unit = {},
    onProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BallStarsColor.BgDeep)
                .verticalScroll(rememberScrollState())
        ) {
        // Hero: ~28% - Boy and girl with footballs (reduced from 34%)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.28f)
        ) {
            Image(
                painter = painterResource(Res.drawable.play_choice_hero),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Heading + intro: reduced spacing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BallStarsSpacing.xl)
                .padding(top = BallStarsSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heading with "play?" in green
            val headingText = buildAnnotatedString {
                append("How do you want to ")
                withStyle(SpanStyle(color = BallStarsColor.Primary)) {
                    append("play?")
                }
            }

            Text(
                text = headingText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = BallStarsColor.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.xs))

            // Supporting text
            Text(
                text = "Go through the setup, or jump straight into training or matches.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = BallStarsColor.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }

        Spacer(modifier = Modifier.height(BallStarsSpacing.md))

        // Four choice cards (stacked vertically)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BallStarsSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.sm)
        ) {
            // Choice 1: Setup Steps
            PlayChoiceCard(
                title = "Setup Steps",
                description = "Set up your grid and learn how to play.",
                icon = Res.drawable.play_choice_icon_setup,
                accentColor = BallStarsColor.GlowCyan,
                onClick = onSetupSteps
            )

            // Choice 2: Training Sessions
            PlayChoiceCard(
                title = "Training Sessions",
                description = "Practise skills and improve your score.",
                icon = Res.drawable.play_choice_icon_training,
                accentColor = BallStarsColor.Primary,
                onClick = onTraining
            )

            // Choice 3: Matches
            PlayChoiceCard(
                title = "Matches",
                description = "Challenge your friends and compete.",
                icon = Res.drawable.play_choice_icon_matches,
                accentColor = Color(0xFF8B5CF6), // Purple accent
                onClick = onMatches
            )

            // Choice 4: Star Scores
            PlayChoiceCard(
                title = "Star Scores",
                description = "View your scores, stats and progress.",
                icon = Res.drawable.play_choice_icon_star_scores,
                accentColor = BallStarsColor.Gold,
                onClick = onStarScores
            )
        }

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))
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
 * Reusable choice card component
 *
 * @param title Main choice title
 * @param description Supporting description text
 * @param icon Icon resource for the choice
 * @param accentColor Accent color for the border
 * @param onClick Action when card is clicked
 */
@Composable
private fun PlayChoiceCard(
    title: String,
    description: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp),
        shape = RoundedCornerShape(22.dp),
        color = BallStarsColor.Surface,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = BallStarsSpacing.md, vertical = BallStarsSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(BallStarsSpacing.md))

            // Title and description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = BallStarsColor.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = BallStarsColor.TextSecondary,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(BallStarsSpacing.sm))

            // Chevron
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
