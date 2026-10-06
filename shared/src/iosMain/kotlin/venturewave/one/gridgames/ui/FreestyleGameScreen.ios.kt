package venturewave.one.gridgames.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import venturewave.one.gridgames.model.CHAIN_TIMEOUT_MS
import venturewave.one.gridgames.model.CalibratedGrid
import venturewave.one.gridgames.model.FreestyleChainState
import venturewave.one.gridgames.model.FreestyleResult
import venturewave.one.gridgames.model.chainMultiplier
import venturewave.one.gridgames.platform.ComboMilestone
import venturewave.one.gridgames.platform.playComboSound
import venturewave.one.gridgames.platform.playCountdownBeep
import venturewave.one.gridgames.platform.playGoSound
import venturewave.one.gridgames.viewmodels.currentTimeMillis

/**
 * Freestyle Gameplay Mode - iOS implementation. Mirrors GameScreen.ios.kt's
 * structure (same simplified-vs-Android visual approach, NOT YET TESTED on
 * a simulator/device - see IOS_HANDOFF.md). No single pre-selected
 * pattern: any confirmed new box entry is fed into FreestyleChainState
 * (commonMain), which recognizes whenever a known pattern's sequence has
 * just been completed - the same shared logic the Android actual uses, so
 * the two platforms can't drift into different scoring formulas.
 */
