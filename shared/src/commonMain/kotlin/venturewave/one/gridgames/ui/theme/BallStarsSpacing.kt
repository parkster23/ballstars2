package venturewave.one.gridgames.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * BallStars Spacing System
 *
 * Consistent spacing scale for padding, margins, gaps.
 * Based on 4dp grid for Material Design alignment.
 */
object BallStarsSpacing {
    val xs: Dp = 4.dp      // Tiny gaps, fine-tuning
    val sm: Dp = 8.dp      // Small gaps between related elements
    val md: Dp = 16.dp     // Default spacing, padding
    val lg: Dp = 24.dp     // Large gaps, section spacing
    val xl: Dp = 32.dp     // Extra large, screen padding
    val xxl: Dp = 48.dp    // Massive gaps, hero spacing

    // Semantic spacing
    val contentPadding = md                // Default content padding
    val cardPadding = lg                   // Card internal padding
    val screenPadding = lg                 // Screen edge padding
    val sectionGap = xl                    // Gap between major sections
    val listItemGap = sm                   // Gap between list items
    val chipGap = sm                       // Gap between chips

    // Touch target sizes (for accessibility)
    val touchTargetMin: Dp = 48.dp        // Minimum touch target (WCAG)
    val touchTargetPrimary: Dp = 56.dp    // Primary action buttons
    val touchTargetLarge: Dp = 64.dp      // FAB, major CTAs
}
