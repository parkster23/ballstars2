package com.ballstars.mobile

import android.content.Context
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import grid_games_mobile.shared.generated.resources.Res
import grid_games_mobile.shared.generated.resources.target_header_background
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import venturewave.one.gridgames.data.GridRepository
import venturewave.one.gridgames.model.CalibratedGrid
import venturewave.one.gridgames.model.GridTarget
import venturewave.one.gridgames.ui.theme.BallStarsColor

/*
 * IMPORTANT
 *
 * Keep the package/import names for your existing:
 *
 * GridRepository
 * GridTarget
 * CalibratedGrid
 * BallStarsColor
 * Res
 *
 * if they live in different packages.
 *
 * This file deliberately preserves the manual 9-point calibration model.
 */

private const val TAG = "ScanTargets2Screen"

private val GridPointLabels = listOf(
    "Top left",
    "Top middle",
    "Top right",
    "Middle left",
    "Center",
    "Middle right",
    "Bottom left",
    "Bottom middle",
    "Bottom right"
)


// ============================================================================
// MAIN SCREEN
// ============================================================================

@Composable
fun ScanTargets2Screen(
    onBack: () -> Unit,
    onGridCalibrated: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember {
        mutableStateOf<Camera?>(null)
    }

    var zoomRatio by remember {
        mutableFloatStateOf(0.5f)  // Default to 50% zoom (0.0 causes camera crash)
    }

    var calibrationViewportSize by remember {
        mutableStateOf(IntSize.Zero)
    }

    val touchPositions = remember {
        mutableStateListOf<Offset>()
    }

    var isConfirmed by remember {
        mutableStateOf(false)
    }

    var showWipeAnimation by remember {
        mutableStateOf(false)
    }

    var wipeProgress by remember {
        mutableFloatStateOf(0f)
    }

    var showLockInButton by remember {
        mutableStateOf(false)
    }

    // Trigger wipe animation when 9 points are selected
    LaunchedEffect(touchPositions.size) {
        if (touchPositions.size == 9 && !isConfirmed) {
            showWipeAnimation = true

            // Animate wipe from 0 to 1 over 3 seconds
            val animationDuration = 3000L
            val startTime = System.currentTimeMillis()

            while (wipeProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                wipeProgress = (elapsed.toFloat() / animationDuration).coerceIn(0f, 1f)
                delay(16) // ~60 FPS
            }

            // Show lock-in button after animation completes
            showLockInButton = true
        }
    }

    /*
     * Main screen.
     *
     * The decorative artwork is completely independent from the
     * CameraX viewport.
     */
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BallStarsColor.BgDeep)
    ) {

        // ------------------------------------------------------------
        // BACKGROUND ARTWORK
        // ------------------------------------------------------------

        BallStarsFrameArtwork()


        // ------------------------------------------------------------
        // ACTUAL SCREEN LAYOUT
        // ------------------------------------------------------------

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {

            // ========================================================
            // HEADER
            // ========================================================

            TargetDetectionHeader(
                onBack = onBack
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )


            // ========================================================
            // CALIBRATION PROGRESS
            // ========================================================

            CalibrationProgress(
                completedSteps = touchPositions.size,
                totalSteps = 9,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // ========================================================
            // CAMERA REGION
            //
            // This takes all remaining space between HUD and controls.
            // ========================================================

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {

                CameraAperture(
                    // Same width fraction + aspect ratio as GameScreen's aperture so
                    // the two screens show an IDENTICAL crop of the 4:3 camera feed.
                    // Calibrated GridTarget coordinates are only valid if the visible
                    // framing matches exactly between Set Up The Zone and GameScreen.
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(3f / 4f),
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    touchPositions = touchPositions,
                    isConfirmed = isConfirmed,
                    showWipeAnimation = showWipeAnimation,
                    wipeProgress = wipeProgress,
                    showLockInButton = showLockInButton,

                    onCameraReady = { cam ->

                        camera = cam

                        cam.cameraControl.setLinearZoom(
                            zoomRatio
                        )
                    },

                    onTouch = { offset ->

                        if (
                            !isConfirmed &&
                            touchPositions.size < 9
                        ) {

                            touchPositions.add(offset)

                            val index =
                                touchPositions.lastIndex

                            Log.i(
                                TAG,
                                "Grid point ${index + 1}: " +
                                    "${GridPointLabels[index]} " +
                                    "at $offset"
                            )
                        }
                    },

                    onLockIn = {
                        if (touchPositions.size != 9) {
                            return@CameraAperture
                        }

                        isConfirmed = true

                        val gridTargets =
                            createGridTargetsFromTouchPositions(
                                touchPositions
                            )

                        val calibratedGrid =
                            CalibratedGrid(
                                targets = gridTargets,
                                zoomRatio = zoomRatio,
                                sourceViewportWidth = calibrationViewportSize.width.toFloat(),
                                sourceViewportHeight = calibrationViewportSize.height.toFloat()
                            )

                        GridRepository.saveGrid(
                            calibratedGrid
                        )

                        Log.i(
                            TAG,
                            "Grid saved. " +
                                "Targets=${gridTargets.size}, " +
                                "zoom=$zoomRatio"
                        )

                        onGridCalibrated()
                    },

                    onReset = {
                        touchPositions.clear()
                        isConfirmed = false
                        showWipeAnimation = false
                        wipeProgress = 0f
                        showLockInButton = false
                    },

                    onViewportSizeChanged = { size ->
                        calibrationViewportSize = size
                    },

                    // Zoom control is rendered INSIDE the aperture, anchored to
                    // its bottom edge, so it's horizontal and scoped to the
                    // camera aperture box's own width rather than floating
                    // vertically down the side of the whole screen.
                    zoomRatio = zoomRatio,

                    onZoomIn = {

                        zoomRatio =
                            (zoomRatio + 0.1f)
                                .coerceIn(0f, 1f)

                        camera
                            ?.cameraControl
                            ?.setLinearZoom(zoomRatio)
                    },

                    onZoomOut = {

                        zoomRatio =
                            (zoomRatio - 0.1f)
                                .coerceIn(0f, 1f)

                        camera
                            ?.cameraControl
                            ?.setLinearZoom(zoomRatio)
                    }
                )
            }


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            // ========================================================
            // INSTRUCTIONS
            // ========================================================

            InstructionOverlay(
                touchCount = touchPositions.size,
                isConfirmed = isConfirmed
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )


            // ========================================================
            // RESET / CONFIRM
            // ========================================================

            CalibrationControls(
                touchCount = touchPositions.size,
                isConfirmed = isConfirmed,
                showLockInButton = showLockInButton,

                onReset = {

                    touchPositions.clear()

                    isConfirmed = false

                    showWipeAnimation = false

                    wipeProgress = 0f

                    showLockInButton = false
                },

                onConfirm = {
                    // This is now handled by the Lock In button
                }
            )


            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
    }
}


// ============================================================================
// HEADER
// ============================================================================

@Composable
private fun TargetDetectionHeader(
    onBack: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 18.dp,
                top = 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp)
        ) {

            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.ArrowBack,

                contentDescription = "Back",

                tint = BallStarsColor.GlowCyan,

                modifier = Modifier.size(30.dp)
            )
        }


        Spacer(
            modifier = Modifier.width(4.dp)
        )


        Column {

            Text(
                text = "Set Up The Zone",

                color =
                    BallStarsColor.TextPrimary,

                fontSize = 25.sp,

                fontWeight = FontWeight.Bold
            )


            Text(
                text =
                    "Tap the 9 grid positions in order",

                color =
                    BallStarsColor.TextSecondary,

                fontSize = 14.sp
            )
        }
    }
}