private enum class IosFreestylePhase {
    TIME_SELECTION, COUNTDOWN, ACTIVE, PAUSED, COMPLETE
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun FreestyleGameScreen(
    onSessionComplete: (FreestyleResult) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier
) {
    val calibratedGrid = GridRepository.getGrid()

    var gamePhase by remember { mutableStateOf(IosFreestylePhase.TIME_SELECTION) }
    var selectedTimeDuration by remember { mutableStateOf(30) }
    var countdownValue by remember { mutableStateOf(10) }
    var timeRemaining by remember { mutableStateOf(30) }
    var startTime by remember { mutableStateOf(0L) }

    val chainState = remember { FreestyleChainState() }

    var sessionScore by remember { mutableStateOf(0) }
    var liveChainScore by remember { mutableStateOf(0) }
    var chainLength by remember { mutableStateOf(0) }
    var longestChain by remember { mutableStateOf(0) }
    var totalTricksLanded by remember { mutableStateOf(0) }
    var lastEnteredBoxNumber by remember { mutableStateOf<Int?>(null) }
    var lastChainActivityTime by remember { mutableStateOf(0L) }
    var lastTrickName by remember { mutableStateOf<String?>(null) }
    var recapBanner by remember { mutableStateOf<String?>(null) }
    var finalResult by remember { mutableStateOf<FreestyleResult?>(null) }

    var screenSize by remember { mutableStateOf(IntSize(1080, 2424)) }
    var latestDetections by remember { mutableStateOf<List<TFLDetection>>(emptyList()) }

    val captureWidth = 1280f
    val captureHeight = 720f

    val ballDetector = remember {
        BallDetector(
            threshold = 0.20f,
            maxResults = 1,
            detectorListener = object : BallDetector.DetectorListener {
                override fun onError(error: String) {}
                override fun onResults(detections: List<TFLDetection>) {
                    latestDetections = detections
                }
            }
        )
    }

    fun bankCurrentChain() {
        val banked = chainState.bankChain() ?: return
        sessionScore += banked.chainScore
        if (banked.chainLength > longestChain) longestChain = banked.chainLength
        liveChainScore = 0
        chainLength = 0
        recapBanner = "${banked.chainLength}-trick chain - ${banked.landedNames.joinToString(" -> ")} - +${banked.chainScore}"
    }

    // Box-entry detection - same transform GameScreen.ios.kt uses, but no
    // single "current expected target": any matched box is valid input.
    LaunchedEffect(latestDetections, calibratedGrid, gamePhase) {
        if (gamePhase != IosFreestylePhase.ACTIVE || calibratedGrid == null) return@LaunchedEffect

        val sourceWidth = calibratedGrid.sourceViewportWidth
        val sourceHeight = calibratedGrid.sourceViewportHeight
        val calToGameScaleX = if (sourceWidth > 0f) screenSize.width / sourceWidth else 1f
        val calToGameScaleY = if (sourceHeight > 0f) screenSize.height / sourceHeight else 1f
        val gameToCaptureScaleX = captureWidth / screenSize.width.toFloat()
        val gameToCaptureScaleY = captureHeight / screenSize.height.toFloat()

        val matchedBoxNumber: Int? = latestDetections.firstNotNullOfOrNull { detection ->
            @Suppress("UNCHECKED_CAST")
            val categories = detection.categories as List<platform.TensorFlowLiteTaskVision.TFLCategory>
            val score = categories.firstOrNull()?.score ?: 0f
            if (score < 0.20f) return@firstNotNullOfOrNull null

            val box = detection.boundingBox
            val captureX = platform.CoreGraphics.CGRectGetMidX(box).toFloat()
            val captureY = platform.CoreGraphics.CGRectGetMidY(box).toFloat()

            val gameX = captureX / gameToCaptureScaleX
            val gameY = captureY / gameToCaptureScaleY
            val calibrationX = gameX / calToGameScaleX
            val calibrationY = gameY / calToGameScaleY

            calibratedGrid.findTargetAt(calibrationX, calibrationY)?.let { it.index + 1 }
        }

        // Only advance lastEnteredBoxNumber when a DIFFERENT box is
        // positively detected - do NOT reset it to null on a frame with no
        // confident detection (see FreestyleGameScreen.android.kt for the
        // full explanation: this avoids a flicker-induced spurious
        // duplicate corrupting the exact-match sequence buffer). Safe since
        // none of the 13 known patterns repeat the same box twice in a row.
        if (matchedBoxNumber != null && lastEnteredBoxNumber != matchedBoxNumber) {
            lastEnteredBoxNumber = matchedBoxNumber
            lastChainActivityTime = currentTimeMillis()

            val landed = chainState.onBoxEntered(matchedBoxNumber)
            if (landed != null) {
                totalTricksLanded++
                lastTrickName = landed.pattern.name
                liveChainScore = chainState.currentChainScore
                chainLength = chainState.chainLength

                when (landed.milestoneReached) {
                    3 -> playComboSound(ComboMilestone.CHAIN_3)
                    5 -> playComboSound(ComboMilestone.CHAIN_5)
                    8 -> playComboSound(ComboMilestone.CHAIN_8_PLUS)
                }
                if (landed.flowBonusPoints > 0) playComboSound(ComboMilestone.FLOW_BONUS)
            }
        }
    }

    LaunchedEffect(gamePhase) {
        while (gamePhase == IosFreestylePhase.ACTIVE) {
            delay(250)
            if (chainState.chainLength > 0 && currentTimeMillis() - lastChainActivityTime > CHAIN_TIMEOUT_MS) {
                bankCurrentChain()
            }
        }
    }

    LaunchedEffect(gamePhase) {
        if (gamePhase == IosFreestylePhase.COUNTDOWN) {
            countdownValue = 10
            playCountdownBeep()
            while (countdownValue > 0) {
                delay(1000)
                countdownValue--
                if (countdownValue > 0) playCountdownBeep() else playGoSound()
            }
            delay(1000)
            gamePhase = IosFreestylePhase.ACTIVE
            startTime = currentTimeMillis()
            lastChainActivityTime = startTime
            timeRemaining = selectedTimeDuration
        }
    }

    LaunchedEffect(gamePhase, timeRemaining) {
        if (gamePhase == IosFreestylePhase.ACTIVE && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
            if (timeRemaining <= 0) {
                bankCurrentChain()
                gamePhase = IosFreestylePhase.COMPLETE
                val totalTime = currentTimeMillis() - startTime
                finalResult = FreestyleResult(
                    sessionScore = sessionScore,
                    longestChain = longestChain,
                    totalTricksLanded = totalTricksLanded,
                    totalTime = totalTime,
                    patternBreakdown = chainState.patternCompletionCounts.toList().sortedByDescending { it.second }
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
                Text("Freestyle", style = MaterialTheme.typography.titleMedium)
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
                        if (gamePhase == IosFreestylePhase.ACTIVE) {
                            ballDetector.detect(sampleBuffer)
                        }
                    }
                )

                // Only shown pre-game: freestyle has no single "next
                // expected" box, so there's nothing meaningful to highlight
                // once play starts (mirrors GameScreen.ios.kt).
                if (calibratedGrid != null &&
                    (gamePhase == IosFreestylePhase.TIME_SELECTION || gamePhase == IosFreestylePhase.COUNTDOWN)
                ) {
                    FreestyleGridOverlayIos(
                        calibratedGrid = calibratedGrid,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (gamePhase) {
                IosFreestylePhase.TIME_SELECTION -> {
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
                        Button(onClick = { gamePhase = IosFreestylePhase.COUNTDOWN }) { Text("Start") }
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

                IosFreestylePhase.COUNTDOWN -> {
                    Text(
                        if (countdownValue > 0) "$countdownValue" else "GO!",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                IosFreestylePhase.ACTIVE, IosFreestylePhase.PAUSED -> {
                    val nextMultiplier = chainMultiplier(chainLength + 1, isRepeatOfPrevious = false)
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Time: $timeRemaining")
                            Text("Score: ${sessionScore + liveChainScore}")
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Chain: $chainLength")
                            Text("Next: ${nextMultiplier}x")
                            Text("Longest: $longestChain")
                        }
                        lastTrickName?.let { Text("Last trick: $it") }
                        recapBanner?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    Button(
                        onClick = {
                            gamePhase = if (gamePhase == IosFreestylePhase.ACTIVE) IosFreestylePhase.PAUSED else IosFreestylePhase.ACTIVE
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (gamePhase == IosFreestylePhase.ACTIVE) "Pause" else "Resume")
                    }
                }

                IosFreestylePhase.COMPLETE -> {
                    val result = finalResult
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Session complete!", style = MaterialTheme.typography.titleLarge)
                        if (result != null) {
                            Text("Score: ${result.sessionScore}")
                            Text("Longest chain: ${result.longestChain}")
                            Text("Tricks landed: ${result.totalTricksLanded}")
                            result.patternBreakdown.forEach { (name, count) ->
                                Text("$name: $count")
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { onSessionComplete(result) }) { Text("Done") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreestyleGridOverlayIos(
    calibratedGrid: CalibratedGrid,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val scaleX = if (calibratedGrid.sourceViewportWidth > 0f) size.width / calibratedGrid.sourceViewportWidth else 1f
        val scaleY = if (calibratedGrid.sourceViewportHeight > 0f) size.height / calibratedGrid.sourceViewportHeight else 1f

        for (i in 0 until 9) {
            val target = calibratedGrid[i]
            val center = Offset(
                (target.x + target.width / 2f) * scaleX,
                (target.y + target.height / 2f) * scaleY
            )
            val radius = minOf(target.width * scaleX, target.height * scaleY) * 0.24f

            drawCircle(color = Color.Cyan, radius = radius, center = center, style = Stroke(width = 4f))
        }
    }
}
