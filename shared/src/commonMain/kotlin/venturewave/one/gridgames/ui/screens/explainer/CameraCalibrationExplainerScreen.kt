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
import grid_games_mobile.shared.generated.resources.camera
import grid_games_mobile.shared.generated.resources.explainer_number_bg
import grid_games_mobile.shared.generated.resources.setup_button_next
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsSpacing
import venturewave.one.gridgames.ui.components.navigation.FloatingBackIcon
import venturewave.one.gridgames.ui.components.navigation.FloatingNavIcons
import venturewave.one.gridgames.platform.exitApp

/**
 * Camera Calibration Explainer Screen - Step 3 of explainer flow
 *
 * Layout:
 * - Top 50%: Hero image showing camera aperture with calibration
 * - Bottom 50%: Dark navy background with:
 *   - Step number 3
 *   - Heading "Calibrate in Camera"
 *   - Instruction text explaining the 9-point touch calibration
 *   - Next button (which goes to actual calibration)
 *
 * Teaches users:
 * - They'll see their grid through the camera
 * - Touch 9 corners in order (numbered 1-9)
 * - This creates the virtual scoring grid overlay
 */
@Composable
fun CameraCalibrationExplainerScreen(
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
            // Top 40%: Hero image showing phone with grid overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            ) {
                Image(
                    painter = painterResource(Res.drawable.camera),
                    contentDescription = "Phone showing 3x3 grid calibration",
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
                    text = "3",
                    style = MaterialTheme.typography.titleLarge,
                    color = BallStarsColor.TextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Heading text (native Compose)
            Text(
                text = "Calibrate in Camera",
                style = MaterialTheme.typography.headlineMedium,
                color = BallStarsColor.GlowCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Instruction text - detailed explanation
            Text(
                text = "You'll see your grid through the phone camera. Touch the screen to mark the 9 corners of your grid in order:",
                style = MaterialTheme.typography.bodyMedium,
                color = BallStarsColor.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Numbered instructions
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = BallStarsSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(BallStarsSpacing.sm)
            ) {
                CalibrationStep(
                    stepNumbers = "1-3",
                    description = "Top row: left, middle, right"
                )
                CalibrationStep(
                    stepNumbers = "4-6",
                    description = "Middle row: left, center, right"
                )
                CalibrationStep(
                    stepNumbers = "7-9",
                    description = "Bottom row: left, middle, right"
                )
            }

            Spacer(modifier = Modifier.height(BallStarsSpacing.md))

            // Additional tip
            Text(
                text = "Touch each corner precisely where your physical markers are. This creates the virtual scoring grid that tracks your kicks!",
                style = MaterialTheme.typography.bodySmall,
                color = BallStarsColor.TextSecondary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

                Spacer(modifier = Modifier.height(BallStarsSpacing.xl))

                // Next button (proceeds to actual calibration)
                Image(
                    painter = painterResource(Res.drawable.setup_button_next),
                    contentDescription = "Start Calibration",
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
 * Individual calibration step instruction
 */
@Composable
private fun CalibrationStep(
    stepNumbers: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step numbers badge
        Box(
            modifier = Modifier
                .width(50.dp)
                .background(
                    color = BallStarsColor.GlowCyan.copy(alpha = 0.2f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumbers,
                style = MaterialTheme.typography.labelLarge,
                color = BallStarsColor.GlowCyan,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(BallStarsSpacing.sm))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = BallStarsColor.TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}