// ============================================================================
// PROGRESS 1 → 9
// ============================================================================

@Composable
private fun CalibrationProgress(
    completedSteps: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(
                6.dp,
                Alignment.CenterHorizontally
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        repeat(totalSteps) { index ->

            val number = index + 1

            val completed =
                index < completedSteps

            val current =
                index == completedSteps &&
                    completedSteps < totalSteps


            val borderColor =
                when {
                    completed ->
                        BallStarsColor.Primary

                    current ->
                        BallStarsColor.GlowCyan

                    else ->
                        BallStarsColor.TextSecondary
                            .copy(alpha = 0.35f)
                }


            val background: Color =
                when {
                    completed ->
                        BallStarsColor.Primary
                            .copy(alpha = 0.20f)

                    current ->
                        BallStarsColor.GlowCyan
                            .copy(alpha = 0.18f)

                    else ->
                        BallStarsColor.Surface
                }


            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(background)
                    .border(
                        width =
                            if (current) 2.dp
                            else 1.dp,

                        color = borderColor,

                        shape = CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = number.toString(),

                    color =
                        when {
                            completed ->
                                BallStarsColor.Primary

                            current ->
                                BallStarsColor.GlowCyan

                            else ->
                                BallStarsColor.TextSecondary
                        },

                    fontSize = 12.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


// ============================================================================
// CAMERA APERTURE
//
// This is the critical fix.
//
// PreviewView and CalibrationCanvas share EXACTLY the same bounds.
//
// The cyan border is NOT part of the clipped container.
// It is drawn afterwards as a sibling overlay.
// ============================================================================

@Composable
private fun CameraAperture(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    touchPositions: List<Offset>,
    isConfirmed: Boolean,
    showWipeAnimation: Boolean,
    wipeProgress: Float,
    showLockInButton: Boolean,
    onCameraReady: (Camera) -> Unit,
    onTouch: (Offset) -> Unit,
    onLockIn: () -> Unit,
    onReset: () -> Unit,
    onViewportSizeChanged: (IntSize) -> Unit = {},
    zoomRatio: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {

    val apertureShape =
        RoundedCornerShape(24.dp)


    Box(
        modifier = modifier
    ) {

        // ------------------------------------------------------------
        // CLIPPED CAMERA CONTENT
        // ------------------------------------------------------------

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black)
                .onSizeChanged { size ->
                    onViewportSizeChanged(size)
                    android.util.Log.d(
                        "ScanTargets3",
                        "CALIBRATION VIEWPORT = ${size.width}x${size.height}"
                    )
                }
        ) {

            // --------------------------------------------------------
            // REAL CAMERA
            // --------------------------------------------------------

            CameraPreviewLayer(
                context = context,
                lifecycleOwner = lifecycleOwner,

                onCameraReady =
                    onCameraReady,

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(apertureShape)
                        .graphicsLayer {
                            clip = true
                            shape = apertureShape
                        }
            )


            // --------------------------------------------------------
            // TOUCH + GRID OVERLAY
            //
            // IMPORTANT:
            // Same parent and same bounds as camera.
            // --------------------------------------------------------

            CalibrationCanvas(
                touchPositions =
                    touchPositions,

                isConfirmed =
                    isConfirmed,

                showWipeAnimation =
                    showWipeAnimation,

                onTouch =
                    onTouch,

                modifier =
                    Modifier.fillMaxSize()
            )

            // --------------------------------------------------------
            // WIPE ANIMATION OVERLAY
            // --------------------------------------------------------

            if (showWipeAnimation && wipeProgress > 0f) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(apertureShape)
                ) {
                    val wipeHeight = size.height * wipeProgress

                    drawRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(
                            size.width,
                            wipeHeight
                        )
                    )
                }
            }

            // --------------------------------------------------------
            // LOCK-IN AND RESET BUTTONS
            // --------------------------------------------------------

            if (showLockInButton && !isConfirmed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(apertureShape),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Reset Button
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 60.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(30.dp)
                                ),
                            shape = RoundedCornerShape(30.dp),
                            // Default Material3 content padding (24dp/side)
                            // left too little room for the label on narrow
                            // phones once the 88%-width aperture and Row
                            // padding/spacing were accounted for.
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BallStarsColor.GlowCyan
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                BallStarsColor.GlowCyan
                            )
                        ) {
                            Text(
                                text = "RESET",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Lock In Button
                        Button(
                            onClick = onLockIn,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 60.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(30.dp),
                                    spotColor = BallStarsColor.GlowCyan
                                ),
                            shape = RoundedCornerShape(30.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BallStarsColor.GlowCyan,
                                contentColor = BallStarsColor.BgDeep
                            )
                        ) {
                            Text(
                                text = "LOCK IN",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // --------------------------------------------------------
            // ZOOM
            //
            // Horizontal bar across the bottom of the aperture, sized
            // to the aperture's own width — not a vertical strip down
            // the side of the whole screen.
            // --------------------------------------------------------

            ZoomControls(
                zoomRatio = zoomRatio,
                onZoomIn = onZoomIn,
                onZoomOut = onZoomOut,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(0.82f)
            )
        }


        // ------------------------------------------------------------
        // CYAN FRAME
        //
        // Separate from clipped content so PreviewView can NEVER
        // draw over it.
        // ------------------------------------------------------------

        Box(
            modifier = Modifier
                .matchParentSize()

                .border(
                    width = 4.dp,

                    color =
                        BallStarsColor.GlowCyan,

                    shape =
                        apertureShape
                )
        )


        // ------------------------------------------------------------
        // SUBTLE INNER GLOW
        // ------------------------------------------------------------

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)

                .border(
                    width = 1.dp,

                    color =
                        BallStarsColor.GlowCyan
                            .copy(alpha = 0.35f),

                    shape =
                        RoundedCornerShape(20.dp)
                )
        )
    }
}


