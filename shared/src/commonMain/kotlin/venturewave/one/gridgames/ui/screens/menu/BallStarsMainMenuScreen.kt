package venturewave.one.gridgames.ui.screens.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * BallStars Main Menu Screen - Clean Asset Implementation
 *
 * Based on BallStars_MainMenu_Clean_AssetPack
 * Reference: 00_MainMenu_Reference.png
 *
 * Layout:
 * - ONE continuous urban/graffiti background (full screen)
 * - Transparent BallStars logo integrated into background
 * - Four compact primary cards with clean transparent icons
 * - Stats and Settings directly below card stack (NOT pushed to bottom)
 *
 * Four primary cards (top to bottom):
 * - SETUP STEPS: Learn setup process (cyan accent)
 * - TRAINING: Practice patterns (green accent)
 * - GAME MODE: Test your skills (cyan accent)
 * - SETUP ZONE: Calibrate your grid (cyan accent)
 *
 * Bottom actions: Stats and Settings
 */
@Composable
fun BallStarsMainMenuScreen(
    onTraining: () -> Unit,
    onGameMode: () -> Unit,
    onSetupZone: () -> Unit,
    onSetupExplainer: () -> Unit = {},
    onStats: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        // Card/gap/spacer sizes below scale CONTINUOUSLY with the actual
        // available dp height (`maxHeight`, from BoxWithConstraintsScope)
        // as a fraction of maxHeight, instead of a fixed dp value gated by
        // a step threshold. A step threshold can't fix the case where two
        // devices both land in the same bucket (e.g. "not short") but have
        // meaningfully different effective dp heights - which happens
        // whenever a user raises Android's Settings > Display size and/or
        // Font size sliders, shrinking the effective dp canvas without
        // changing physical pixels or density bucket (e.g. a Pixel 9a at
        // 1080x2424px/420 physical density can report an effective
        // ~776dp-tall canvas with those sliders raised, vs. ~914dp on a
        // stock emulator at the same physical density). Fixed-dp cards
        // then become a bigger fraction of the shorter canvas and look
        // visibly taller, even though both pass the same "tall enough"
        // check.
        //
        // The fractions below are calibrated so a ~914dp effective height
        // (stock 1080x2400 emulator, density 420, font scale 1.0)
        // reproduces the previously-reviewed ~105dp cards / 14dp gaps /
        // 22dp bottom spacer almost exactly, while a ~776dp effective
        // height (e.g. Pixel 9a with Display size + Font size raised)
        // shrinks proportionally instead of staying pinned to the same
        // absolute dp size. coerceIn(...) bounds keep sizing sane well
        // outside these two data points (very compact phones, tablets).
        //
        // Computed here (top of BoxWithConstraints, not inside the Column
        // below) because `maxHeight` is a BoxWithConstraintsScope member;
        // Compose's @LayoutScopeMarker forbids resolving it via an implicit
        // receiver once ColumnScope becomes the innermost receiver.
        val cardHeight = (maxHeight * 0.115f).coerceIn(90.dp, 130.dp)
        val cardGap = (maxHeight * 0.0153f).coerceIn(10.dp, 18.dp)
        val bottomSpacer = (maxHeight * 0.024f).coerceIn(16.dp, 26.dp)

        // ONE continuous background - full screen urban/graffiti environment
        Image(
            painter = painterResource(Res.drawable.mainmenu_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        // Subtle gradient for readability (15-25% alpha max)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF07131F).copy(alpha = 0.15f),
                            Color(0xFF07131F).copy(alpha = 0.20f),
                            Color(0xFF07131F).copy(alpha = 0.25f)
                        )
                    )
                )
        )

        // Content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BallStars Logo at top
            Image(
                painter = painterResource(Res.drawable.mainmenu_logo),
                contentDescription = "BallStars",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(2f)
                    .padding(top = 32.dp),
                contentScale = ContentScale.Fit
            )

            // Spacer pushes tiles to bottom
            Spacer(modifier = Modifier.weight(1f))

            // Primary card block - 4 tiles. cardHeight/cardGap/bottomSpacer
            // computed above (fraction of maxHeight) — see rationale there.
            PrimaryMenuCard(
                iconResource = Res.drawable.explainer,
                heading = "SETUP STEPS",
                subtitle = "Learn setup process",
                accentColor = Color(0xFF27E6F5), // Cyan
                onClick = onSetupExplainer,
                modifier = Modifier.fillMaxWidth(),
                height = cardHeight
            )

            Spacer(modifier = Modifier.height(cardGap))

            PrimaryMenuCard(
                iconResource = Res.drawable.play,
                heading = "TRAINING",
                subtitle = "Practice patterns",
                accentColor = Color(0xFF1ED36A), // Green
                onClick = onTraining,
                modifier = Modifier.fillMaxWidth(),
                height = cardHeight
            )

            Spacer(modifier = Modifier.height(cardGap))

            PrimaryMenuCard(
                iconResource = Res.drawable.match,
                heading = "GAME MODE",
                subtitle = "Test your skills",
                accentColor = Color(0xFF27E6F5), // Cyan
                onClick = onGameMode,
                modifier = Modifier.fillMaxWidth(),
                height = cardHeight
            )

            Spacer(modifier = Modifier.height(cardGap))

            PrimaryMenuCard(
                iconResource = Res.drawable.Setupgrid,
                heading = "SETUP ZONE",
                subtitle = "Calibrate your grid",
                accentColor = Color(0xFF27E6F5), // Cyan
                onClick = onSetupZone,
                modifier = Modifier.fillMaxWidth(),
                height = cardHeight
            )

            // Gap to Stats/Settings (NOT weight(1f) - keep compact!)
            Spacer(modifier = Modifier.height(bottomSpacer))

            // Bottom Actions Row - directly below cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomAction(
                    iconResource = Res.drawable.mainmenu_icon_stats,
                    label = "Stats",
                    onClick = onStats
                )

                BottomAction(
                    iconResource = Res.drawable.mainmenu_icon_settings,
                    label = "Settings",
                    onClick = onSettings
                )
            }

            // Small bottom padding for spacing from screen edge
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Primary menu card with clean transparent icon
 *
 * Features:
 * - Dark navy background (#08263A / #071F31)
 * - Bright colored 3.dp border
 * - Clean transparent icon on left
 * - Bold white uppercase heading
 * - Pale blue/grey subtitle
 * - Whole card is clickable (48.dp+ touch target)
 */
@Composable
private fun PrimaryMenuCard(
    iconResource: org.jetbrains.compose.resources.DrawableResource,
    heading: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 120.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Card internals scale with the card's own `height` (which already
    // scales with maxHeight - see BallStarsMainMenuScreen) instead of using
    // fixed dp/sp constants. Without this, the outer card correctly shrinks
    // on a device with a smaller effective dp canvas (e.g. Android Display
    // size / Font size raised), but the icon/text inside it would stay
    // pinned to their old absolute size and start to look disproportionately
    // large relative to the now-smaller card. Fractions below are chosen to
    // reproduce the original fixed values (64dp icon, 22dp spacer, 24sp/15sp
    // text, 18dp vertical padding, 4dp heading-subtitle gap) almost exactly
    // at the previous reference height of 105dp.
    val iconSize = (height.value * 0.61f).dp.coerceIn(52.dp, 76.dp)
    val iconTextGap = (height.value * 0.21f).dp.coerceIn(16.dp, 26.dp)
    val verticalPadding = (height.value * 0.17f).dp.coerceIn(12.dp, 20.dp)
    val headingFontSize = (height.value * 0.229f).coerceIn(20f, 28f).sp
    // Bumped from 0.143f/[13,17] - there's slack in the card (icon row is
    // ~69dp tall at the 105dp reference height, while heading+gap+subtitle
    // only needed ~51dp), so the subtitle can read larger without crowding.
    val subtitleFontSize = (height.value * 0.181f).coerceIn(16f, 20f).sp
    val headingSubtitleGap = (height.value * 0.038f).dp.coerceIn(3.dp, 6.dp)

    Box(
        modifier = modifier
            .semantics { contentDescription = "$heading: $subtitle" }
            .height(height)
            .border(
                width = 4.dp, // Thicker border for visibility
                color = accentColor,
                shape = RoundedCornerShape(24.dp)
            )
            .background(
                color = Color(0xFF08263A), // Dark navy interior
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 22.dp, vertical = verticalPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clean transparent icon - scales with card height
            Image(
                painter = painterResource(iconResource),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.width(iconTextGap))

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = heading,
                    color = Color.White,
                    fontSize = headingFontSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(headingSubtitleGap))
                Text(
                    text = subtitle,
                    color = Color(0xFFB0C4D8), // Pale blue/grey
                    fontSize = subtitleFontSize,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Bottom compact action button with clean transparent icon
 * Icon size: 44-48.dp visual
 * Touch target: 48.dp+ minimum
 */
@Composable
private fun BottomAction(
    iconResource: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(16.dp), // 48.dp+ touch target with icon
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(iconResource),
            contentDescription = label,
            modifier = Modifier.size(48.dp), // Larger icon
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp, // Larger label
            fontWeight = FontWeight.Medium
        )
    }
}

// System bars padding helpers
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
