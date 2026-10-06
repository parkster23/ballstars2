package venturewave.one.gridgames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import venturewave.one.gridgames.navigation.Screen
import venturewave.one.gridgames.navigation.rememberNavigationState
import venturewave.one.gridgames.ui.theme.BallStarsColor
import venturewave.one.gridgames.ui.GameScreen
import venturewave.one.gridgames.ui.FreestyleGameScreen
import venturewave.one.gridgames.ui.HomeMenuScreen
import venturewave.one.gridgames.ui.PatternSelectionScreen
import venturewave.one.gridgames.ui.ScoreScreen
import venturewave.one.gridgames.ui.screens.splash.SplashScreen
import venturewave.one.gridgames.ui.screens.splash.BallStarsSplashScreen
import venturewave.one.gridgames.ui.screens.menu.BallStarsMainMenuScreen
import venturewave.one.gridgames.ui.screens.intro.Intro1Screen
import venturewave.one.gridgames.ui.screens.explainer.SetupStartExplainerScreen
import venturewave.one.gridgames.ui.screens.explainer.GridCreatorExplainerScreen
import venturewave.one.gridgames.ui.screens.explainer.CameraCalibrationExplainerScreen
import venturewave.one.gridgames.ui.screens.onboarding.OnboardingScreen
import venturewave.one.gridgames.ui.screens.training.TrainingPatternSelectionScreen
import venturewave.one.gridgames.ui.screens.training.CreatePatternScreen
import venturewave.one.gridgames.ui.screens.profile.ProfileScreen
import venturewave.one.gridgames.ui.theme.BallStarsTheme

/**
 * Main app entry point with navigation
 *
 * Flow: Splash → Home → (Setup → GridCreator → CameraCalibration → ScanTargets2 | Training → PatternSelection → Game → Score | BestScores)
 * Onboarding screens removed from flow
 */
