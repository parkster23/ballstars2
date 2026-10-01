package venturewave.one.gridgames.detection

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector

/**
 * Ball detection helper using TensorFlow Lite
 * Based on BallStars ObjectDetectorHelper
 */
class BallDetectorHelper(
    var threshold: Float = 0.3f,
    var numThreads: Int = 2,
    var maxResults: Int = 1,
    var currentDelegate: Int = 0,
    val context: Context,
    val detectorListener: DetectorListener?
) {
    private var objectDetector: ObjectDetector? = null

    // Reused across detect() calls to avoid memory churn
    private var cachedRotationSteps: Int? = null
    private var cachedImageProcessor: ImageProcessor? = null
    private val reusableTensorImage = TensorImage()

    init {
        setupObjectDetector()
    }

    fun clearObjectDetector() {
        objectDetector?.close()
        objectDetector = null
    }

    fun setupObjectDetector() {
        // Close existing detector to prevent leaks
        objectDetector?.close()

        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default - use CPU
                Log.i("BallDetector", "Using CPU delegate with $numThreads threads")
            }
            DELEGATE_GPU -> {
                try {
                    if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                        baseOptionsBuilder.useGpu()
                        Log.i("BallDetector", "Using GPU delegate (Adreno GPU acceleration)")
                    } else {
                        Log.w("BallDetector", "GPU not supported on device, falling back to NNAPI")
                        baseOptionsBuilder.useNnapi()
                    }
                } catch (e: Exception) {
                    Log.w("BallDetector", "GPU delegate failed, falling back to NNAPI: ${e.message}")
                    baseOptionsBuilder.useNnapi()
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
                Log.i("BallDetector", "Using NNAPI delegate with $numThreads threads")
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            objectDetector = ObjectDetector.createFromFileAndOptions(
                context,
                "ball-tracker.tflite",
                optionsBuilder.build()
            )
        } catch (e: IllegalStateException) {
            detectorListener?.onError(
                "Ball detector failed to initialize. See error logs for details"
            )
            Log.e("BallDetector", "TFLite failed to load model with error: " + e.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        if (objectDetector == null) {
            setupObjectDetector()
        }

        var inferenceTime = SystemClock.uptimeMillis()

        // Preprocessor - rebuilt only when rotation changes
        val rotationSteps = -imageRotation / 90
        if (cachedImageProcessor == null || cachedRotationSteps != rotationSteps) {
            cachedImageProcessor = if (rotationSteps == 0) {
                ImageProcessor.Builder().build()
            } else {
                ImageProcessor.Builder().add(Rot90Op(rotationSteps)).build()
            }
            cachedRotationSteps = rotationSteps
        }
        val imageProcessor = cachedImageProcessor!!

        // Preprocess and detect
        reusableTensorImage.load(image)
        val tensorImage = imageProcessor.process(reusableTensorImage)

        val results = objectDetector?.detect(tensorImage)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        Log.i(
            "BallDetector",
            "INFER ts=${System.currentTimeMillis()} inferenceMs=$inferenceTime " +
                "resultCount=${results?.size ?: 0} topScore=${results?.firstOrNull()?.categories?.firstOrNull()?.score} " +
                "rotationSteps=$rotationSteps tensorSize=${tensorImage.width}x${tensorImage.height} " +
                "originalSize=${image.width}x${image.height}"
        )

        detectorListener?.onResults(
            results,
            inferenceTime,
            tensorImage.height,
            tensorImage.width
        )
    }

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: MutableList<Detection>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
    }
}
