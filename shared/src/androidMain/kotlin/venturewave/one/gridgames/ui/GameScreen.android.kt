package venturewave.one.gridgames.ui

import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import grid_games_mobile.shared.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.tensorflow.lite.task.vision.detector.Detection
import venturewave.one.gridgames.data.GridRepository
import venturewave.one.gridgames.detection.BallDetectorHelper
import venturewave.one.gridgames.detection.BallDetectorViewModel
import venturewave.one.gridgames.model.GameResult
import venturewave.one.gridgames.model.TrainingPattern
import venturewave.one.gridgames.ui.theme.BallStarsColor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.text.NumberFormat

/**
 * Game phase states following BallStars HUD specification
 */
private enum class GamePhase {
    TIME_SELECTION,  // User selects 30 or 60 seconds
    COUNTDOWN,       // 10→1→GO countdown
    ACTIVE,          // Game in progress with timer counting down
    PAUSED,          // Game paused
    COMPLETE         // Game finished
}

private const val SHOW_DETECTION_DEBUG = false

private val GameplayNavy = Color(0xFF07131F)
private val GameplaySurface = Color(0xFF0E2236)
private val GameplayGreen = Color(0xFF1ED36A)
private val GameplayGold = Color(0xFFFFC21A)
private val GameplayOrange = Color(0xFFFF7A1A)
private val GameplayMagenta = Color(0xFFFF37E6)

/*
 * Typography: this file intentionally consumes MaterialTheme.typography so it
 * inherits the BallStars theme. Map title/headline styles to Alatsi and
 * body/label styles to Almarai in the shared theme rather than loading fonts
 * directly in this Android screen.
 */

