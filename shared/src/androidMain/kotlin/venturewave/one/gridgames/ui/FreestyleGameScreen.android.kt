package venturewave.one.gridgames.ui

import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import venturewave.one.gridgames.data.GridRepository
import venturewave.one.gridgames.detection.BallDetectorHelper
import venturewave.one.gridgames.detection.BallDetectorViewModel
import venturewave.one.gridgames.model.CHAIN_TIMEOUT_MS
import venturewave.one.gridgames.model.FreestyleChainState
import venturewave.one.gridgames.model.FreestyleResult
import venturewave.one.gridgames.model.chainMultiplier
import venturewave.one.gridgames.platform.ComboMilestone
import venturewave.one.gridgames.platform.playComboSound
import venturewave.one.gridgames.platform.playCountdownBeep
import venturewave.one.gridgames.platform.playGoSound
import venturewave.one.gridgames.ui.theme.BallStarsColor
import kotlin.math.max
import kotlin.math.min
import java.text.NumberFormat

/**
 * Freestyle Gameplay Mode - Android implementation.
 *
 * Copied from GameScreen.android.kt's visual design (same BallStars HUD
 * look - camera aperture, header, overlays) per explicit direction to
 * reuse that screen rather than build a stripped-down one; the scoring/
 * pattern-recognition logic itself lives once in commonMain
 * (FreestyleEngine.kt / FreestyleChainState) instead of being duplicated
 * here and in the iOS actual, since per-platform scoring formulas drifting
 * apart is exactly the bug Training Mode's scoring had before being
 * consolidated into TrainingScoring.kt.
 *
 * Unlike Training Mode, there's no single pre-selected pattern: any
 * confirmed new box entry is fed into FreestyleChainState, which
 * recognizes whenever a known pattern's sequence has just been completed.
 */
private enum class FreestyleGamePhase {
    TIME_SELECTION, COUNTDOWN, ACTIVE, PAUSED, COMPLETE
}

