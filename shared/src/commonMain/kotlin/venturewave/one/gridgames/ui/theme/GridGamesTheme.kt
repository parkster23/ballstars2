package venturewave.one.gridgames.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * BallStars-inspired color palette
 * Extracted from reference image for neon/cyberpunk aesthetic
 */
object GridGamesColors {
    // Background colors
    val DarkNavy = Color(0xFF0A1929)          // Main background
    val DeepBlue = Color(0xFF0D2137)          // Cards and surfaces

    // Primary BallStars theme colors
    val ElectricCyan = Color(0xFF00FFD9)      // Primary glow - vibrant cyan
    val BrightOrange = Color(0xFFFF6B35)      // Active/hit state - energetic orange
    val StarYellow = Color(0xFFFFD700)        // Particles and stars
    val VibrantPurple = Color(0xFFFF00FF)     // 3x combo color

    // Legacy accent colors (keep for compatibility)
    val NeonCyan = Color(0xFF00D9FF)          // Primary accent for highlights
    val NeonBlue = Color(0xFF00B4D8)          // Secondary accent for detection boxes
    val BrightGreen = Color(0xFF00FF88)       // Success states and buttons
    val GlowYellow = Color(0xFFFFD700)        // Highlights and warnings

    // Glow variants (for additive blending effects)
    val CyanGlow = ElectricCyan.copy(alpha = 0.6f)
    val OrangeGlow = BrightOrange.copy(alpha = 0.7f)
    val YellowGlow = StarYellow.copy(alpha = 0.5f)

    // Neutral colors
    val White = Color(0xFFFFFFFF)
    val WhiteAlpha70 = Color(0xB3FFFFFF)
    val BlackAlpha80 = Color(0xCC000000)
    val DarkGray = Color(0xFF2C2C2C)
}

/**
 * Shape definitions for consistent rounded corners
 */
object GridGamesShapes {
    val RoundedButton = RoundedCornerShape(12.dp)
    val Card = RoundedCornerShape(16.dp)
    val ProgressBar = RoundedCornerShape(8.dp)
    val SmallRounded = RoundedCornerShape(4.dp)
}

/**
 * Material 3 dark color scheme using GridGames palette
 */
private val GridGamesDarkColorScheme = darkColorScheme(
    primary = GridGamesColors.NeonCyan,
    secondary = GridGamesColors.NeonBlue,
    tertiary = GridGamesColors.BrightGreen,
    background = GridGamesColors.DarkNavy,
    surface = GridGamesColors.DeepBlue,
    onPrimary = GridGamesColors.White,
    onSecondary = GridGamesColors.White,
    onTertiary = GridGamesColors.DarkNavy,
    onBackground = GridGamesColors.White,
    onSurface = GridGamesColors.White,
    error = GridGamesColors.GlowYellow
)

/**
 * GridGames theme with BallStars-inspired design system
 *
 * Features:
 * - Dark navy background with neon accents
 * - Cyan/blue glow effects
 * - Rounded corners for modern look
 * - Material 3 theming for Compose compatibility
 */
@Composable
fun GridGamesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GridGamesDarkColorScheme,
        content = content
    )
}