@Composable
@Preview
fun App() {
    BallStarsTheme {
        val navigationState = rememberNavigationState(initialScreen = Screen.Home)

        when (val screen = navigationState.currentScreen) {
            Screen.Splash -> BallStarsSplashScreen(
                onPlay = { navigationState.navigateTo(Screen.Home) }
            )

            Screen.MainMenu -> BallStarsMainMenuScreen(
                onTraining = { navigationState.navigateTo(Screen.PatternSelection) },
                onGameMode = { navigationState.navigateTo(Screen.Freestyle) },
                onSetupZone = { navigationState.navigateTo(Screen.SetupExplainer) },
                onStats = { navigationState.navigateTo(Screen.Stats) },
                onSettings = { navigationState.navigateTo(Screen.Settings) }
            )

            Screen.PatternSelection -> TrainingPatternSelectionScreen(
                onPatternSelected = { pattern ->
                    navigationState.navigateToGame(pattern)
                },
                onBack = { navigationState.navigateBack() },
                onCreatePattern = { navigationState.navigateTo(Screen.CreatePattern) },
                onHome = { navigationState.navigateTo(Screen.Home) },
                onProfile = { navigationState.navigateTo(Screen.Profile) }
            )

            Screen.CreatePattern -> CreatePatternScreen(
                onPatternCreated = {
                    // Navigate back to pattern selection
                    navigationState.navigateBack()
                },
                onBack = { navigationState.navigateBack() },
                onHome = { navigationState.navigateTo(Screen.Home) },
                onProfile = { navigationState.navigateTo(Screen.Profile) }
            )

            is Screen.Game -> GameScreen(
                pattern = screen.pattern,
                onGameComplete = { result ->
                    navigationState.navigateToScore(result)
                },
                onBackPressed = { navigationState.navigateBack() }
            )

            is Screen.Score -> ScoreScreen(
                result = screen.result,
                onPlayAgain = { navigationState.navigateTo(Screen.PatternSelection) },
                onBackToHome = { navigationState.navigateTo(Screen.Home) }
            )

            Screen.SetupExplainer -> SetupStartExplainerScreen(
                onNext = { navigationState.navigateTo(Screen.GridCreatorExplainer) },
                onBack = { navigationState.navigateTo(Screen.Home) },
                onHome = {
                    println("App.kt: SetupExplainer onHome callback triggered")
                    navigationState.navigateTo(Screen.Home)
                },
                onProfile = {
                    println("App.kt: SetupExplainer onProfile callback triggered")
                    navigationState.navigateTo(Screen.Profile)
                }
            )

            Screen.GridCreatorExplainer -> GridCreatorExplainerScreen(
                onNext = { navigationState.navigateTo(Screen.CameraCalibrationExplainer) },
                onBack = { navigationState.navigateBack() },
                onHome = {
                    println("App.kt: GridCreatorExplainer onHome callback triggered")
                    navigationState.navigateTo(Screen.Home)
                },
                onProfile = {
                    println("App.kt: GridCreatorExplainer onProfile callback triggered")
                    navigationState.navigateTo(Screen.Profile)
                }
            )

            Screen.CameraCalibrationExplainer -> CameraCalibrationExplainerScreen(
                onNext = { navigationState.navigateTo(Screen.ScanTargets2) },
                onBack = { navigationState.navigateBack() },
                onHome = { navigationState.navigateTo(Screen.Home) },
                onProfile = { navigationState.navigateTo(Screen.Profile) }
            )

            Screen.ScanTargets2 -> PlatformSpecificScanTargets3(
                onBack = { navigationState.navigateBack() },
                onGridCalibrated = { navigationState.navigateTo(Screen.Home) }
            )

            Screen.Home -> BallStarsMainMenuScreen(
                onTraining = { navigationState.navigateTo(Screen.PatternSelection) },
                onGameMode = { navigationState.navigateTo(Screen.Freestyle) },
                onSetupZone = { navigationState.navigateTo(Screen.ScanTargets2) },
                onSetupExplainer = { navigationState.navigateTo(Screen.SetupExplainer) },
                onStats = { navigationState.navigateTo(Screen.Stats) },
                onSettings = { navigationState.navigateTo(Screen.Settings) }
            )

            Screen.BestScores -> PlatformSpecificBestScores(
                onBack = { navigationState.navigateBack() }
            )

            Screen.Freestyle -> FreestyleGameScreen(
                onSessionComplete = { navigationState.navigateTo(Screen.Home) },
                onBackPressed = { navigationState.navigateBack() }
            )

            // Placeholder screens - to be implemented
            Screen.Stats -> PlaceholderScreen(
                title = "Stats",
                message = "Your statistics will appear here",
                onBack = { navigationState.navigateBack() }
            )

            Screen.Settings -> PlaceholderScreen(
                title = "Settings",
                message = "Settings will appear here",
                onBack = { navigationState.navigateBack() }
            )

            Screen.Profile -> ProfileScreen(
                onBack = { navigationState.navigateBack() }
            )

            // Onboarding and Intro1 screens removed from flow - redirect if somehow accessed
            Screen.Onboarding -> {
                // Skip onboarding, go directly to grid calibration
                navigationState.navigateTo(Screen.ScanTargets2)
            }

            Screen.Intro1 -> {
                // Skip intro, go home
                navigationState.navigateTo(Screen.Home)
            }

            // Fallback: if somehow any other screen is accessed, show main menu
            else -> {
                BallStarsSplashScreen(
                    onPlay = { navigationState.navigateTo(Screen.MainMenu) }
                )
            }
        }
    }
}

/**
 * Platform-specific scan targets 3 screen (Android: BallStars-branded manual grid calibration, others: placeholder)
 */
@Composable
expect fun PlatformSpecificScanTargets3(onBack: () -> Unit, onGridCalibrated: () -> Unit = {})

/**
 * Platform-specific best scores screen
 */
@Composable
expect fun PlatformSpecificBestScores(onBack: () -> Unit)

/**
 * Placeholder screen for未implemented features
 */
@Composable
fun PlaceholderScreen(
    title: String,
    message: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .background(BallStarsColor.BgDeep),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            androidx.compose.material3.Text(
                text = title,
                color = BallStarsColor.TextPrimary,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Text(
                text = message,
                color = BallStarsColor.TextSecondary,
                fontSize = 16.sp
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            androidx.compose.material3.Button(
                onClick = onBack,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = BallStarsColor.Primary
                )
            ) {
                androidx.compose.material3.Text("Go Back")
            }
        }
    }
}
