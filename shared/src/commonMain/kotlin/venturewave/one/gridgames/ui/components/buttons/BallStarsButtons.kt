package venturewave.one.gridgames.ui.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.theme.BallStarsShape
import venturewave.one.gridgames.ui.theme.BallStarsSpacing

/**
 * Primary Button - Solid green pill
 *
 * Used for main CTAs: "Get Started", "Next", "Start Game", etc.
 * Design reference: Onboarding screens, mode select
 *
 * @param text Button label text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether button is enabled
 * @param width Optional fixed width (null = wrap content)
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    width: Dp? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(BallStarsSpacing.touchTargetPrimary)
            .semantics { contentDescription = text },
        enabled = enabled,
        shape = BallStarsShape.ButtonPill,
        colors = ButtonDefaults.buttonColors(
            containerColor = BallStarsColor.Primary,
            contentColor = BallStarsColor.TextPrimary,
            disabledContainerColor = BallStarsColor.Primary.copy(alpha = 0.4f),
            disabledContentColor = BallStarsColor.TextPrimary.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(
            horizontal = BallStarsSpacing.lg,
            vertical = BallStarsSpacing.md
        )
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Success Button - Solid gold pill
 *
 * Used for completion/continue actions: "Continue", "Collect XP", "Claim Reward"
 * Design reference: Move success screen "+Continue" button
 *
 * @param text Button label text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether button is enabled
 * @param width Optional fixed width
 */
@Composable
fun SuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    width: Dp? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(BallStarsSpacing.touchTargetPrimary)
            .semantics { contentDescription = text },
        enabled = enabled,
        shape = BallStarsShape.ButtonPill,
        colors = ButtonDefaults.buttonColors(
            containerColor = BallStarsColor.Gold,
            contentColor = BallStarsColor.BgDeep, // Dark text on gold
            disabledContainerColor = BallStarsColor.Gold.copy(alpha = 0.4f),
            disabledContentColor = BallStarsColor.BgDeep.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(
            horizontal = BallStarsSpacing.lg,
            vertical = BallStarsSpacing.md
        )
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Outline Button - Cyan outlined pill
 *
 * Used for secondary/tertiary actions: "Back to home", "Skip", "Redo"
 * Design reference: Empty state "Back to home", grid setup "Redo"
 *
 * @param text Button label text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether button is enabled
 * @param outlineColor Color of the outline (default: cyan glow)
 * @param width Optional fixed width
 */
@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlineColor: Color = BallStarsColor.GlowCyan,
    width: Dp? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(BallStarsSpacing.touchTargetPrimary)
            .semantics { contentDescription = text },
        enabled = enabled,
        shape = BallStarsShape.ButtonPill,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = outlineColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = outlineColor.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (enabled) outlineColor else outlineColor.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(
            horizontal = BallStarsSpacing.lg,
            vertical = BallStarsSpacing.md
        )
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * Text Button - No background, just text
 *
 * Used for tertiary actions where visual weight should be minimal
 *
 * @param text Button label text
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Whether button is enabled
 * @param textColor Color of text (default: text secondary)
 */
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = BallStarsColor.TextSecondary
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier
            .height(BallStarsSpacing.touchTargetMin)
            .semantics { contentDescription = text },
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = textColor,
            disabledContentColor = textColor.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(
            horizontal = BallStarsSpacing.md,
            vertical = BallStarsSpacing.sm
        )
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge
        )
    }
}