/**
 * Android implementation of GameScreen
 * Integrates camera detection with game logic
 *
 * ADAPTIVE LAYOUT:
 * - Uses Box with weight-based sizing
 * - Target grid overlay scales with screen size
 * - Camera preview fills available space
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun GameScreen(
    pattern: TrainingPattern,
    onGameComplete: (GameResult) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Load calibrated grid - fetch fresh each time composable renders
    // Do NOT use remember{} here as it would cache the first value (possibly null)
    // and never update when user calibrates the grid later
    val calibratedGrid = GridRepository.getGrid()
    android.util.Log.d("GameScreen", "Grid loaded: ${if (calibratedGrid != null) "YES (${calibratedGrid.targets.size} targets, zoom=${calibratedGrid.zoomRatio})" else "NO (null)"}")

    // Camera control for zoom
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Screen size for coordinate transformation
    var screenSize by remember { mutableStateOf(IntSize(1080, 2424)) } // Default, will be updated

    // Apply calibration zoom level when camera is ready
    LaunchedEffect(camera, calibratedGrid) {
        camera?.let { cam ->
            calibratedGrid?.let { grid ->
                // Only apply zoom if it's a valid value (0.0 causes camera crash)
                if (grid.zoomRatio > 0f) {
                    try {
                        cam.cameraControl.setLinearZoom(grid.zoomRatio)
                        android.util.Log.i("GameScreen", "Applied calibration zoom: ${grid.zoomRatio}")
                    } catch (e: Exception) {
                        android.util.Log.e("GameScreen", "Failed to apply zoom: ${grid.zoomRatio}", e)
                    }
                } else {
                    android.util.Log.w("GameScreen", "Skipping invalid zoom ratio: ${grid.zoomRatio}")
                }
            }
        }
    }

    // BallStars HUD game phase state
    var gamePhase by remember { mutableStateOf(GamePhase.TIME_SELECTION) }
    var selectedTimeDuration by remember { mutableStateOf(30) } // 30 or 60 seconds
    var countdownValue by remember { mutableStateOf(10) } // For 10→1→GO countdown
    var timeRemaining by remember { mutableStateOf(30) } // Game timer in seconds

    // Game state
    var previewIndex by remember { mutableStateOf(0) }
    var currentTargetIndex by remember { mutableStateOf(0) }
    var hitCount by remember { mutableStateOf(0) }
    var missCount by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf(0L) }
    var gameActive by remember { mutableStateOf(false) }
    var lastHitTargetNumber by remember { mutableStateOf<Int?>(null) }  // Track last hit target for highlighting

    // Power-up and game mechanics state
    var currentStreak by remember { mutableStateOf(0) }
    var bestStreak by remember { mutableStateOf(0) }
    var stars by remember { mutableStateOf(0) }
    var showStarAnimation by remember { mutableStateOf(false) }
    var combo by remember { mutableStateOf(1) }
    var powerUpActive by remember { mutableStateOf(false) }
    var powerUpTimeRemaining by remember { mutableStateOf(0f) }
    var hitAnimationTrigger by remember { mutableStateOf<Int?>(null) } // Trigger for hit animation

    // Track which sequence positions have been hit in current iteration
    var hitPositionsInIteration by remember { mutableStateOf(setOf<Int>()) }

    // Create ViewModel for ball detection
    val viewModel = remember {
        BallDetectorViewModel(
            context = context,
            lifecycleOwner = lifecycleOwner,
            threshold = 0.20f, // Same as BallStars
            maxResults = 1,
            numberOfThreads = 4, // Increased for better CPU utilization
            currentDelegate = BallDetectorHelper.DELEGATE_GPU // Use GPU for hardware acceleration (Pixel 9 Adreno GPU)
        )
    }

    val detectionResults by viewModel.detectionResults.collectAsState()

    // Simon Says preview animation - highlight each target in sequence (during TIME_SELECTION phase)
    LaunchedEffect(gamePhase) {
        if (gamePhase == GamePhase.TIME_SELECTION && calibratedGrid != null) {
            while (gamePhase == GamePhase.TIME_SELECTION) {
                kotlinx.coroutines.delay(800) // Show each target for 800ms
                previewIndex = (previewIndex + 1) % pattern.targetSequence.size
            }
        }
    }

    // Check for ball hits on target boxes
    LaunchedEffect(detectionResults, calibratedGrid) {
        // Log detection results
        if (detectionResults.isNotEmpty()) {
            detectionResults.forEach { detection ->
                val score = detection.categories.firstOrNull()?.score ?: 0f
                val label = detection.categories.firstOrNull()?.label ?: "unknown"
                val box = detection.boundingBox
                android.util.Log.i("GameScreen", "BALL_DETECTION: score=$score label=$label box=[${box.left}, ${box.top}, ${box.right}, ${box.bottom}] center=[${box.centerX()}, ${box.centerY()}]")
            }
        }

        // Only detect hits during ACTIVE phase
        if (gamePhase != GamePhase.ACTIVE || calibratedGrid == null) {
            if (gamePhase != GamePhase.ACTIVE) {
                android.util.Log.d("GameScreen", "Game not in ACTIVE phase, skipping hit detection")
            }
            return@LaunchedEffect
        }

        // Loop the pattern - use modulo to repeat
        val actualTargetIndex = currentTargetIndex % pattern.targetSequence.size
        val currentTargetNumber = pattern.targetSequence[actualTargetIndex]
        val currentTargetBox = calibratedGrid[currentTargetNumber - 1] // Convert 1-based to 0-based index

        android.util.Log.d("GameScreen", "Current target: $currentTargetNumber (index $actualTargetIndex), box=[${currentTargetBox.x}, ${currentTargetBox.y}, ${currentTargetBox.width}, ${currentTargetBox.height}]")

        // Check if ball detection intersects with current target box
        val hit = detectionResults.any { detection ->
            val score = detection.categories.firstOrNull()?.score ?: 0f
            if (score < 0.20f) {
                android.util.Log.d("GameScreen", "Detection score too low: $score")
                return@any false
            }

            val ballBox = detection.boundingBox

            // Use actual tensor image dimensions (after rotation is applied)
            val tensorWidth = viewModel.tensorImageWidth.toFloat()
            val tensorHeight = viewModel.tensorImageHeight.toFloat()

            if (tensorWidth == 0f || tensorHeight == 0f) {
                android.util.Log.w("GameScreen", "Tensor dimensions not available yet")
                return@any false
            }

            // FIRST: Transform from calibration space to current game aperture space
            val sourceWidth = calibratedGrid.sourceViewportWidth
            val sourceHeight = calibratedGrid.sourceViewportHeight

            val calibrationToGameScaleX = if (sourceWidth > 0f) {
                screenSize.width.toFloat() / sourceWidth
            } else 1f

            val calibrationToGameScaleY = if (sourceHeight > 0f) {
                screenSize.height.toFloat() / sourceHeight
            } else 1f

            val gameTargetX = currentTargetBox.x * calibrationToGameScaleX
            val gameTargetY = currentTargetBox.y * calibrationToGameScaleY
            val gameTargetWidth = currentTargetBox.width * calibrationToGameScaleX
            val gameTargetHeight = currentTargetBox.height * calibrationToGameScaleY

            // SECOND: Transform from game aperture space to tensor space
            val gameToTensorScaleX = tensorWidth / screenSize.width.toFloat()
            val gameToTensorScaleY = tensorHeight / screenSize.height.toFloat()

            val targetCameraLeft = gameTargetX * gameToTensorScaleX
            val targetCameraTop = gameTargetY * gameToTensorScaleY
            val targetCameraRight = (gameTargetX + gameTargetWidth) * gameToTensorScaleX
            val targetCameraBottom = (gameTargetY + gameTargetHeight) * gameToTensorScaleY

            // Ball coordinates are already in tensor/camera space - use directly
            val ballCameraX = ballBox.centerX()
            val ballCameraY = ballBox.centerY()

            val isInside = ballCameraX >= targetCameraLeft && ballCameraX <= targetCameraRight &&
                ballCameraY >= targetCameraTop && ballCameraY <= targetCameraBottom

            android.util.Log.d("GameScreen", "Ball camera=[$ballCameraX, $ballCameraY] targetCamera=[L:$targetCameraLeft T:$targetCameraTop R:$targetCameraRight B:$targetCameraBottom] tensorSize=${tensorWidth}x${tensorHeight} screenSize=${screenSize.width}x${screenSize.height} inside=$isInside")

            isInside
        }

        if (hit) {
            // Check if this sequence position was already hit in current iteration
            val alreadyHitThisPosition = hitPositionsInIteration.contains(actualTargetIndex)

            // Only count as a new hit if we're not already on this target AND position not hit yet
            if (lastHitTargetNumber != currentTargetNumber && !alreadyHitThisPosition) {
                android.util.Log.i("GameScreen", "🎯 BALL HIT TARGET $currentTargetNumber at sequence position $actualTargetIndex!")

                // Mark this sequence position as hit
                hitPositionsInIteration = hitPositionsInIteration + actualTargetIndex

                hitCount++
                lastHitTargetNumber = currentTargetNumber
                currentTargetIndex++
                hitAnimationTrigger = currentTargetNumber // Trigger hit animation

                // Update streak
                currentStreak++
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak
                }

                // Award stars every 5 hits
                if (hitCount % 5 == 0) {
                    stars++
                    showStarAnimation = true
                    android.util.Log.i("GameScreen", "⭐ STAR EARNED! Total: $stars")
                }

                // Increase combo multiplier (max 3x)
                if (currentStreak >= 10) combo = 3
                else if (currentStreak >= 5) combo = 2
                else combo = 1

                // Check if we completed an iteration
                val newIterationCount = currentTargetIndex / pattern.targetSequence.size
                val previousIterationCount = (currentTargetIndex - 1) / pattern.targetSequence.size

                if (newIterationCount > previousIterationCount) {
                    // Completed iteration - activate power-up!
                    android.util.Log.i("GameScreen", "✅ ITERATION $newIterationCount COMPLETE! Starting iteration ${newIterationCount + 1}")

                    // Reset hit positions for new iteration
                    hitPositionsInIteration = setOf()

                    powerUpActive = true
                    powerUpTimeRemaining = 5f // 5 seconds of power-up
                }
            } else if (alreadyHitThisPosition) {
                android.util.Log.d("GameScreen", "⚠️ Position $actualTargetIndex already hit in this iteration - ignoring")
            }
        } else {
            // Ball not in any target box - clear last hit
            if (lastHitTargetNumber != null) {
                android.util.Log.d("GameScreen", "Ball left target $lastHitTargetNumber")
                lastHitTargetNumber = null
                // Reset streak when ball leaves target (missed shot)
                currentStreak = 0
                combo = 1
            }
        }
    }

    // Power-up timer countdown
    LaunchedEffect(powerUpActive) {
        if (powerUpActive) {
            while (powerUpTimeRemaining > 0) {
                kotlinx.coroutines.delay(100)
                powerUpTimeRemaining -= 0.1f
            }
            powerUpActive = false
        }
    }

    // Star animation auto-dismiss
    LaunchedEffect(showStarAnimation) {
        if (showStarAnimation) {
            kotlinx.coroutines.delay(1500)
            showStarAnimation = false
        }
    }

    // Hit flash is visual-only and should clear quickly after a registered hit.
    LaunchedEffect(hitAnimationTrigger) {
        if (hitAnimationTrigger != null) {
            delay(450)
            hitAnimationTrigger = null
        }
    }

    // Countdown timer (10→1→GO)
    LaunchedEffect(gamePhase) {
        if (gamePhase == GamePhase.COUNTDOWN) {
            countdownValue = 10
            while (countdownValue > 0) {
                delay(1000)
                countdownValue--
            }
            // Show "GO!" for 1 second
            delay(1000)
            // Start active game
            gamePhase = GamePhase.ACTIVE
            gameActive = true
            startTime = System.currentTimeMillis()
            timeRemaining = selectedTimeDuration
        }
    }

    // Game timer countdown (active phase)
    LaunchedEffect(gamePhase, timeRemaining) {
        if (gamePhase == GamePhase.ACTIVE && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
            if (timeRemaining <= 0) {
                // Game complete
                gamePhase = GamePhase.COMPLETE
                gameActive = false

                // Calculate final results
                val totalTime = System.currentTimeMillis() - startTime
                val totalTargets = hitCount + missCount
                val accuracy = if (totalTargets > 0) hitCount.toFloat() / totalTargets.toFloat() * 100f else 0f
                val score = calculateScore(totalTime, accuracy)

                // Full loops through pattern.targetSequence completed this
                // session (e.g. Triangles [5,7,4,5] hit once = 1). Same
                // completed-iteration math already used for power-up timing
                // above (currentTargetIndex advances by 1 per confirmed hit).
                val patternsCompleted = currentTargetIndex / pattern.targetSequence.size

                onGameComplete(
                    GameResult(
                        pattern = pattern,
                        score = score,
                        totalTime = totalTime,
                        hitCount = hitCount,
                        missCount = missCount,
                        accuracy = accuracy,
                        patternsCompleted = patternsCompleted
                    )
                )
            }
        }
    }

    // NOTE: Do NOT call viewModel.cleanup() here!
    // The CameraPreview's DisposableEffect handles stopProcessing() which is sufficient
    // for navigation away from GameScreen. cleanup() blocks with awaitTermination() and
    // causes crashes. The ViewModel's executors are left running to allow reuse.

    // BallStars gameplay screen: same visual frame as Target Detection, with a
    // calibration-compatible camera aperture and a dedicated lower HUD.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(GameplayNavy)
    ) {
        BallStarsGameplayBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val activeStep = (currentTargetIndex % pattern.targetSequence.size) + 1

            GameplayPatternHeader(
                patternName = pattern.name,
                currentStep = activeStep,
                totalSteps = pattern.targetSequence.size,
                gamePhase = gamePhase,
                onBack = onBackPressed,
                onPause = {
                    if (gamePhase == GamePhase.ACTIVE) gamePhase = GamePhase.PAUSED
                },
                onResume = {
                    if (gamePhase == GamePhase.PAUSED) gamePhase = GamePhase.ACTIVE
                }
            )

            Spacer(Modifier.height(8.dp))

            // CAMERA / PLAY AREA
            // Aperture is pinned to fillMaxWidth(0.88f) + aspectRatio(3f/4f) — the
            // SAME formula used by ScanTargets3Screen's CameraAperture — so both
            // screens show an identical crop of the 4:3 camera feed on a given
            // device. This is required for calibrated GridTarget coordinates to
            // line up; deriving width/height independently (or from leftover
            // layout space) lets FILL_CENTER crop the sensor feed differently
            // between the two screens even when the overlay's own coordinates
            // are correctly scaled.
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val apertureShape = RoundedCornerShape(24.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(3f / 4f)
                        .background(Color.Black) // Dark fallback during camera initialization
                        .shadow(
                            elevation = 12.dp,
                            shape = apertureShape,
                            ambientColor = BallStarsColor.GlowCyan,
                            spotColor = BallStarsColor.GlowCyan
                        )
                        .onSizeChanged { size ->
                            // Detection and calibrated targets MUST share this exact viewport.
                            screenSize = size
                            android.util.Log.d(
                                "GameScreen",
                                "GAME VIEWPORT = ${size.width}x${size.height}"
                            )
                        }
                ) {
                    CameraPreview(
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        viewModel = viewModel,
                        onCameraReady = { cam -> camera = cam },
                        modifier = Modifier.fillMaxSize()
                        // NOTE: .clip() removed - doesn't work on AndroidView
                        // Clipping is handled at the Android View level via clipToOutline
                    )

                    if (calibratedGrid != null) {
                        val actualTargetIndex = currentTargetIndex % pattern.targetSequence.size
                        val activeTarget = when (gamePhase) {
                            GamePhase.TIME_SELECTION -> pattern.targetSequence[previewIndex]
                            GamePhase.COUNTDOWN,
                            GamePhase.ACTIVE,
                            GamePhase.PAUSED -> pattern.targetSequence[actualTargetIndex]
                            GamePhase.COMPLETE -> null
                        }

                        CalibratedGridOverlay(
                            calibratedGrid = calibratedGrid,
                            currentTarget = activeTarget,
                            completedTarget = hitAnimationTrigger,
                            isPreviewMode = gamePhase == GamePhase.TIME_SELECTION,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (SHOW_DETECTION_DEBUG) {
                        BallDetectionDebugOverlay(
                            detectionResults = detectionResults,
                            screenSize = screenSize,
                            tensorWidth = viewModel.tensorImageWidth,
                            tensorHeight = viewModel.tensorImageHeight,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (gamePhase == GamePhase.COUNTDOWN) {
                        CountdownOverlay(countdownValue = countdownValue)
                    }

                    if (gamePhase == GamePhase.PAUSED) {
                        PauseOverlay(
                            onResume = { gamePhase = GamePhase.ACTIVE }
                        )
                    }

                    if (showStarAnimation) {
                        StarEarnedOverlay()
                    }
                }

                // Border is deliberately outside the clipped camera container so it
                // always remains visible above PreviewView.
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(3f / 4f)
                        .border(
                            width = 4.dp,
                            color = BallStarsColor.GlowCyan,
                            shape = RoundedCornerShape(24.dp)
                        )
                )
            }

            Spacer(Modifier.height(10.dp))

            // BOTTOM ~30% HUD
            when (gamePhase) {
                GamePhase.TIME_SELECTION -> {
                    TimeSelectionHud(
                        selectedSeconds = selectedTimeDuration,
                        onSelectSeconds = { selectedTimeDuration = it },
                        onStart = { gamePhase = GamePhase.COUNTDOWN },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                GamePhase.COUNTDOWN,
                GamePhase.ACTIVE,
                GamePhase.PAUSED,
                GamePhase.COMPLETE -> {
                    val accuracy = if (hitCount + missCount > 0) {
                        hitCount.toFloat() / (hitCount + missCount).toFloat() * 100f
                    } else 0f
                    val elapsedForHud = if (gameActive) {
                        (selectedTimeDuration - timeRemaining).coerceAtLeast(0) * 1000L
                    } else 0L
                    val liveScore = calculateScore(elapsedForHud, accuracy)
                    val displaySeconds = if (gamePhase == GamePhase.COUNTDOWN) {
                        selectedTimeDuration
                    } else {
                        timeRemaining
                    }

                    GameplayHud(
                        timeRemaining = displaySeconds,
                        score = liveScore,
                        streak = currentStreak,
                        lastHitTarget = lastHitTargetNumber,
                        stars = stars,
                        combo = combo,
                        currentStepIndex = currentTargetIndex % pattern.targetSequence.size,
                        totalSteps = pattern.targetSequence.size,
                        powerUpActive = powerUpActive,
                        powerUpTimeRemaining = powerUpTimeRemaining,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BallStarsGameplayBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF17334D),
                            GameplayNavy,
                            Color(0xFF081827)
                        )
                    )
                )
        )

        // This artwork is a single complete scene (both characters, the ball,
        // the BALL STARS logo) at a native ~461x342 (461f/342f) aspect ratio —
        // NOT header-banner art. A fixed height() here would crop a different
        // amount of the image on every device, since fillMaxWidth() makes the
        // box's aspect ratio vary by screen width while the source image's
        // aspect ratio stays fixed. Locking the box to the image's own aspect
        // ratio instead guarantees the full scene is visible on any screen
        // size, with zero cropping, rather than "less cropping."
        Image(
            painter = painterResource(Res.drawable.target_header_background),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(461f / 342f)
                .align(Alignment.TopCenter),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        Image(
            painter = painterResource(Res.drawable.target_left_edge),
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .width(58.dp)
                .align(Alignment.CenterStart),
            contentScale = ContentScale.FillHeight
        )

        Image(
            painter = painterResource(Res.drawable.target_right_edge),
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .width(58.dp)
                .align(Alignment.CenterEnd),
            contentScale = ContentScale.FillHeight
        )
    }
}

@Composable
private fun GameplayPatternHeader(
    patternName: String,
    currentStep: Int,
    totalSteps: Int,
    gamePhase: GamePhase,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .heightIn(min = 62.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = BallStarsColor.GlowCyan,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(4.dp))

        Surface(
            modifier = Modifier.weight(1f),
            color = GameplayNavy.copy(alpha = 0.94f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, BallStarsColor.GlowCyan.copy(alpha = 0.85f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(Res.drawable.training_icon),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = patternName.uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = "STEP $currentStep OF $totalSteps",
                        color = BallStarsColor.GlowCyan,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                HeaderProgressDots(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    modifier = Modifier.widthIn(max = 104.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        if (gamePhase in listOf(GamePhase.ACTIVE, GamePhase.PAUSED)) {
            Image(
                painter = painterResource(
                    if (gamePhase == GamePhase.PAUSED) Res.drawable.play_button
                    else Res.drawable.pause_button
                ),
                contentDescription = if (gamePhase == GamePhase.PAUSED) "Resume" else "Pause",
                modifier = Modifier
                    .size(54.dp)
                    .clickable {
                        if (gamePhase == GamePhase.PAUSED) onResume() else onPause()
                    }
            )
        } else {
            Spacer(Modifier.size(54.dp))
        }
    }
}

@Composable
private fun HeaderProgressDots(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    if (totalSteps <= 0) return
    val shown = min(totalSteps, 5)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(shown) { index ->
            val step = index + 1
            val color = when {
                step < currentStep -> GameplayGreen
                step == currentStep -> BallStarsColor.GlowCyan
                else -> Color(0xFF4A6680)
            }
            Box(
                modifier = Modifier
                    .size(if (step == currentStep) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun TimeSelectionHud(
    selectedSeconds: Int,
    onSelectSeconds: (Int) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 18.dp, vertical = 4.dp),
        color = GameplayNavy.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
        border = BorderStroke(1.5.dp, BallStarsColor.GlowCyan.copy(alpha = 0.78f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "SELECT ROUND TIME",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoundTimeChoice(
                    text = "30 SEC",
                    selected = selectedSeconds == 30,
                    onClick = { onSelectSeconds(30) },
                    modifier = Modifier.weight(1f)
                )
                RoundTimeChoice(
                    text = "60 SEC",
                    selected = selectedSeconds == 60,
                    onClick = { onSelectSeconds(60) },
                    modifier = Modifier.weight(1f)
                )
            }

            Image(
                painter = painterResource(Res.drawable.play_button),
                contentDescription = "Start game",
                modifier = Modifier
                    .size(76.dp)
                    .clickable(onClick = onStart)
            )
        }
    }
}

@Composable
private fun RoundTimeChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick),
        color = if (selected) GameplayGreen.copy(alpha = 0.18f) else GameplaySurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            if (selected) 2.5.dp else 1.5.dp,
            if (selected) GameplayGreen else BallStarsColor.GlowCyan.copy(alpha = 0.55f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) GameplayGreen else Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CountdownOverlay(countdownValue: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "countdownPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "countdownScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(142.dp)
                .scale(scale)
                .drawBehind {
                    drawCircle(BallStarsColor.GlowCyan.copy(alpha = 0.15f), radius = size.minDimension * 0.55f)
                    drawCircle(BallStarsColor.GlowCyan.copy(alpha = 0.34f), radius = size.minDimension * 0.46f, style = Stroke(width = 12f))
                    drawCircle(GameplayNavy.copy(alpha = 0.88f), radius = size.minDimension * 0.40f)
                    drawCircle(
                        if (countdownValue > 0) BallStarsColor.GlowCyan else GameplayGreen,
                        radius = size.minDimension * 0.40f,
                        style = Stroke(width = 5f)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (countdownValue > 0) countdownValue.toString() else "GO!",
                color = if (countdownValue > 0) Color.White else GameplayGreen,
                fontSize = if (countdownValue > 0) 72.sp else 46.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = BallStarsColor.GlowCyan.copy(alpha = 0.8f),
                        blurRadius = 16f
                    )
                )
            )
        }
    }
}

@Composable
private fun PauseOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameplayNavy.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = GameplayNavy.copy(alpha = 0.94f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, BallStarsColor.GlowCyan.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 34.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "PAUSED",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Image(
                    painter = painterResource(Res.drawable.play_button),
                    contentDescription = "Resume",
                    modifier = Modifier
                        .size(82.dp)
                        .clickable(onClick = onResume)
                )
            }
        }
    }
}

@Composable
private fun StarEarnedOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = GameplayNavy.copy(alpha = 0.90f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(2.dp, GameplayGold),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.star_icon),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = "STAR EARNED!",
                    color = GameplayGold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun GameplayHud(
    timeRemaining: Int,
    score: Int,
    streak: Int,
    lastHitTarget: Int?,
    stars: Int,
    combo: Int,
    currentStepIndex: Int,
    totalSteps: Int,
    powerUpActive: Boolean,
    powerUpTimeRemaining: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 18.dp, vertical = 2.dp),
        color = GameplayNavy.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
        border = BorderStroke(1.5.dp, BallStarsColor.GlowCyan.copy(alpha = 0.72f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudMetric(
                    icon = Res.drawable.timer_icon,
                    label = "TIME LEFT",
                    value = "%02d:%02d".format(timeRemaining / 60, timeRemaining % 60),
                    valueColor = if (timeRemaining <= 10) GameplayOrange else BallStarsColor.GlowCyan,
                    modifier = Modifier.weight(1f)
                )
                HudDivider()
                HudMetric(
                    icon = Res.drawable.score_trophy_icon,
                    label = "SCORE",
                    value = NumberFormat.getIntegerInstance().format(score),
                    valueColor = GameplayGold,
                    modifier = Modifier.weight(1f)
                )
                HudDivider()
                HudMetric(
                    icon = Res.drawable.streak_flame_icon,
                    label = "STREAK",
                    value = streak.toString(),
                    valueColor = when {
                        streak >= 10 -> GameplayOrange
                        streak >= 5 -> GameplayGold
                        else -> Color.White
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryHudMetric(
                    icon = Res.drawable.lasthit_target_icon,
                    label = "LAST HIT",
                    value = lastHitTarget?.let { "#$it" } ?: "–",
                    valueColor = GameplayGreen,
                    modifier = Modifier.weight(1f)
                )
                SecondaryHudMetric(
                    icon = Res.drawable.star_icon,
                    label = "STARS",
                    value = stars.toString(),
                    valueColor = GameplayGold,
                    modifier = Modifier.weight(1f)
                )
                SecondaryHudMetric(
                    icon = Res.drawable.combo_lightning_icon,
                    label = "COMBO",
                    value = "${combo}x",
                    valueColor = when (combo) {
                        3 -> GameplayMagenta
                        2 -> BallStarsColor.GlowCyan
                        else -> Color.White
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (powerUpActive) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(50))
                        .background(GameplayGold.copy(alpha = 0.15f))
                        .border(1.dp, GameplayGold.copy(alpha = 0.65f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.combo_lightning_icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "POWER-UP ${powerUpTimeRemaining.toInt()}s",
                        color = GameplayGold,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            PatternProgressStrip(
                currentStepIndex = currentStepIndex,
                totalSteps = totalSteps,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HudMetric(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = label,
                color = Color(0xFFAFC6D9),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = valueColor.copy(alpha = 0.35f),
                    blurRadius = 8f
                )
            )
        )
    }
}

@Composable
private fun HudDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(76.dp)
            .background(BallStarsColor.GlowCyan.copy(alpha = 0.24f))
    )
}

@Composable
private fun SecondaryHudMetric(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 70.dp),
        color = GameplaySurface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BallStarsColor.GlowCyan.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(7.dp))
            Column {
                Text(
                    text = label,
                    color = Color(0xFFAFC6D9),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    color = valueColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun PatternProgressStrip(
    currentStepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    if (totalSteps <= 0) return
    Canvas(
        modifier = modifier
            .height(30.dp)
            .padding(horizontal = 10.dp)
    ) {
        val count = totalSteps.coerceAtLeast(1)
        val startX = 14.dp.toPx()
        val endX = size.width - 14.dp.toPx()
        val y = size.height / 2f
        val step = if (count <= 1) 0f else (endX - startX) / (count - 1)

        drawLine(
            color = Color(0xFF52718B),
            start = Offset(startX, y),
            end = Offset(endX, y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        if (count > 1 && currentStepIndex > 0) {
            val completedEnd = startX + step * currentStepIndex.coerceAtMost(count - 1)
            drawLine(
                color = GameplayGreen,
                start = Offset(startX, y),
                end = Offset(completedEnd, y),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        repeat(count) { i ->
            val x = if (count == 1) size.width / 2f else startX + step * i
            val completed = i < currentStepIndex
            val current = i == currentStepIndex
            if (current) {
                drawCircle(BallStarsColor.GlowCyan.copy(alpha = 0.20f), 11.dp.toPx(), Offset(x, y))
            }
            drawCircle(
                color = when {
                    completed -> GameplayGreen
                    current -> BallStarsColor.GlowCyan
                    else -> Color(0xFF52718B)
                },
                radius = if (current) 7.dp.toPx() else 6.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = if (current) 3.dp.toPx() else 2.dp.toPx())
            )
            if (completed) {
                drawCircle(GameplayGreen, 3.dp.toPx(), Offset(x, y))
            }
        }
    }
}

/**
 * CameraX preview component for ball detection
 */