// ============================================================================
// CAMERA PREVIEW
// ============================================================================

@Composable
private fun CameraPreviewLayer(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCameraReady: (Camera) -> Unit,
    modifier: Modifier = Modifier
) {

    val previewView = remember(context) {
        PreviewView(context).apply {
            /*
             * IMPORTANT:
             *
             * COMPATIBLE normally uses TextureView,
             * allowing the preview to participate in
             * normal UI compositing.
             *
             * DO NOT switch this to PERFORMANCE for
             * this design.
             */
            implementationMode =
                PreviewView
                    .ImplementationMode
                    .COMPATIBLE

            /*
             * FILL_CENTER fills our custom aperture.
             *
             * FIT_CENTER would expose black letterbox
             * bars because the camera source is 4:3.
             */
            scaleType =
                PreviewView
                    .ScaleType
                    .FILL_CENTER
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    DisposableEffect(
        lifecycleOwner,
        previewView
    ) {
        var disposed = false
        var previewUseCase: Preview? = null
        var cameraProvider: ProcessCameraProvider? = null

        val providerFuture =
            ProcessCameraProvider.getInstance(context)

        Log.i("CameraPreview", "CALIBRATION CAMERA BIND START")

        providerFuture.addListener({
            if (disposed) {
                Log.i("CameraPreview", "Camera provider returned after dispose — ignoring")
                return@addListener
            }

            try {
                cameraProvider = providerFuture.get()

                /*
                 * KEEP CAMERA SOURCE 4:3.
                 */
                val preview =
                    Preview.Builder()
                        .setTargetAspectRatio(
                            AspectRatio.RATIO_4_3
                        )
                        .build()
                        .also {
                            it.setSurfaceProvider(
                                previewView.surfaceProvider
                            )
                        }

                previewUseCase = preview

                // NO unbindAll() HERE
                val camera =
                    cameraProvider!!.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )

                Log.i("CameraPreview", "CALIBRATION CAMERA BOUND")
                onCameraReady(camera)

            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "Unable to bind CameraX preview",
                    exception
                )
            }

        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            Log.i("CameraPreview", "CALIBRATION CAMERA DISPOSE")

            previewUseCase?.let { preview ->
                try {
                    cameraProvider?.unbind(preview)
                    Log.i("CameraPreview", "CALIBRATION CAMERA UNBOUND")
                } catch (e: Exception) {
                    Log.e("CameraPreview", "CALIBRATION CAMERA UNBIND FAILED", e)
                }
            }
        }
    }
}


// ============================================================================
// ZOOM
// ============================================================================

@Composable
private fun ZoomControls(
    zoomRatio: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier,

        color =
            BallStarsColor.BgDeep
                .copy(alpha = 0.88f),

        shape =
            RoundedCornerShape(22.dp),

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                BallStarsColor.GlowCyan
                    .copy(alpha = 0.8f)
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(44.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Remove,

                    contentDescription =
                        "Zoom out",

                    tint =
                        BallStarsColor.GlowCyan
                )
            }

            Text(
                text =
                    "${(zoomRatio * 100).toInt()}%",

                color =
                    BallStarsColor.TextPrimary,

                fontSize = 14.sp,

                fontWeight =
                    FontWeight.Bold,

                textAlign =
                    TextAlign.Center,

                modifier =
                    Modifier.weight(1f)
            )

            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(44.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,

                    contentDescription =
                        "Zoom in",

                    tint =
                        BallStarsColor.GlowCyan
                )
            }
        }
    }
}


