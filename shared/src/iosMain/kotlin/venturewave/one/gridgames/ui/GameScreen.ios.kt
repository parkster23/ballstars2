package venturewave.one.gridgames.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.TensorFlowLiteTaskVision.TFLDetection
import venturewave.one.gridgames.data.GridRepository
import venturewave.one.gridgames.detection.BallDetector
import venturewave.one.gridgames.model.CalibratedGrid
import venturewave.one.gridgames.model.GameResult
import venturewave.one.gridgames.model.TrainingPattern
import venturewave.one.gridgames.model.POINTS_PER_HIT
import venturewave.one.gridgames.model.WRONG_TARGET_PENALTY
import venturewave.one.gridgames.model.patternCompletionBonus
import venturewave.one.gridgames.platform.playCountdownBeep
import venturewave.one.gridgames.platform.playGoSound
import venturewave.one.gridgames.viewmodels.currentTimeMillis

/**
 * iOS GameScreen - camera + live ball detection wired to the same
 * touch-calibrated grid model GameScreen.android.kt uses
 * (GridRepository/CalibratedGrid/GridTarget, all commonMain - unaffected
 * by this session's CV-subsystem removal, since that was the old
 * automated Hough-line detector, not this touch-tap calibration model).
 *
 * NOT YET TESTED on a simulator or device - see IOS_HANDOFF.md. Verified
 * only to Kotlin-compile/link via `./gradlew
 * :shared:linkDebugFrameworkIosSimulatorArm64
 * :shared:linkDebugFrameworkIosArm64`.
 *
 * Deliberately simpler than GameScreen.android.kt's visual design (no
 * BallStars HUD animations/styling) - this pass prioritizes a correct,
 * genuinely wired core loop (camera -> detection -> hit test -> score)
 * over matching Android's polish. Porting the visual HUD is a reasonable
 * follow-up once the core loop is confirmed working on real hardware.
 *
 * IMPORTANT GAP: there's no grid CALIBRATION screen on iOS yet
 * (PlatformSpecificScanTargets3's iOS actual is still the "Coming Soon"
 * stub) - GridRepository.getGrid() will be null on iOS until that's also
 * ported, so the hit-detection path below is unverified even in principle
 * until then. IosCameraPreview (CameraCapture.ios.kt) is reusable for that
 * screen too - it doesn't depend on anything gameplay-specific.
 */
private enum class IosGamePhase {
    TIME_SELECTION,
    COUNTDOWN,
    ACTIVE,
    PAUSED,
    COMPLETE
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GameScreen(
    pattern: TrainingPattern,
    onGameComplete: (GameResult) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier
) {
    val calibratedGrid = GridRepository.getGrid()

    var gamePhase by remember { mutableStateOf(IosGamePhase.TIME_SELECTION) }
    var selectedTimeDuration by remember { mutableStateOf(30) }
    var countdownValue by remember { mutableStateOf(10) }
    var timeRemaining by remember { mutableStateOf(30) }

    var currentTargetIndex by remember { mutableStateOf(0) }
    var hitCount by remember { mutableStateOf(0) }
    var missCount by remember { mutableStateOf(0) }
    var lastHitTargetNumber by remember { mutableStateOf<Int?>(null) }
    var hitPositionsInIteration by remember { mutableStateOf(setOf<Int>()) }
    var gameActive by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(0L) }

    // Training scoring state - mirrors GameScreen.android.kt, backed by the
    // same shared TrainingScoring.kt functions.
    var sessionScore by remember { mutableStateOf(0) }
    var cleanLap by remember { mutableStateOf(true) }
    var hitsThisLap by remember { mutableStateOf(0) }
    var bonusPoints by remember { mutableStateOf(0) }
    var penaltyPoints by remember { mutableStateOf(0) }
    var missedTargetsInIteration by remember { mutableStateOf(setOf<Int>()) }  // 0-based indices already penalized this lap

    var screenSize by remember { mutableStateOf(IntSize(1080, 2424)) }
    var latestDetections by remember { mutableStateOf<List<TFLDetection>>(emptyList()) }

    // Same 1280x720 preset CameraCapture.ios.kt configures the capture
    // session with - TFLDetection.boundingBox comes back in that pixel
    // space, so this is the "tensor size" equivalent of
    // viewModel.tensorImageWidth/Height on the Android side.
    val captureWidth = 1280f
    val captureHeight = 720f

    val ballDetector = remember {
        BallDetector(
            threshold = 0.20f,
            maxResults = 1,
            detectorListener = object : BallDetector.DetectorListener {
                override fun onError(error: String) {
                    // Detector failed to initialize (e.g. model not bundled
                    // yet - see IOS_HANDOFF.md). Detections simply stay
                    // empty; nothing else to do here.
                }

                override fun onResults(detections: List<TFLDetection>) {
                    latestDetections = detections
                }
            }
        )
    }

