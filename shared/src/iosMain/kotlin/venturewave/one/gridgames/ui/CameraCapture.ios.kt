package venturewave.one.gridgames.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset1280x720
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.Foundation.NSNumber
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

/**
 * iOS camera capture using AVFoundation - mirrors the role of CameraX's
 * Preview + ImageAnalysis used on Android (GameScreen.android.kt's
 * CameraPreview / ScanTargets3Screen.kt's CameraPreviewLayer).
 *
 * NOT YET TESTED on a simulator or device: this machine's Xcode can't open
 * iosApp.xcodeproj to produce a runnable .app (see IOS_HANDOFF.md at the
 * repo root for why, and what's needed to finish verifying this on a
 * machine with working Xcode). It IS verified to Kotlin-compile via
 * `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
 * :shared:linkDebugFrameworkIosArm64` - that's the extent of what could be
 * confirmed here.
 *
 * Written against Kotlin/Native's documented AVFoundation + UIKitView
 * interop pattern:
 * https://kotlinlang.org/docs/multiplatform/compose-uikit-integration.html
 */
@OptIn(ExperimentalForeignApi::class)
class IosCameraController(
    private val onFrame: ((CMSampleBufferRef) -> Unit)? = null
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    val session: AVCaptureSession = AVCaptureSession()
    private var started = false

    /** The active camera device, available once [start] has run. */
    var device: AVCaptureDevice? = null
        private set

    /**
     * Configures and starts the capture session: back camera input +
     * a video data output delivering frames to [onFrame]. Safe to call
     * more than once - subsequent calls are no-ops while already running.
     */
    fun start() {
        if (started) return
        started = true

        session.beginConfiguration()
        session.sessionPreset = AVCaptureSessionPreset1280x720

        val camera = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        device = camera

        if (camera != null) {
            val input = AVCaptureDeviceInput.deviceInputWithDevice(camera, error = null)
            if (input != null && session.canAddInput(input)) {
                session.addInput(input)
            }
        }

        if (onFrame != null) {
            val output = AVCaptureVideoDataOutput()
            output.alwaysDiscardsLateVideoFrames = true
            // TFLObjectDetector's GMLImage(sampleBuffer:) explicitly requires
            // kCVPixelFormatType_32BGRA frames from AVCaptureVideoDataOutput -
            // using any other format silently produces wrong detections.
            output.videoSettings = mapOf(
                kCVPixelBufferPixelFormatTypeKey to NSNumber(unsignedInt = kCVPixelFormatType_32BGRA)
            )
            output.setSampleBufferDelegate(this, queue = dispatch_get_main_queue())
            if (session.canAddOutput(output)) {
                session.addOutput(output)
            }
        }

        session.commitConfiguration()
        session.startRunning()
    }

    fun stop() {
        if (!started) return
        started = false
        session.stopRunning()
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        val sampleBuffer = didOutputSampleBuffer ?: return
        onFrame?.invoke(sampleBuffer)
    }
}

/**
 * Compose wrapper embedding an AVCaptureVideoPreviewLayer-backed UIView,
 * mirroring the AndroidView-wraps-PreviewView pattern already used on
 * Android. Pass [onFrame] to additionally receive live frames (e.g. for
 * ball detection during gameplay); omit it for a calibration-only preview
 * with no frame analysis overhead.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
fun IosCameraPreview(
    modifier: Modifier = Modifier,
    onFrame: ((CMSampleBufferRef) -> Unit)? = null,
    onCameraReady: (IosCameraController) -> Unit = {}
) {
    UIKitView(
        factory = {
            val controller = IosCameraController(onFrame)
            val previewLayer = AVCaptureVideoPreviewLayer(session = controller.session)
            previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill

            val view = object : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
                override fun layoutSubviews() {
                    super.layoutSubviews()
                    previewLayer.frame = this.bounds
                }
            }
            view.layer.addSublayer(previewLayer)

            controller.start()
            onCameraReady(controller)

            view
        },
        modifier = modifier
    )
}