// ============================================================================
// INSTRUCTION PANEL
// ============================================================================

@Composable
private fun InstructionOverlay(
    touchCount: Int,
    isConfirmed: Boolean
) {

    val title: String

    val subtitle: String


    when {

        isConfirmed -> {

            title =
                "Grid set!"

            subtitle =
                "The Nexus Grid is ready"
        }


        touchCount >= 9 -> {

            title =
                "Grid set!"

            subtitle =
                "Confirm your scoring zone"
        }


        else -> {

            title =
                "Tap position ${touchCount + 1}"

            subtitle =
                GridPointLabels[touchCount]
        }
    }


    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp),

        color =
            BallStarsColor.Surface
                .copy(alpha = 0.96f),

        shape =
            RoundedCornerShape(18.dp),

        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,

                BallStarsColor.GlowCyan
                    .copy(alpha = 0.45f)
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // Step / success marker

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)

                    .background(
                        color = if (touchCount >= 9)
                            BallStarsColor.Primary
                                .copy(alpha = 0.20f)
                        else
                            BallStarsColor.GlowCyan
                                .copy(alpha = 0.18f)
                    )

                    .border(
                        2.dp,

                        if (touchCount >= 9)
                            BallStarsColor.Primary
                        else
                            BallStarsColor.GlowCyan,

                        CircleShape
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        if (touchCount >= 9)
                            "✓"
                        else
                            "${touchCount + 1}",

                    color =
                        if (touchCount >= 9)
                            BallStarsColor.Primary
                        else
                            BallStarsColor.GlowCyan,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize = 18.sp
                )
            }


            Spacer(
                Modifier.width(14.dp)
            )


            Column {

                Text(
                    text = title,

                    color =
                        BallStarsColor.TextPrimary,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize = 18.sp
                )


                Text(
                    text = subtitle,

                    color =
                        BallStarsColor.TextSecondary,

                    fontSize = 14.sp
                )
            }
        }
    }
}


