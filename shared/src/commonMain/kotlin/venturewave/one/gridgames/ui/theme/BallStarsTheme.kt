package venturewave.one.gridgames.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * BallStars Theme
 *
 * Main theme composable for the BallStars app.
 * - Street football aesthetic with neon cyberpunk elements
 * - Dark-only theme (no light mode, street football is played at dusk)
 * - Custom color palette, typography (Lilita One + Nunito), shapes, and spacing
 *
 * Usage:
 * ```
 * @Composable
 * fun App() {
 *     BallStarsTheme {
 *         // Your content
 *     }
 * }
 * ```
 */

private val BallStarsDarkColorScheme = darkColorScheme(
    // Primary brand colors
    primary = BallStarsColor.Primary,              // Street green for CTAs
    onPrimary = BallStarsColor.TextPrimary,       // White text on green
    primaryContainer = BallStarsColor.Primary.copy(alpha = 0.2f),
    onPrimaryContainer = BallStarsColor.Primary,

    // Secondary (Cyan glow)
    secondary = BallStarsColor.GlowCyan,
    onSecondary = BallStarsColor.BgDeep,
    secondaryContainer = BallStarsColor.GlowCyan.copy(alpha = 0.2f),
    onSecondaryContainer = BallStarsColor.GlowCyan,

    // Tertiary (Gold)
    tertiary = BallStarsColor.Gold,
    onTertiary = BallStarsColor.BgDeep,
    tertiaryContainer = BallStarsColor.Gold.copy(alpha = 0.2f),
    onTertiaryContainer = BallStarsColor.Gold,

    // Background & Surface
    background = BallStarsColor.BgDeep,
    onBackground = BallStarsColor.TextPrimary,
    surface = BallStarsColor.Surface,
    onSurface = BallStarsColor.TextPrimary,
    surfaceVariant = BallStarsColor.SurfaceRaised,
    onSurfaceVariant = BallStarsColor.TextSecondary,

    // Error
    error = BallStarsColor.Error,
    onError = BallStarsColor.TextPrimary,
    errorContainer = BallStarsColor.Error.copy(alpha = 0.2f),
    onErrorContainer = BallStarsColor.Error,

    // Outline & borders
    outline = BallStarsColor.Outline,
    outlineVariant = BallStarsColor.Outline.copy(alpha = 0.5f),

    // Surface tint (for elevation)
    surfaceTint = BallStarsColor.GlowCyan.copy(alpha = 0.1f)
)

/**
 * Composition local for accessing spacing values
 */
val LocalSpacing = staticCompositionLocalOf { BallStarsSpacing }

@Composable
fun BallStarsTheme(
    darkTheme: Boolean = true, // Always dark theme for street football aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = BallStarsDarkColorScheme // Force dark theme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ballStarsTypography(),
        shapes = ballStarsShapes(),
        content = {
            CompositionLocalProvider(LocalSpacing provides BallStarsSpacing) {
                content()
            }
        }
    )
}

/**
 * Helper extension to access spacing from any composable
 *
 * Usage:
 * ```
 * @Composable
 * fun MyScreen() {
 *     val spacing = LocalSpacing.current
 *     Column(modifier = Modifier.padding(spacing.md)) {
 *         // Content
 *     }
 * }
 * ```
 */
val spacing: BallStarsSpacing
    @Composable get() = LocalSpacing.current
