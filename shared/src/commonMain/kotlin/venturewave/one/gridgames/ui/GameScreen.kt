package venturewave.one.gridgames.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import venturewave.one.gridgames.model.GameResult
import venturewave.one.gridgames.model.TrainingPattern

/**
 * Game screen - platform-specific implementation required
 *
 * Android: Shows camera preview with target detection and live scoring
 * iOS: Placeholder (not implemented yet)
 *
 * ADAPTIVE LAYOUT PRINCIPLE:
 * - Platform-specific implementations must use weight-based layouts
 * - Target grid overlay must scale proportionally
 * - No fixed dp sizing for game elements
 */
@Composable
expect fun GameScreen(
    pattern: TrainingPattern,
    onGameComplete: (GameResult) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
)
