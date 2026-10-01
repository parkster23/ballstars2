package venturewave.one.gridgames.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * BallStars Color Palette
 *
 * Extracted from design reference (docs/design/ballstars-reference.png)
 * Street football theme with neon cyberpunk aesthetic
 */
object BallStarsColor {
    // Background colors
    val BgDeep = Color(0xFF0A1929)           // App background, deepest navy
    val Surface = Color(0xFF132A42)          // Cards, panels
    val SurfaceRaised = Color(0xFF1A3A56)    // List rows, nav bar, elevated elements
    val Outline = Color(0xFF2A5073)          // Card borders, dividers

    // Primary brand colors
    val Primary = Color(0xFF1ED36A)          // Street green - CTAs, headings, XP bar, active nav
    val GlowCyan = Color(0xFF00D9FF)         // Cyan glow - grid lines, tracking, focus rings, FAB
    val Gold = Color(0xFFFFC21A)             // Stars, move glyphs, XP text, success CTA
    val Flame = Color(0xFFFF7A1A)            // Streaks, motion trails, combo heat

    // Text colors
    val TextPrimary = Color(0xFFFFFFFF)      // Headings, primary text
    val TextSecondary = Color(0xFFB7C7D6)    // Body text, subtitles, timestamps

    // Semantic colors
    val Success = Primary                     // Success states (same as primary green)
    val Error = Color(0xFFFF4444)            // Errors, ball lost, bail states
    val Warning = Color(0xFFFFB800)          // Warnings, caution states

    // Glow variants (for multi-layer effects with BlendMode.Screen)
    val CyanGlow = GlowCyan.copy(alpha = 0.6f)
    val GoldGlow = Gold.copy(alpha = 0.7f)
    val FlameGlow = Flame.copy(alpha = 0.5f)
    val PrimaryGlow = Primary.copy(alpha = 0.6f)

    // Special states
    val ComboHeat1 = Color(0xFFFFAA00)       // 2x combo
    val ComboHeat2 = Color(0xFFFF7700)       // 3x combo
    val ComboHeat3 = Flame                    // 4x+ combo
    val SpecialGold = Color(0xFFFFD700)      // Special move activation

    // Grid overlay colors
    val GridLine = GlowCyan                   // Base grid lines
    val GridLineGlow = CyanGlow               // Grid line outer glow
    val TargetIdle = GlowCyan                 // Idle target cells
    val TargetActive = Gold                   // Current target cell
    val TargetHit = Color(0xFF00FF88)        // Just-hit cell flash

    // Badge/unlock colors
    val BadgeGold = Color(0xFFFFD700)
    val BadgeSilver = Color(0xFFC0C0C0)
    val BadgeBronze = Color(0xFFCD7F32)
    val Locked = Color(0xFF4A5568)           // Locked moves/modes

    // Gradient stops (for radial/linear gradients)
    val GradientStart = BgDeep
    val GradientMid = Surface
    val GradientEnd = SurfaceRaised
}