private val GameplayNavy = Color(0xFF07131F)
private val GameplaySurface = Color(0xFF0E2236)
private val GameplayGreen = Color(0xFF1ED36A)
private val GameplayGold = Color(0xFFFFC21A)
private val GameplayOrange = Color(0xFFFF7A1A)
private val GameplayMagenta = Color(0xFFFF37E6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun FreestyleGameScreen(
    onSessionComplete: (FreestyleResult) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val calibratedGrid = GridRepository.getGrid()

    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var screenSize by remember { mutableStateOf(IntSize(1080, 2424)) }

    LaunchedEffect(camera, calibratedGrid) {
        camera?.let { cam ->
            calibratedGrid?.let { grid ->
                if (grid.zoomRatio > 0f) {
                    try {
                        cam.cameraControl.setLinearZoom(grid.zoomRatio)
                    } catch (e: Exception) {
                        android.util.Log.e("FreestyleGameScreen", "Failed to apply zoom: ${grid.zoomRatio}", e)
                    }
                }
            }
        }
    }

    var gamePhase by remember { mutableStateOf(FreestyleGamePhase.TIME_SELECTION) }
    var selectedTimeDuration by remember { mutableStateOf(30) }
    var countdownValue by remember { mutableStateOf(10) }
    var timeRemaining by remember { mutableStateOf(30) }
    var startTime by remember { mutableStateOf(0L) }

    // Pure, Compose-free chain/recognition state (see FreestyleEngine.kt).
    // Plain var, not itself observable - the Compose-reactive vars below are
    // manually synced from it after each call so the HUD recomposes.
    val chainState = remember { FreestyleChainState() }

    var sessionScore by remember { mutableStateOf(0) }       // banked total across completed chains
    var liveChainScore by remember { mutableStateOf(0) }      // mirrors chainState.currentChainScore
    var chainLength by remember { mutableStateOf(0) }         // mirrors chainState.chainLength
    var longestChain by remember { mutableStateOf(0) }
    var totalTricksLanded by remember { mutableStateOf(0) }
    var lastEnteredBoxNumber by remember { mutableStateOf<Int?>(null) } // debounces box-entry, mirrors Training Mode's lastHitTargetNumber
    var lastChainActivityTime by remember { mutableStateOf(0L) }

    var lastTrickName by remember { mutableStateOf<String?>(null) }
    var lastTrickNonce by remember { mutableStateOf(0) }
    var recapBanner by remember { mutableStateOf<String?>(null) }
    var recapNonce by remember { mutableStateOf(0) }
    var finalResult by remember { mutableStateOf<FreestyleResult?>(null) }

    val viewModel = remember {
        BallDetectorViewModel(
            context = context,
            lifecycleOwner = lifecycleOwner,
            threshold = 0.20f,
            maxResults = 1,
            numberOfThreads = 4,
            currentDelegate = BallDetectorHelper.DELEGATE_GPU
        )
    }

    val detectionResults by viewModel.detectionResults.collectAsState()

    fun bankCurrentChain() {
        val banked = chainState.bankChain() ?: return
        sessionScore += banked.chainScore
        longestChain = max(longestChain, banked.chainLength)
        liveChainScore = 0
        chainLength = 0
        recapBanner = "${banked.chainLength}-trick chain — ${banked.landedNames.joinToString(" → ")} — +${banked.chainScore}"
        recapNonce++
    }

    // Box-entry detection - same coordinate-transform technique Training
    // Mode's hit-detection uses (ball's tensor-space center mapped back
    // through game-aperture space into calibration space, then
    // CalibratedGrid.findTargetAt tells us which of the 9 boxes it's in)
    // but with no single "current expected target" - ANY matched box is
    // valid input to the pattern recognizer.
    LaunchedEffect(detectionResults, calibratedGrid) {
        if (gamePhase != FreestyleGamePhase.ACTIVE || calibratedGrid == null) return@LaunchedEffect

        val tensorWidth = viewModel.tensorImageWidth.toFloat()
        val tensorHeight = viewModel.tensorImageHeight.toFloat()

        val sourceWidth = calibratedGrid.sourceViewportWidth
        val sourceHeight = calibratedGrid.sourceViewportHeight
        val calibrationToGameScaleX = if (sourceWidth > 0f) screenSize.width.toFloat() / sourceWidth else 1f
        val calibrationToGameScaleY = if (sourceHeight > 0f) screenSize.height.toFloat() / sourceHeight else 1f
        val gameToTensorScaleX = if (screenSize.width > 0) tensorWidth / screenSize.width.toFloat() else 1f
        val gameToTensorScaleY = if (screenSize.height > 0) tensorHeight / screenSize.height.toFloat() else 1f

        val matchedBoxNumber: Int? = if (tensorWidth == 0f || tensorHeight == 0f) {
            null
        } else {
            detectionResults.firstNotNullOfOrNull { detection ->
                val score = detection.categories.firstOrNull()?.score ?: 0f
                if (score < 0.20f) return@firstNotNullOfOrNull null

                val ballBox = detection.boundingBox
                val ballGameX = ballBox.centerX() / gameToTensorScaleX
                val ballGameY = ballBox.centerY() / gameToTensorScaleY
                val ballCalibrationX = ballGameX / calibrationToGameScaleX
                val ballCalibrationY = ballGameY / calibrationToGameScaleY

                calibratedGrid.findTargetAt(ballCalibrationX, ballCalibrationY)?.let { it.index + 1 }
            }
        }

        // Only advance lastEnteredBoxNumber when a DIFFERENT box is
        // positively detected - deliberately do NOT reset it to null on a
        // frame with no confident detection at all. Live detection commonly
        // flickers to "no box" for a frame or two even while the ball is
        // genuinely still sitting in the same box (confidence dips, motion
        // blur); resetting on that would make the very next frame look like
        // a fresh re-entry into the box the ball never actually left,
        // inserting a spurious duplicate into the sequence buffer (e.g.
        // "5,7,4,5" corrupted into "5,7,4,4,5") that permanently breaks
        // exact-match recognition for that attempt. Safe to do since none
        // of the 13 known patterns ever repeat the same box twice in a row,
        // so a genuine same-box re-entry is never something recognition
        // actually needs to detect.
        if (matchedBoxNumber != null && lastEnteredBoxNumber != matchedBoxNumber) {
            lastEnteredBoxNumber = matchedBoxNumber
            lastChainActivityTime = System.currentTimeMillis()

            val landed = chainState.onBoxEntered(matchedBoxNumber)
            android.util.Log.i("FreestyleGameScreen", "BOX ENTERED: $matchedBoxNumber landed=${landed?.pattern?.name}")
            if (landed != null) {
                totalTricksLanded++
                lastTrickName = landed.pattern.name
                lastTrickNonce++
                liveChainScore = chainState.currentChainScore
                chainLength = chainState.chainLength
                android.util.Log.i("FreestyleGameScreen", "✨ TRICK LANDED: ${landed.pattern.name} points=${landed.points} chainPos=${landed.chainPosition} flowBonus=${landed.flowBonusPoints} milestone=${landed.milestoneReached}")

                when (landed.milestoneReached) {
                    3 -> playComboSound(ComboMilestone.CHAIN_3)
                    5 -> playComboSound(ComboMilestone.CHAIN_5)
                    8 -> playComboSound(ComboMilestone.CHAIN_8_PLUS)
                }
                if (landed.flowBonusPoints > 0) {
                    playComboSound(ComboMilestone.FLOW_BONUS)
                }
            }
        }
    }

    // Chain-timeout ticker: a chain that's gone quiet for CHAIN_TIMEOUT_MS
    // gets banked automatically so its points aren't lost in limbo.
    LaunchedEffect(gamePhase) {
        while (gamePhase == FreestyleGamePhase.ACTIVE) {
            delay(250)
            if (chainState.chainLength > 0 && System.currentTimeMillis() - lastChainActivityTime > CHAIN_TIMEOUT_MS) {
                bankCurrentChain()
            }
        }
    }

    // Trick-landed banner auto-dismiss
    LaunchedEffect(lastTrickNonce) {
        if (lastTrickNonce > 0) {
            delay(1200)
            lastTrickName = null
        }
    }

    // Chain recap banner auto-dismiss
    LaunchedEffect(recapNonce) {
        if (recapNonce > 0) {
            delay(2200)
            recapBanner = null
        }
    }

    // Countdown timer (10→1→GO)
    LaunchedEffect(gamePhase) {
        if (gamePhase == FreestyleGamePhase.COUNTDOWN) {
            countdownValue = 10
            playCountdownBeep()
            while (countdownValue > 0) {
                delay(1000)
                countdownValue--
                if (countdownValue > 0) playCountdownBeep() else playGoSound()
            }
            delay(1000)
            gamePhase = FreestyleGamePhase.ACTIVE
            startTime = System.currentTimeMillis()
            lastChainActivityTime = startTime
            timeRemaining = selectedTimeDuration
        }
    }

    // Game timer countdown (active phase)
    LaunchedEffect(gamePhase, timeRemaining) {
        if (gamePhase == FreestyleGamePhase.ACTIVE && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
            if (timeRemaining <= 0) {
                // A session timer expiring mid-chain must not silently drop
                // that chain's points - bank it before computing the total.
                bankCurrentChain()

                gamePhase = FreestyleGamePhase.COMPLETE
                val totalTime = System.currentTimeMillis() - startTime
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(GameplayNavy)
    ) {
        FreestyleGameplayBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FreestyleHeader(
                chainLength = chainLength,
                gamePhase = gamePhase,
                onBack = onBackPressed,
                onPause = { if (gamePhase == FreestyleGamePhase.ACTIVE) gamePhase = FreestyleGamePhase.PAUSED },
                onResume = { if (gamePhase == FreestyleGamePhase.PAUSED) gamePhase = FreestyleGamePhase.ACTIVE }
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val apertureShape = RoundedCornerShape(24.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(3f / 4f)
                        .background(Color.Black)
                        .shadow(
                            elevation = 12.dp,
                            shape = apertureShape,
                            ambientColor = BallStarsColor.GlowCyan,
                            spotColor = BallStarsColor.GlowCyan
                        )
                        .onSizeChanged { size -> screenSize = size }
                ) {
                    FreestyleCameraPreview(
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        viewModel = viewModel,
                        onCameraReady = { cam -> camera = cam },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Only shown pre-game: no "next expected" box exists in
                    // freestyle, and (same lesson as Training Mode) the
                    // overlay's perpetual pulse animation keeps redrawing at
                    // ~60fps for as long as it's on screen, which visibly
                    // lags the camera preview during actual play.
                    if (calibratedGrid != null &&
                        (gamePhase == FreestyleGamePhase.TIME_SELECTION || gamePhase == FreestyleGamePhase.COUNTDOWN)
                    ) {
                        FreestyleGridOverlay(
                            calibratedGrid = calibratedGrid,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (gamePhase == FreestyleGamePhase.COUNTDOWN) {
                        FreestyleCountdownOverlay(countdownValue = countdownValue)
                    }

                    if (gamePhase == FreestyleGamePhase.PAUSED) {
                        FreestylePauseOverlay(onResume = { gamePhase = FreestyleGamePhase.ACTIVE })
                    }

                    lastTrickName?.let { name ->
                        TrickLandedBanner(name)
                    }

                    if (gamePhase == FreestyleGamePhase.COMPLETE) {
                        finalResult?.let { result ->
                            FreestyleSummaryOverlay(
                                result = result,
                                onDone = { onSessionComplete(result) }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(3f / 4f)
                        .border(width = 4.dp, color = BallStarsColor.GlowCyan, shape = RoundedCornerShape(24.dp))
                )
            }

            Spacer(Modifier.height(10.dp))

            recapBanner?.let { text ->
                ChainRecapBanner(text, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
            }

            when (gamePhase) {
                FreestyleGamePhase.TIME_SELECTION -> {
                    FreestyleTimeSelectionHud(
                        selectedSeconds = selectedTimeDuration,
                        onSelectSeconds = { selectedTimeDuration = it },
                        onStart = { gamePhase = FreestyleGamePhase.COUNTDOWN },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }

                FreestyleGamePhase.COUNTDOWN,
                FreestyleGamePhase.ACTIVE,
                FreestyleGamePhase.PAUSED,
                FreestyleGamePhase.COMPLETE -> {
                    val displaySeconds = if (gamePhase == FreestyleGamePhase.COUNTDOWN) selectedTimeDuration else timeRemaining
                    val nextMultiplier = chainMultiplier(chainLength + 1, isRepeatOfPrevious = false)

                    FreestyleHud(
                        timeRemaining = displaySeconds,
                        score = sessionScore + liveChainScore,
                        chainLength = chainLength,
                        nextMultiplier = nextMultiplier,
                        longestChain = longestChain,
                        lastTrickName = lastTrickName,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FreestyleGameplayBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF17334D), GameplayNavy, Color(0xFF081827))
                    )
                )
        )

        Image(
            painter = painterResource(Res.drawable.target_left_edge),
            contentDescription = null,
            modifier = Modifier.fillMaxHeight().width(58.dp).align(Alignment.CenterStart),
            contentScale = ContentScale.FillHeight
        )

        Image(
            painter = painterResource(Res.drawable.target_right_edge),
            contentDescription = null,
            modifier = Modifier.fillMaxHeight().width(58.dp).align(Alignment.CenterEnd),
            contentScale = ContentScale.FillHeight
        )

        // Drawn last (on top) so it spans the full width across both side
        // edge images, rather than having them cut across its corners.
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
    }
}

@Composable
private fun FreestyleHeader(
    chainLength: Int,
    gamePhase: FreestyleGamePhase,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(0.92f).heightIn(min = 62.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
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
                    painter = painterResource(Res.drawable.match),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FREESTYLE",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = if (chainLength > 0) "CHAIN x$chainLength" else "LINK PATTERNS TOGETHER",
                        color = BallStarsColor.GlowCyan,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        if (gamePhase in listOf(FreestyleGamePhase.ACTIVE, FreestyleGamePhase.PAUSED)) {
            Image(
                painter = painterResource(
                    if (gamePhase == FreestyleGamePhase.PAUSED) Res.drawable.play_button else Res.drawable.pause_button
                ),
                contentDescription = if (gamePhase == FreestyleGamePhase.PAUSED) "Resume" else "Pause",
                modifier = Modifier.size(54.dp).clickable {
                    if (gamePhase == FreestyleGamePhase.PAUSED) onResume() else onPause()
                }
            )
        } else {
            Spacer(Modifier.size(54.dp))
        }
    }
}

@Composable
private fun FreestyleTimeSelectionHud(
    selectedSeconds: Int,
    onSelectSeconds: (Int) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        color = GameplayNavy.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
        border = BorderStroke(1.5.dp, BallStarsColor.GlowCyan.copy(alpha = 0.78f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FreestyleRoundTimeChoice(
                    text = "30 SEC",
                    selected = selectedSeconds == 30,
                    onClick = { onSelectSeconds(30) },
                    modifier = Modifier.weight(1f)
                )
                FreestyleRoundTimeChoice(
                    text = "60 SEC",
                    selected = selectedSeconds == 60,
                    onClick = { onSelectSeconds(60) },
                    modifier = Modifier.weight(1f)
                )
            }

            Image(
                painter = painterResource(Res.drawable.play_button),
                contentDescription = "Start game",
                modifier = Modifier.size(76.dp).clickable(onClick = onStart)
            )
        }
    }
}

@Composable
private fun FreestyleRoundTimeChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 58.dp).clickable(onClick = onClick),
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
private fun FreestyleCountdownOverlay(countdownValue: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "countdownPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(animation = tween(650, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "countdownScale"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.14f)),
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
                    shadow = androidx.compose.ui.graphics.Shadow(color = BallStarsColor.GlowCyan.copy(alpha = 0.8f), blurRadius = 16f)
                )
            )
        }
    }
}

@Composable
private fun FreestylePauseOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(GameplayNavy.copy(alpha = 0.62f)),
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
                Text(text = "PAUSED", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Image(
                    painter = painterResource(Res.drawable.play_button),
                    contentDescription = "Resume",
                    modifier = Modifier.size(82.dp).clickable(onClick = onResume)
                )
            }
        }
    }
}

