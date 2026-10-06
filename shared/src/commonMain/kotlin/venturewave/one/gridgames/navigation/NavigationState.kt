package venturewave.one.gridgames.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import venturewave.one.gridgames.model.GameResult
import venturewave.one.gridgames.model.TrainingPattern

/**
 * Navigation screens for GridGames app
 * Uses sealed class to represent different screens with their data
 *
 * Flow: Splash → Home → (SetupExplainer → GridCreatorExplainer → CameraCalibrationExplainer → ScanTargets2 | PatternSelection → Game → Score | BestScores)
 */
sealed class Screen {
    data object Splash : Screen()              // Initial splash screen with logo
    data object MainMenu : Screen()            // Main menu (Training/Game Mode/Setup Zone)
    data object Intro1 : Screen()              // First intro screen with hero image
    data object SetupExplainer : Screen()      // Setup & Start explainer screen (Step 1)
    data object GridCreatorExplainer : Screen() // Grid Creator explainer screen (Step 2)
    data object CameraCalibrationExplainer : Screen() // Camera calibration explainer screen (Step 3)
    data object Onboarding : Screen()          // First-time user onboarding (4 pages)
    data object Home : Screen()                // Home menu with options (legacy)
    data object ScanTargets : Screen()         // Camera-based target detection (Android-only)
    data object ScanTargets2 : Screen()        // OpenCV grid detection (Android-only)
    data object PatternSelection : Screen()    // Pattern selection for game
    data object CreatePattern : Screen()       // Create custom training pattern
    data class Game(val pattern: TrainingPattern) : Screen()  // Active game session
    data object BestScores : Screen()          // Leaderboard per pattern
    data class Score(val result: GameResult) : Screen()       // Post-game score screen
    data object Freestyle : Screen()           // Freestyle Gameplay Mode (no single pre-selected pattern)
    data object Stats : Screen()               // Stats screen
    data object Settings : Screen()            // Settings screen
    data object Profile : Screen()             // Player profile screen
}

/**
 * Navigation state holder
 * Manages current screen and navigation actions
 */
class NavigationState(initialScreen: Screen = Screen.Splash) {
    var currentScreen by mutableStateOf<Screen>(initialScreen)
        private set

    private val backStack = mutableListOf<Screen>()

    /**
     * Navigate to a new screen and add current screen to back stack
     */
    fun navigateTo(screen: Screen) {
        backStack.add(currentScreen)
        currentScreen = screen
    }

    /**
     * Navigate back to previous screen
     * Returns true if navigation occurred, false if already at root
     */
    fun navigateBack(): Boolean {
        return if (backStack.isNotEmpty()) {
            currentScreen = backStack.removeLast()
            true
        } else {
            false
        }
    }

    // Convenience methods for common navigation actions
    fun navigateToHome() {
        navigateTo(Screen.Home)
    }

    fun navigateToGame(pattern: TrainingPattern) {
        navigateTo(Screen.Game(pattern))
    }

    fun navigateToScore(result: GameResult) {
        navigateTo(Screen.Score(result))
    }
}

/**
 * Remember navigation state across recomposition
 */
@Composable
fun rememberNavigationState(initialScreen: Screen = Screen.Splash): NavigationState {
    return remember { NavigationState(initialScreen) }
}