    // Hit detection - same two-stage scaling GameScreen.android.kt uses:
    // calibration viewport -> current aperture size -> detector's pixel
    // space - just expressed with CGRect/TFLDetection instead of
    // Android's RectF/Detection.
    LaunchedEffect(latestDetections, calibratedGrid, gamePhase) {
        if (gamePhase != IosGamePhase.ACTIVE || calibratedGrid == null) return@LaunchedEffect

        val actualTargetIndex = currentTargetIndex % pattern.targetSequence.size
        val currentTargetNumber = pattern.targetSequence[actualTargetIndex]

        val sourceWidth = calibratedGrid.sourceViewportWidth
        val sourceHeight = calibratedGrid.sourceViewportHeight
        val calToGameScaleX = if (sourceWidth > 0f) screenSize.width / sourceWidth else 1f
        val calToGameScaleY = if (sourceHeight > 0f) screenSize.height / sourceHeight else 1f
        val gameToCaptureScaleX = captureWidth / screenSize.width.toFloat()
        val gameToCaptureScaleY = captureHeight / screenSize.height.toFloat()

        // Map each valid detection's capture-space center back into
        // calibration-viewport space (inverse of the forward transform) so
        // CalibratedGrid's own findTargetAt can tell us which of the 9
        // boxes (if any) it's in - current target or a wrong one. Mirrors
        // GameScreen.android.kt's equivalent refactor.
        val matchedTargets = latestDetections.mapNotNull { detection ->
            // NSArray<TFLCategory *> comes through cinterop as an erased
            // List<*>, same as TFLDetectionResult.detections in
            // BallDetector.ios.kt.
            @Suppress("UNCHECKED_CAST")
            val categories = detection.categories as List<platform.TensorFlowLiteTaskVision.TFLCategory>
            val score = categories.firstOrNull()?.score ?: 0f
            if (score < 0.20f) return@mapNotNull null

            // CGRectGetMidX/Y avoid useContents{}'s origin/size property
            // names clashing with this file's Compose Modifier.size/width
            // extension-function imports.
            val box = detection.boundingBox
            val captureX = platform.CoreGraphics.CGRectGetMidX(box).toFloat()
            val captureY = platform.CoreGraphics.CGRectGetMidY(box).toFloat()

            val gameX = captureX / gameToCaptureScaleX
            val gameY = captureY / gameToCaptureScaleY
            val calibrationX = gameX / calToGameScaleX
            val calibrationY = gameY / calToGameScaleY

            calibratedGrid.findTargetAt(calibrationX, calibrationY)
        }

        val hit = matchedTargets.any { it.index == currentTargetNumber - 1 }
        val wrongTargetNumber: Int? = if (!hit) {
            matchedTargets.firstOrNull { it.index != currentTargetNumber - 1 }?.let { it.index + 1 }
        } else null

        if (hit) {
            val alreadyHit = hitPositionsInIteration.contains(actualTargetIndex)
            if (lastHitTargetNumber != currentTargetNumber && !alreadyHit) {
                hitPositionsInIteration = hitPositionsInIteration + actualTargetIndex
                hitCount++
                sessionScore += POINTS_PER_HIT
                hitsThisLap++
                lastHitTargetNumber = currentTargetNumber
                currentTargetIndex++

                val newIteration = currentTargetIndex / pattern.targetSequence.size
                val prevIteration = (currentTargetIndex - 1) / pattern.targetSequence.size
                if (newIteration > prevIteration) {
                    hitPositionsInIteration = setOf()
                    missedTargetsInIteration = setOf()

                    // Completion bonus: only paid if no wrong-target hit
                    // occurred anywhere during the lap that just finished.
                    if (cleanLap) {
                        val bonus = patternCompletionBonus(hitsThisLap)
                        sessionScore += bonus
                        bonusPoints += bonus
                    }
                    cleanLap = true
                    hitsThisLap = 0
                }
            }
        } else if (wrongTargetNumber != null) {
            val wrongTargetIndex = wrongTargetNumber - 1
            if (!missedTargetsInIteration.contains(wrongTargetIndex)) {
                missedTargetsInIteration = missedTargetsInIteration + wrongTargetIndex
                missCount++
                cleanLap = false
                sessionScore -= WRONG_TARGET_PENALTY
                penaltyPoints += WRONG_TARGET_PENALTY
            }
            if (lastHitTargetNumber != null) {
                lastHitTargetNumber = null
            }
        } else {
            if (lastHitTargetNumber != null) {
                lastHitTargetNumber = null
            }
        }
    }