// ============================================================================
// RESET + CONFIRM
// ============================================================================

@Composable
private fun CalibrationControls(
    touchCount: Int,
    isConfirmed: Boolean,
    showLockInButton: Boolean,
    onReset: () -> Unit,
    onConfirm: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp),

        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        OutlinedButton(
            onClick = onReset,

            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp),

            shape =
                RoundedCornerShape(18.dp),

            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor =
                        BallStarsColor.GlowCyan
                )
        ) {

            Text(
                text = "Reset",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }


        // Don't show confirm button if lock-in button is visible
        if (
            touchCount == 9 &&
            !isConfirmed &&
            !showLockInButton
        ) {

            Button(
                onClick = onConfirm,

                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            BallStarsColor.Primary,

                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text = "Confirm",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


// ============================================================================
// DECORATIVE BALLSTARS ARTWORK
//
// Keep this SIMPLE until camera geometry is proven.
// Do NOT stretch decorative edge assets across the entire screen.
// ============================================================================

@Composable
private fun BallStarsFrameArtwork() {

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        // Base environment background image

        Image(
            painter = painterResource(Res.drawable.target_header_background),
            contentDescription = "BallStars urban environment background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )

        // Left edge border with urban texture gradient
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0A1929).copy(alpha = 0.9f),
                            Color(0xFF132A42).copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Right edge border with urban texture gradient
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .align(Alignment.CenterEnd)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF132A42).copy(alpha = 0.6f),
                            Color(0xFF0A1929).copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Header background with urban atmosphere gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF15334B).copy(alpha = 0.95f),
                            Color(0xFF0E2236).copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}


// ============================================================================
// CALIBRATION CANVAS
//
// This version is included so the camera and touch layer definitely share
// the same coordinate system.
//
// If your existing CalibrationCanvas has additional animation code,
// preserve that code inside this composable.
// ============================================================================

@Composable
private fun CalibrationCanvas(
    touchPositions: List<Offset>,
    isConfirmed: Boolean,
    showWipeAnimation: Boolean,
    onTouch: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier = modifier

            .pointerInput(
                isConfirmed,
                touchPositions.size
            ) {

                detectTapGestures { offset ->

                    if (
                        !isConfirmed &&
                        touchPositions.size < 9
                    ) {

                        onTouch(offset)
                    }
                }
            }
    ) {

        // ------------------------------------------------------------
        // CALIBRATION TOUCH MARKERS
        // ------------------------------------------------------------

        if (!isConfirmed) {

            touchPositions.forEachIndexed {
                    index,
                    position ->

                // Glow

                drawCircle(
                    color =
                        Color(0xFF27E6F5)
                            .copy(alpha = 0.22f),

                    radius = 24.dp.toPx(),

                    center = position
                )


                // Outer ring

                drawCircle(
                    color =
                        Color(0xFF27E6F5),

                    radius = 15.dp.toPx(),

                    center = position,

                    style =
                        Stroke(
                            width = 3.dp.toPx()
                        )
                )


                // Centre

                drawCircle(
                    color =
                        Color(0xFF1ED36A),

                    radius = 5.dp.toPx(),

                    center = position
                )
            }
        }


        // ------------------------------------------------------------
        // CONFIRMED GRID
        // ------------------------------------------------------------

        if (
            isConfirmed &&
            touchPositions.size == 9
        ) {

            /*
             * IMPORTANT:
             *
             * If your existing file already has drawNexusGrid(...)
             * call that implementation here instead.
             *
             * Do not replace its geometry.
             */

            drawNexusGrid(
                touchPositions =
                    touchPositions
            )
        }
    }
}