@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    viewModel: BallDetectorViewModel,
    onCameraReady: (androidx.camera.core.Camera) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val previewView = remember(context) {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val imageAnalysis = remember(viewModel) {
        viewModel.getImageAnalysisUseCase()
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    DisposableEffect(lifecycleOwner, previewView, imageAnalysis) {
        var provider: ProcessCameraProvider? = null
        var previewUseCase: Preview? = null
        var disposed = false

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        android.util.Log.i("CameraPreview", "GAME CAMERA BIND START")

        cameraProviderFuture.addListener({
            if (disposed) {
                android.util.Log.i("CameraPreview", "Camera provider returned after dispose — ignoring")
                return@addListener
            }

            try {
                val cameraProvider = cameraProviderFuture.get()
                provider = cameraProvider

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build()

                previewUseCase = preview
                preview.setSurfaceProvider(previewView.surfaceProvider)

                // ABSOLUTELY NO unbindAll() HERE
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

                android.util.Log.i("CameraPreview", "GAME CAMERA BOUND SUCCESSFULLY")
                onCameraReady(camera)

            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "GAME CAMERA BIND FAILED", e)
            }
        }, executor)

        onDispose {
            disposed = true
            android.util.Log.i("CameraPreview", "GAME CAMERA DISPOSE")

            try {
                if (provider != null && previewUseCase != null) {
                    provider!!.unbind(previewUseCase!!, imageAnalysis)
                    android.util.Log.i("CameraPreview", "GAME CAMERA UNBOUND")
                }
            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "GAME CAMERA UNBIND FAILED", e)
            }
        }
    }
}