@Composable
private fun TrickLandedBanner(trickName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Image(painter = painterResource(Res.drawable.combo_lightning_icon), contentDescription = null, modifier = Modifier.size(42.dp))
                Text(
                    text = trickName.uppercase(),
                    color = GameplayGold,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ChainRecapBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = GameplayNavy.copy(alpha = 0.92f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, GameplayGreen.copy(alpha = 0.8f))
    ) {
        Text(
            text = text,
            color = GameplayGreen,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun FreestyleSummaryOverlay(result: FreestyleResult, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameplayNavy.copy(alpha = 0.85f))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight(0.92f),
            color = GameplayNavy.copy(alpha = 0.96f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, BallStarsColor.GlowCyan.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 30.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "SESSION COMPLETE!", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = NumberFormat.getIntegerInstance().format(result.sessionScore),
                    color = GameplayGold,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Longest chain: ${result.longestChain}  •  Tricks landed: ${result.totalTricksLanded}",
                    color = BallStarsColor.GlowCyan,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (result.patternBreakdown.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = GameplaySurface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BallStarsColor.GlowCyan.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            result.patternBreakdown.forEach { (name, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = name,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "x$count",
                                        color = GameplayGold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Image(
                    painter = painterResource(Res.drawable.play_button),
                    contentDescription = "Done",
                    modifier = Modifier.size(76.dp).clickable(onClick = onDone)
                )
            }
        }
    }
}

@Composable
private fun FreestyleHud(
    timeRemaining: Int,
    score: Int,
    chainLength: Int,
    nextMultiplier: Float,
    longestChain: Int,
    lastTrickName: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 2.dp),
        color = GameplayNavy.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
        border = BorderStroke(1.5.dp, BallStarsColor.GlowCyan.copy(alpha = 0.72f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FreestyleHudMetric(
                    icon = Res.drawable.timer_icon,
                    label = "TIME LEFT",
                    value = "%02d:%02d".format(timeRemaining / 60, timeRemaining % 60),
                    valueColor = if (timeRemaining <= 10) GameplayOrange else BallStarsColor.GlowCyan,
                    modifier = Modifier.weight(1f)
                )
                FreestyleHudDivider()
                FreestyleHudMetric(
                    icon = Res.drawable.score_trophy_icon,
                    label = "SCORE",
                    value = NumberFormat.getIntegerInstance().format(score),
                    valueColor = GameplayGold,
                    modifier = Modifier.weight(1f)
                )
                FreestyleHudDivider()
                FreestyleHudMetric(
                    icon = Res.drawable.combo_lightning_icon,
                    label = "CHAIN",
                    value = chainLength.toString(),
                    valueColor = when {
                        chainLength >= 8 -> GameplayMagenta
                        chainLength >= 3 -> GameplayGold
                        else -> Color.White
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FreestyleSecondaryHudMetric(
                    icon = Res.drawable.lasthit_target_icon,
                    label = "LAST TRICK",
                    value = lastTrickName ?: "–",
                    valueColor = GameplayGreen,
                    modifier = Modifier.weight(1f)
                )
                FreestyleSecondaryHudMetric(
                    icon = Res.drawable.star_icon,
                    label = "LONGEST",
                    value = longestChain.toString(),
                    valueColor = GameplayGold,
                    modifier = Modifier.weight(1f)
                )
                FreestyleSecondaryHudMetric(
                    icon = Res.drawable.streak_flame_icon,
                    label = "NEXT",
                    value = "${if (nextMultiplier == nextMultiplier.toInt().toFloat()) nextMultiplier.toInt().toString() else nextMultiplier.toString()}x",
                    valueColor = when {
                        nextMultiplier >= 3f -> GameplayMagenta
                        nextMultiplier >= 1.5f -> BallStarsColor.GlowCyan
                        else -> Color.White
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FreestyleHudMetric(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(26.dp))
            Text(text = label, color = Color(0xFFAFC6D9), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        }
        Text(
            text = value,
            color = valueColor,
            fontSize = 31.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(color = valueColor.copy(alpha = 0.35f), blurRadius = 8f)
            )
        )
    }
}

@Composable
private fun FreestyleHudDivider() {
    Box(modifier = Modifier.width(1.dp).height(76.dp).background(BallStarsColor.GlowCyan.copy(alpha = 0.24f)))
}

@Composable
private fun FreestyleSecondaryHudMetric(
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
        Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(7.dp))
            Column {
                Text(text = label, color = Color(0xFFAFC6D9), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = value,
                    color = valueColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** CameraX preview - copied from GameScreen.android.kt's CameraPreview (fully generic, no pattern-specific logic). */
@Composable
private fun FreestyleCameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    viewModel: BallDetectorViewModel,
    onCameraReady: (androidx.camera.core.Camera) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val previewView = remember(context) {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val imageAnalysis = remember(viewModel) { viewModel.getImageAnalysisUseCase() }

    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner, previewView, imageAnalysis) {
        var provider: ProcessCameraProvider? = null
        var previewUseCase: Preview? = null
        var disposed = false

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                provider = cameraProvider

                val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build()
                previewUseCase = preview
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                onCameraReady(camera)
            } catch (e: Exception) {
                android.util.Log.e("FreestyleCameraPreview", "GAME CAMERA BIND FAILED", e)
            }
        }, executor)

        onDispose {
            disposed = true
            try {
                if (provider != null && previewUseCase != null) {
                    provider!!.unbind(previewUseCase!!, imageAnalysis)
                }
            } catch (e: Exception) {
                android.util.Log.e("FreestyleCameraPreview", "GAME CAMERA UNBIND FAILED", e)
            }
        }
    }
}

/** Static grid wireframe (no pulsing current-target highlight - freestyle has no single "next expected" box). */
@Composable
private fun FreestyleGridOverlay(
    calibratedGrid: venturewave.one.gridgames.model.CalibratedGrid,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (calibratedGrid.targets.size < 9) return@Canvas

        val sourceWidth = calibratedGrid.sourceViewportWidth
        val sourceHeight = calibratedGrid.sourceViewportHeight
        val scaleX = if (sourceWidth > 0f) size.width / sourceWidth else 1f
        val scaleY = if (sourceHeight > 0f) size.height / sourceHeight else 1f

        val centers = (0 until 9).map { i ->
            val target = calibratedGrid[i]
            val x = target.x * scaleX
            val y = target.y * scaleY
            val width = target.width * scaleX
            val height = target.height * scaleY
            Offset(x + width / 2f, y + height / 2f)
        }

        val connections = listOf(
            0 to 1, 1 to 2, 3 to 4, 4 to 5, 6 to 7, 7 to 8,
            0 to 3, 3 to 6, 1 to 4, 4 to 7, 2 to 5, 5 to 8,
            0 to 4, 4 to 8, 2 to 4, 4 to 6
        )

        connections.forEach { (from, to) ->
            drawLine(
                color = BallStarsColor.GlowCyan.copy(alpha = 0.10f),
                start = centers[from], end = centers[to],
                strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round, blendMode = BlendMode.Screen
            )
        }
        connections.forEach { (from, to) ->
            drawLine(
                color = BallStarsColor.GlowCyan.copy(alpha = 0.56f),
                start = centers[from], end = centers[to],
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
            )
        }

        for (i in 0 until 9) {
            val target = calibratedGrid[i]
            val center = centers[i]
            val transformedWidth = target.width * scaleX
            val transformedHeight = target.height * scaleY
            val baseRadius = (min(transformedWidth, transformedHeight) * 0.24f)
                .coerceAtLeast(12.dp.toPx())
                .coerceAtMost(28.dp.toPx())

            drawCircle(color = BallStarsColor.GlowCyan.copy(alpha = 0.10f), radius = baseRadius * 1.45f, center = center, blendMode = BlendMode.Screen)
            drawCircle(color = BallStarsColor.GlowCyan.copy(alpha = 0.62f), radius = baseRadius, center = center, style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.White.copy(alpha = 0.88f), radius = baseRadius * 0.18f, center = center)
        }
    }
}