    LaunchedEffect(gamePhase) {
        if (gamePhase == IosGamePhase.COUNTDOWN) {
            countdownValue = 10
            playCountdownBeep()
            while (countdownValue > 0) {
                delay(1000)
                countdownValue--
                if (countdownValue > 0) playCountdownBeep() else playGoSound()
            }
            delay(1000)
            gamePhase = IosGamePhase.ACTIVE
            gameActive = true
            startTime = currentTimeMillis()
            timeRemaining = selectedTimeDuration
        }
    }

    LaunchedEffect(gamePhase, timeRemaining) {
        if (gamePhase == IosGamePhase.ACTIVE && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
            if (timeRemaining <= 0) {
                gamePhase = IosGamePhase.COMPLETE
                gameActive = false

                val totalTime = currentTimeMillis() - startTime
                val totalTargets = hitCount + missCount
                val accuracy = if (totalTargets > 0) hitCount.toFloat() / totalTargets * 100f else 0f
                val score = sessionScore
                val patternsCompleted = currentTargetIndex / pattern.targetSequence.size

                onGameComplete(
                    GameResult(
                        pattern = pattern,
                        score = score,
                        totalTime = totalTime,
                        hitCount = hitCount,
                        missCount = missCount,
                        accuracy = accuracy,
                        patternsCompleted = patternsCompleted,
                        bonusPoints = bonusPoints,
                        penaltyPoints = penaltyPoints
                    )
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onBackPressed) { Text("Back") }
                Text(pattern.name, style = MaterialTheme.typography.titleMedium)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .aspectRatio(3f / 4f)
                    .align(Alignment.CenterHorizontally)
                    .onSizeChanged { screenSize = it }
            ) {
                IosCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onFrame = { sampleBuffer ->
                        if (gamePhase == IosGamePhase.ACTIVE) {
                            ballDetector.detect(sampleBuffer)
                        }
                    }
                )

                // Only shown pre-game: the player can't see the screen once
                // play starts, so highlighting a target during ACTIVE/PAUSED
                // serves no purpose (mirrors GameScreen.android.kt).
                if (calibratedGrid != null &&
                    (gamePhase == IosGamePhase.TIME_SELECTION || gamePhase == IosGamePhase.COUNTDOWN)
                ) {
                    GridOverlay(
                        calibratedGrid = calibratedGrid,
                        currentTarget = pattern.targetSequence[currentTargetIndex % pattern.targetSequence.size],
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (gamePhase) {
                IosGamePhase.TIME_SELECTION -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Select duration")
                        Row {
                            Button(onClick = { selectedTimeDuration = 30 }) { Text("30s") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { selectedTimeDuration = 60 }) { Text("60s") }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { gamePhase = IosGamePhase.COUNTDOWN }) { Text("Start") }
                        if (calibratedGrid == null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No calibrated grid yet - grid calibration isn't ported to iOS " +
                                    "yet, see IOS_HANDOFF.md",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                IosGamePhase.COUNTDOWN -> {
                    Text(
                        if (countdownValue > 0) "$countdownValue" else "GO!",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                IosGamePhase.ACTIVE, IosGamePhase.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Time: $timeRemaining")
                        Text("Score: $sessionScore")
                        Text("Hits: $hitCount")
                        Text(
                            "Step ${(currentTargetIndex % pattern.targetSequence.size) + 1}" +
                                "/${pattern.targetSequence.size}"
                        )
                    }
                    Button(
                        onClick = {
                            gamePhase = if (gamePhase == IosGamePhase.ACTIVE) {
                                IosGamePhase.PAUSED
                            } else {
                                IosGamePhase.ACTIVE
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (gamePhase == IosGamePhase.ACTIVE) "Pause" else "Resume")
                    }
                }

                IosGamePhase.COMPLETE -> {
                    Text(
                        "Complete! Hits: $hitCount",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun GridOverlay(
    calibratedGrid: CalibratedGrid,
    currentTarget: Int,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val scaleX = if (calibratedGrid.sourceViewportWidth > 0f) {
            size.width / calibratedGrid.sourceViewportWidth
        } else 1f
        val scaleY = if (calibratedGrid.sourceViewportHeight > 0f) {
            size.height / calibratedGrid.sourceViewportHeight
        } else 1f

        for (i in 0 until 9) {
            val target = calibratedGrid[i]
            val center = Offset(
                (target.x + target.width / 2f) * scaleX,
                (target.y + target.height / 2f) * scaleY
            )
            val radius = minOf(target.width * scaleX, target.height * scaleY) * 0.24f

            drawCircle(
                color = if (i + 1 == currentTarget) Color.Green else Color.Cyan,
                radius = radius,
                center = center,
                style = Stroke(width = 4f)
            )
        }
    }
}