/**
 * Enhanced BallStars-themed grid overlay with vibrant glowing effects
 * Features multi-layer glow, breathing animations, and explosive hit feedback
 */
@Composable
private fun CalibratedGridOverlay(
    calibratedGrid: venturewave.one.gridgames.model.CalibratedGrid,
    currentTarget: Int? = null,
    completedTargets: List<Int> = emptyList(),
    completedTarget: Int? = null,
    isPreviewMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "activeTargetPulse")
    val activeScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activeScale"
    )
    val activeGlow by transition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activeGlow"
    )

    Canvas(modifier = modifier) {
        if (calibratedGrid.targets.size < 9) {
            android.util.Log.w("GridOverlay", "Not enough targets: ${calibratedGrid.targets.size}, expected 9")
            return@Canvas
        }

        // Calculate scale from calibration viewport to current game viewport
        val sourceWidth = calibratedGrid.sourceViewportWidth
        val sourceHeight = calibratedGrid.sourceViewportHeight

        val scaleX = if (sourceWidth > 0f) size.width / sourceWidth else 1f
        val scaleY = if (sourceHeight > 0f) size.height / sourceHeight else 1f

        if (sourceWidth <= 0f || sourceHeight <= 0f) {
            android.util.Log.e(
                "GridOverlay",
                "Invalid calibration viewport (${sourceWidth}x${sourceHeight}) - grid will not align correctly. Please recalibrate."
            )
        }

        android.util.Log.d(
            "GridOverlay",
            "GRID SCALE = ${scaleX}x${scaleY} (source: ${sourceWidth}x${sourceHeight}, current: ${size.width}x${size.height})"
        )

        // Transform all targets to current viewport
        val centers = (0 until 9).map { i ->
            val target = calibratedGrid[i]

            val x = target.x * scaleX
            val y = target.y * scaleY
            val width = target.width * scaleX
            val height = target.height * scaleY

            Offset(x + width / 2f, y + height / 2f)
        }

        // Square, cross and diagonal network matching the setup-grid concept.
        val connections = listOf(
            0 to 1, 1 to 2,
            3 to 4, 4 to 5,
            6 to 7, 7 to 8,
            0 to 3, 3 to 6,
            1 to 4, 4 to 7,
            2 to 5, 5 to 8,
            0 to 4, 4 to 8,
            2 to 4, 4 to 6
        )

        // Soft glow under the whole wireframe.
        connections.forEach { (from, to) ->
            drawLine(
                color = BallStarsColor.GlowCyan.copy(alpha = 0.10f),
                start = centers[from],
                end = centers[to],
                strokeWidth = 9.dp.toPx(),
                cap = StrokeCap.Round,
                blendMode = BlendMode.Screen
            )
        }
        connections.forEach { (from, to) ->
            drawLine(
                color = BallStarsColor.GlowCyan.copy(alpha = 0.56f),
                start = centers[from],
                end = centers[to],
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        for (i in 0 until 9) {
            val targetNumber = i + 1
            val target = calibratedGrid[i]
            val center = centers[i]

            // Transform target dimensions to current viewport
            val transformedWidth = target.width * scaleX
            val transformedHeight = target.height * scaleY

            val baseRadius = (min(transformedWidth, transformedHeight) * 0.24f)
                .coerceAtLeast(12.dp.toPx())
                .coerceAtMost(28.dp.toPx())

            val isCurrent = currentTarget == targetNumber
            val isHit = targetNumber == completedTarget || completedTargets.contains(targetNumber)

            when {
                isHit -> {
                    drawCircle(
                        color = GameplayGold.copy(alpha = 0.20f),
                        radius = baseRadius * 1.65f,
                        center = center,
                        blendMode = BlendMode.Screen
                    )
                    drawCircle(
                        color = GameplayOrange.copy(alpha = 0.48f),
                        radius = baseRadius * 1.35f,
                        center = center,
                        style = Stroke(width = 7.dp.toPx()),
                        blendMode = BlendMode.Screen
                    )
                    drawCircle(
                        color = GameplayGreen,
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(Color.White, baseRadius * 0.22f, center)
                }

                isCurrent -> {
                    val r = baseRadius * activeScale
                    drawCircle(
                        color = GameplayGreen.copy(alpha = 0.16f * activeGlow),
                        radius = r * 1.75f,
                        center = center,
                        blendMode = BlendMode.Screen
                    )
                    drawCircle(
                        color = BallStarsColor.GlowCyan.copy(alpha = 0.32f * activeGlow),
                        radius = r * 1.45f,
                        center = center,
                        style = Stroke(width = 8.dp.toPx()),
                        blendMode = BlendMode.Screen
                    )
                    drawCircle(
                        color = GameplayGreen.copy(alpha = 0.88f),
                        radius = r,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.96f),
                        radius = r * 0.22f,
                        center = center
                    )
                    drawCircle(
                        color = BallStarsColor.GlowCyan,
                        radius = r * 1.06f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                else -> {
                    drawCircle(
                        color = BallStarsColor.GlowCyan.copy(alpha = 0.10f),
                        radius = baseRadius * 1.45f,
                        center = center,
                        blendMode = BlendMode.Screen
                    )
                    drawCircle(
                        color = BallStarsColor.GlowCyan.copy(alpha = 0.62f),
                        radius = baseRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.88f),
                        radius = baseRadius * 0.18f,
                        center = center
                    )
                }
            }
        }
    }
}

/**
 * Debug overlay to visualize ball detection bounding boxes
 * Shows where the ball is detected in camera space, mapped to screen space for display
 */
@Composable
private fun BallDetectionDebugOverlay(
    detectionResults: List<Detection>,
    screenSize: IntSize,
    tensorWidth: Int,
    tensorHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (detectionResults.isNotEmpty() && tensorWidth > 0 && tensorHeight > 0) {
            detectionResults.forEach { detection ->
                val ballBox = detection.boundingBox
                val score = detection.categories.firstOrNull()?.score ?: 0f

                // Transform from tensor space (after rotation) to screen space for DISPLAY only
                val scaleX = screenSize.width.toFloat() / tensorWidth.toFloat()
                val scaleY = screenSize.height.toFloat() / tensorHeight.toFloat()

                // Transform ball bounding box to screen coordinates for visualization
                val ballScreenLeft = ballBox.left * scaleX
                val ballScreenTop = ballBox.top * scaleY
                val ballScreenRight = ballBox.right * scaleX
                val ballScreenBottom = ballBox.bottom * scaleY

                // Draw red box around detected ball
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(ballScreenLeft, ballScreenTop),
                    size = Size(
                        width = ballScreenRight - ballScreenLeft,
                        height = ballScreenBottom - ballScreenTop
                    ),
                    style = Stroke(width = 6f)
                )

                // Draw center point
                drawCircle(
                    color = Color.Red,
                    radius = 10f,
                    center = Offset(
                        (ballScreenLeft + ballScreenRight) / 2f,
                        (ballScreenTop + ballScreenBottom) / 2f
                    )
                )
            }
        }
    }
}

/**
 * Calculate score based on time and accuracy
 * Faster time + higher accuracy = higher score
 */
private fun calculateScore(totalTimeMs: Long, accuracy: Float): Int {
    val baseScore = 1000
    val timeBonus = maxOf(0, (30000 - totalTimeMs) / 100).toInt() // Bonus for finishing under 30s
    val accuracyBonus = (accuracy * 10).roundToInt()

    return baseScore + timeBonus + accuracyBonus
}
