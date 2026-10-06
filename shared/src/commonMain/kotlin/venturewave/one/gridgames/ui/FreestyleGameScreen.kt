package venturewave.one.gridgames.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import venturewave.one.gridgames.model.FreestyleResult

/**
 * Freestyle Gameplay Mode screen - platform-specific implementation required.
 *
 * Unlike GameScreen (Training Mode), there's no single pre-selected
 * pattern to track: the player moves the ball freely around the grid and
 * the app recognizes in real time whenever any of the known patterns is
 * completed, chaining recognized patterns into combos. See
 * GAME_MECHANICS_PROPOSAL.md section 2 and FreestyleEngine.kt.
 */
@Composable
expect fun FreestyleGameScreen(
    onSessionComplete: (FreestyleResult) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
)