/**
 * Draw The Nexus Grid - the confirmed 3x3 grid after successful calibration
 * Center box (4) is 1.5x the size of the others
 * Outer boxes form a ring: boxes join edge-to-edge without overlap
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNexusGrid(
    touchPositions: List<Offset>
) {
    if (touchPositions.size != 9) return

    // Calculate average spacing between touch points
    val avgHorizontalSpacing = (
        (touchPositions[1].x - touchPositions[0].x) +
        (touchPositions[2].x - touchPositions[1].x) +
        (touchPositions[4].x - touchPositions[3].x) +
        (touchPositions[5].x - touchPositions[4].x) +
        (touchPositions[7].x - touchPositions[6].x) +
        (touchPositions[8].x - touchPositions[7].x)
    ) / 6f

    val avgVerticalSpacing = (
        (touchPositions[3].y - touchPositions[0].y) +
        (touchPositions[4].y - touchPositions[1].y) +
        (touchPositions[5].y - touchPositions[2].y) +
        (touchPositions[6].y - touchPositions[3].y) +
        (touchPositions[7].y - touchPositions[4].y) +
        (touchPositions[8].y - touchPositions[5].y)
    ) / 6f

    // Base size for corner boxes
    val baseSize = minOf(avgHorizontalSpacing, avgVerticalSpacing) * 0.8f

    // Center box is 1.5x bigger (square)
    val centerBoxSize = baseSize * 1.5f

    // Corner boxes are square (standard size)
    val cornerBoxSize = baseSize

    // Top/Bottom boxes: width matches center, height is standard
    val topBottomWidth = centerBoxSize
    val topBottomHeight = baseSize

    // Left/Right boxes: height matches center, width is standard
    val leftRightWidth = baseSize
    val leftRightHeight = centerBoxSize

    // Each box is centered EXACTLY on the tap that was recorded for that
    // grid position — size still comes from the averaged spacing above
    // (keeps the "square" look), but placement is never derived from an
    // offset relative to touch point 4. This lets the grid follow any
    // perspective skew in the actual taps instead of collapsing to a
    // perfect axis-aligned rectangle.
    val sizeByIndex = mapOf(
        0 to (cornerBoxSize to cornerBoxSize),
        1 to (topBottomWidth to topBottomHeight),
        2 to (cornerBoxSize to cornerBoxSize),
        3 to (leftRightWidth to leftRightHeight),
        4 to (centerBoxSize to centerBoxSize),
        5 to (leftRightWidth to leftRightHeight),
        6 to (cornerBoxSize to cornerBoxSize),
        7 to (topBottomWidth to topBottomHeight),
        8 to (cornerBoxSize to cornerBoxSize)
    )

    val boxes = (0 until 9).map { i ->
        val (width, height) = sizeByIndex.getValue(i)
        val tap = touchPositions[i]
        Box(i, tap.x - width / 2f, tap.y - height / 2f, width, height)
    }

    // Draw all boxes
    boxes.forEach { box ->
        // Fill
        drawRect(
            color = BallStarsColor.GlowCyan.copy(alpha = 0.15f),
            topLeft = Offset(box.x, box.y),
            size = androidx.compose.ui.geometry.Size(box.width, box.height)
        )

        // Border - thicker lines for better visibility
        drawRect(
            color = BallStarsColor.GlowCyan,
            topLeft = Offset(box.x, box.y),
            size = androidx.compose.ui.geometry.Size(box.width, box.height),
            style = Stroke(width = 8f)
        )
    }

    // Draw center point markers at each of the 9 touch positions
    touchPositions.forEach { position ->
        drawCircle(
            color = BallStarsColor.GlowCyan,
            radius = 12f,
            center = position
        )
        drawCircle(
            color = BallStarsColor.GlowCyan,
            radius = 8f,
            center = position
        )
        drawCircle(
            color = BallStarsColor.GlowCyan.copy(alpha = 0.4f),
            radius = 18f,
            center = position
        )
    }
}

/**
 * Data class to hold box position and size
 */
