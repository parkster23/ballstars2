package venturewave.one.gridgames.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import org.tensorflow.lite.task.vision.detector.Detection
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.LinkedBlockingQueue

/**
 * Ball Detector ViewModel - Frame Queue Processing
 *
 * Detects soccer balls in camera frames using TensorFlow Lite
 * Uses frame queue to buffer frames for processing
 */
class BallDetectorViewModel(
    private val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val threshold: Float = 0.3f,
    val maxResults: Int = 1,
    val numberOfThreads: Int = 2,
    val currentDelegate: Int = BallDetectorHelper.DELEGATE_CPU
) : ViewModel(), BallDetectorHelper.DetectorListener {

    private lateinit var ballDetectorHelper: BallDetectorHelper

    companion object {
        private const val TAG = "BallDetectorVM"
        private const val QUEUE_CAPACITY = 6
    }

    init {
        Log.i("BSDiag", "BALL_DETECTOR_VM_CREATED id=${System.identityHashCode(this)} ts=${System.currentTimeMillis()} threshold=$threshold maxResults=$maxResults")
    }

    var tensorImageHeight: Int = 0
    var tensorImageWidth: Int = 0

    // Frame queue for buffering
    private data class QueuedFrame(val bitmap: Bitmap, val rotation: Int)
    private val frameQueue = LinkedBlockingQueue<QueuedFrame>(QUEUE_CAPACITY)

    // Frame analysis executor - captures frames from camera
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Queue processor executor - processes queued frames
    private val queueProcessorExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile private var queueProcessorRunning = false
    @Volatile private var shouldStopQueueProcessor = false

    @Volatile private var loggedFrameDims = false

    // FPS tracking
    private var frameCount = 0
    private var lastFpsLogTime = 0L
    private val fpsLogInterval = 1000L // Log FPS every second

    // Detection results flow
    private val _detectionResults = MutableStateFlow<List<Detection>>(emptyList())
    val detectionResults: MutableStateFlow<List<Detection>>
        get() = _detectionResults

    // Inference time flow
    private val _inferenceTime = MutableStateFlow(0L)
    val inferenceTime: MutableStateFlow<Long>
        get() = _inferenceTime

    /**
     * Ensure queue processor is running
     */
    private fun ensureQueueProcessorRunning() {
        if (queueProcessorRunning) return

        synchronized(this) {
            if (queueProcessorRunning) return

            queueProcessorRunning = true
            shouldStopQueueProcessor = false

            queueProcessorExecutor.execute {
                Log.i("BSDiag", "BALL_QUEUE_PROCESSOR_STARTED ts=${System.currentTimeMillis()}")

                while (!shouldStopQueueProcessor) {
                    try {
                        // Block until frame available (with timeout to check stop flag)
                        val frame = frameQueue.poll(100, TimeUnit.MILLISECONDS)

                        if (frame != null) {
                            // Process the frame
                            ballDetectorHelper.detect(frame.bitmap, frame.rotation)

                            // Recycle bitmap after processing
                            frame.bitmap.recycle()
                        }
                    } catch (e: InterruptedException) {
                        Log.i("BSDiag", "BALL_QUEUE_PROCESSOR_INTERRUPTED ts=${System.currentTimeMillis()}")
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "Queue processor error", e)
                    }
                }

                queueProcessorRunning = false
                Log.i("BSDiag", "BALL_QUEUE_PROCESSOR_STOPPED ts=${System.currentTimeMillis()}")
            }
        }
    }

    /**
     * Get ImageAnalysis use case for camera binding
     */
    fun getImageAnalysisUseCase(): ImageAnalysis {
        // Initialize detector
        if (!::ballDetectorHelper.isInitialized) {
            ballDetectorHelper = BallDetectorHelper(
                context = context,
                threshold = threshold,
                numThreads = numberOfThreads,
                currentDelegate = currentDelegate,
                detectorListener = this
            )
        }

        // Start queue processor
        ensureQueueProcessorRunning()

        // Create image analysis use case with frame queuing
        return ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also {
                it.setAnalyzer(analysisExecutor) { imageProxy ->
                    try {
                        if (!loggedFrameDims) {
                            Log.i(
                                "BSDiag",
                                "BALL_CAMERA_FRAME_DIMS width=${imageProxy.width} height=${imageProxy.height} format=${imageProxy.format} ts=${System.currentTimeMillis()}"
                            )
                            loggedFrameDims = true
                        }

                        // Convert YUV to RGB bitmap
                        val bitmap = imageProxy.toBitmap()

                        // Try to add to queue (non-blocking)
                        val queued = frameQueue.offer(QueuedFrame(bitmap, imageProxy.imageInfo.rotationDegrees))

                        if (!queued) {
                            // Queue full, recycle bitmap and drop frame
                            bitmap.recycle()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing frame", e)
                    } finally {
                        // CRITICAL: Always close imageProxy to prevent buffer exhaustion
                        imageProxy.close()
                    }
                }
            }
    }

    /**
     * Stop processing frames (can be resumed later)
     * Call this when navigating away from the screen but keeping ViewModel alive
     */
    fun stopProcessing() {
        try {
            Log.i(TAG, "Stopping ball detection processing")

            // Stop queue processor
            shouldStopQueueProcessor = true

            // Wait for queue processor to finish current frame
            Thread.sleep(100)

            // Clear queue and recycle remaining bitmaps
            synchronized(this) {
                frameQueue.forEach { it.bitmap.recycle() }
                frameQueue.clear()
            }

            Log.i(TAG, "Ball detection processing stopped, queue cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping processing", e)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            // Stop queue processor
            shouldStopQueueProcessor = true

            // Clear queue and recycle remaining bitmaps
            frameQueue.forEach { it.bitmap.recycle() }
            frameQueue.clear()

            if (::ballDetectorHelper.isInitialized) {
                ballDetectorHelper.clearObjectDetector()
            }

            queueProcessorExecutor.shutdown()
            queueProcessorExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)

            analysisExecutor.shutdown()
            analysisExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // DO NOT call cleanup() here - it blocks with awaitTermination()
        // cleanup() will be called explicitly when truly destroying the ViewModel
        // DisposableEffect handles proper stopProcessing() on navigation
        Log.i(TAG, "BallDetectorViewModel.onCleared() - ViewModel cleared (executors NOT shutdown to allow reuse)")
    }

    // BallDetectorHelper.DetectorListener implementation
    override fun onError(error: String) {
        Log.e(TAG, "Detection error: $error")
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        tensorImageHeight = imageHeight
        tensorImageWidth = imageWidth

        // Update flows
        _detectionResults.value = results ?: emptyList()
        _inferenceTime.value = inferenceTime

        // FPS tracking
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (lastFpsLogTime == 0L) {
            lastFpsLogTime = currentTime
        }
        val timeSinceLastLog = currentTime - lastFpsLogTime
        if (timeSinceLastLog >= fpsLogInterval) {
            val fps = (frameCount * 1000f) / timeSinceLastLog
            Log.i(
                "BSDiag",
                "BALL_PERF fps=${String.format("%.1f", fps)} " +
                        "avgInferenceMs=${inferenceTime} " +
                        "frameCount=$frameCount " +
                        "intervalMs=$timeSinceLastLog"
            )
            frameCount = 0
            lastFpsLogTime = currentTime
        }

        // Log results - always log to see if we're getting any detections
        if (!results.isNullOrEmpty()) {
            val topDetection = results.first()
            Log.i(
                "BSDiag",
                "BALL_RESULT ts=${System.currentTimeMillis()} " +
                        "count=${results.size} " +
                        "inferenceMs=$inferenceTime " +
                        "topScore=${topDetection.categories.firstOrNull()?.score} " +
                        "label=${topDetection.categories.firstOrNull()?.label} " +
                        "box=${topDetection.boundingBox}"
            )
        }
    }
}
