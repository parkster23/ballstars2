package venturewave.one.gridgames.detection

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreMedia.CMSampleBufferRef
import platform.Foundation.NSBundle
import platform.TensorFlowLiteTaskVision.GMLImage
import platform.TensorFlowLiteTaskVision.TFLObjectDetector
import platform.TensorFlowLiteTaskVision.TFLObjectDetectorOptions

/**
 * iOS ball detection via TensorFlowLiteTaskVision's TFLObjectDetector -
 * the iOS counterpart to Android's BallDetectorHelper.kt, which uses the
 * same TF Lite Task Library API shape
 * (org.tensorflow.lite.task.vision.detector.ObjectDetector) on that side.
 *
 * NOT YET TESTED on a simulator or device - this machine's Xcode can't
 * build a runnable .app (see IOS_HANDOFF.md). What WAS verified here:
 * `pod install` (iosApp/Podfile) fetched TensorFlowLiteTaskVision as a
 * prebuilt .framework, the Kotlin/Native cinterop binding against its
 * headers (shared/src/nativeInterop/cinterop/TensorFlowLiteTaskVision.def)
 * generated successfully, and this file compiles clean against those
 * bindings via `./gradlew :shared:compileKotlinIosSimulatorArm64`.
 * Linking (producing the final framework) and everything at runtime
 * (does the detector actually load the model, does inference produce
 * sane results) is unverified - that needs a machine where
 * iosApp.xcodeproj can actually be opened/built/run.
 *
 * Before this can run: `ball-tracker.tflite` (already in
 * androidApp/src/main/assets/) needs to be added as a bundle resource in
 * the Xcode project so `NSBundle.mainBundle.pathForResource` finds it.
 *
 * One real API gap vs. Android: TFLComputeSettings only exposes CPU
 * thread-count and an optional CoreML (Neural Engine) delegate - there's
 * no direct "GPU delegate" switch like Android's DELEGATE_GPU. CPU is the
 * safe default used here; Core ML delegate is a possible follow-up once
 * this is actually running and can be benchmarked.
 */
@OptIn(ExperimentalForeignApi::class)
class BallDetector(
    private val threshold: Float = 0.3f,
    private val numThreads: Int = 2,
    private val maxResults: Long = 1,
    private val modelFileName: String = "ball-tracker",
    private val detectorListener: DetectorListener? = null
) {
    private var objectDetector: TFLObjectDetector? = null

    init {
        setupObjectDetector()
    }

    fun setupObjectDetector() {
        val modelPath = NSBundle.mainBundle.pathForResource(modelFileName, ofType = "tflite")
        if (modelPath == null) {
            detectorListener?.onError(
                "Ball detector model not found in bundle: $modelFileName.tflite"
            )
            return
        }

        val options = TFLObjectDetectorOptions(modelPath = modelPath)
        options.baseOptions.computeSettings.cpuSettings.numThreads = numThreads.toLong()
        options.classificationOptions.scoreThreshold = threshold
        options.classificationOptions.maxResults = maxResults

        val detector = TFLObjectDetector.objectDetectorWithOptions(options, error = null)
        if (detector == null) {
            detectorListener?.onError("Ball detector failed to initialize")
        }
        objectDetector = detector
    }

    /**
     * Runs detection on a single camera frame. [sampleBuffer] should come
     * straight from AVCaptureVideoDataOutput (see CameraCapture.ios.kt) -
     * GMLImage's sample-buffer initializer expects exactly that, with
     * kCVPixelFormatType_32BGRA as the pixel format (set when configuring
     * AVCaptureVideoDataOutput.videoSettings).
     */
    fun detect(sampleBuffer: CMSampleBufferRef) {
        val detector = objectDetector ?: run {
            setupObjectDetector()
            objectDetector
        } ?: return

        val image = GMLImage(sampleBuffer = sampleBuffer) ?: return
        val result = detector.detectWithGMLImage(image, error = null)

        // NSArray<TFLDetection *> comes through cinterop as an erased
        // List<*> (Kotlin/Native doesn't bind Objective-C lightweight
        // generics), hence the explicit per-element cast.
        @Suppress("UNCHECKED_CAST")
        val detections = result?.detections.orEmpty() as List<platform.TensorFlowLiteTaskVision.TFLDetection>
        detectorListener?.onResults(detections)
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(detections: List<platform.TensorFlowLiteTaskVision.TFLDetection>)
    }
}
