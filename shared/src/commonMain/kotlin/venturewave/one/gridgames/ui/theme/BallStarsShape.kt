package venturewave.one.gridgames.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * BallStars Shape System
 *
 * Extracted from design reference (docs/design/ballstars-reference.png)
 * - Pill buttons (50% rounded)
 * - Large rounded cards (24dp)
 * - Medium cards (16dp)
 * - Small chips (12dp)
 * - Circular elements (move glyphs, avatars, FAB)
 */
object BallStarsShape {
    // Button shapes
    val ButtonPill = RoundedCornerShape(percent = 50)      // Full pill shape for all buttons
    val ButtonCompact = RoundedCornerShape(20.dp)          // Slightly less rounded for compact buttons

    // Card shapes
    val CardLarge = RoundedCornerShape(24.dp)              // Main content cards, mode select cards
    val CardMedium = RoundedCornerShape(16.dp)             // List items, recent moves, stat cards
    val CardSmall = RoundedCornerShape(12.dp)              // Chips, badges, small containers

    // Navigation shapes
    val BottomNav = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )                                                       // Bottom navigation bar

    // Special shapes
    val Circle = CircleShape                                // Move glyphs, avatars, FAB
    val GridCell = RoundedCornerShape(8.dp)                // Grid overlay cells (subtle rounding)
    val ProgressBar = RoundedCornerShape(8.dp)             // XP bar, combo meter

    // Dialog/Modal shapes
    val Dialog = RoundedCornerShape(28.dp)                 // Full-screen modals, dialogs
    val Sheet = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )                                                       // Bottom sheets
}

/**
 * Material 3 Shapes configuration
 * Maps BallStars shapes to Material 3 size categories
 */
fun ballStarsShapes() = Shapes(
    extraSmall = BallStarsShape.CardSmall,    // Chips, small containers
    small = BallStarsShape.CardMedium,        // Cards, buttons
    medium = BallStarsShape.CardLarge,        // Large cards
    large = BallStarsShape.Dialog,            // Dialogs, bottom sheets
    extraLarge = BallStarsShape.Dialog        // Full-screen components
)