private data class Box(
    val index: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Convert touch positions to GridTargets using same logic as drawNexusGrid
 */
private fun createGridTargetsFromTouchPositions(touchPositions: List<Offset>): List<GridTarget> {
    require(touchPositions.size == 9) { "Must have 9 touch positions" }

    // Calculate average spacing (same as drawNexusGrid)
    val avgHorizontalSpacing = (
        (touchPositions[1].x - touchPositions[0].x) +
        (touchPositions[2].x - touchPositions[1].x) +
        (touchPositions[4].x - touchPositions[3].x) +
        (touchPositions[5].x - touchPositions[4].x) +
        (touchPositions[7].x - touchPositions[6].x) +
        (touchPositions[8].x - touchPositions[7].x)
    ) / 6f

    val avgVerticalSpacing = (
        (touchPositions[3].y - touchPositions[0].y) +
        (touchPositions[4].y - touchPositions[1].y) +
        (touchPositions[5].y - touchPositions[2].y) +
        (touchPositions[6].y - touchPositions[3].y) +
        (touchPositions[7].y - touchPositions[4].y) +
        (touchPositions[8].y - touchPositions[5].y)
    ) / 6f

    // Base size for corner boxes
    val baseSize = kotlin.math.min(avgHorizontalSpacing, avgVerticalSpacing) * 0.8f

    // Center box is 1.5x bigger
    val centerBoxSize = baseSize * 1.5f

    // Corner boxes are square
    val cornerBoxSize = baseSize

    // Top/Bottom boxes: width matches center, height is standard
    val topBottomWidth = centerBoxSize
    val topBottomHeight = baseSize

    // Left/Right boxes: height matches center, width is standard
    val leftRightWidth = baseSize
    val leftRightHeight = centerBoxSize

    // Each box is centered EXACTLY on the tap that was recorded for that
    // grid position (same placement rule as drawNexusGrid's live preview).
    // Size still comes from the averaged spacing above, but position is
    // never derived from an offset relative to touch point 4 — this lets
    // the saved grid follow any perspective skew in the actual taps.
    val sizeByIndex = mapOf(
        0 to (cornerBoxSize to cornerBoxSize),
        1 to (topBottomWidth to topBottomHeight),
        2 to (cornerBoxSize to cornerBoxSize),
        3 to (leftRightWidth to leftRightHeight),
        4 to (centerBoxSize to centerBoxSize),
        5 to (leftRightWidth to leftRightHeight),
        6 to (cornerBoxSize to cornerBoxSize),
        7 to (topBottomWidth to topBottomHeight),
        8 to (cornerBoxSize to cornerBoxSize)
    )

    val boxes = (0 until 9).map { i ->
        val (width, height) = sizeByIndex.getValue(i)
        val tap = touchPositions[i]
        Box(i, tap.x - width / 2f, tap.y - height / 2f, width, height)
    }

    // Convert boxes to GridTargets
    return boxes.map { box ->
        GridTarget(
            index = box.index,
            x = box.x,
            y = box.y,
            width = box.width,
            height = box.height
        )
    }
}