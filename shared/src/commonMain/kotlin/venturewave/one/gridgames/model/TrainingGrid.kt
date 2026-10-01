package venturewave.one.gridgames.model

/**
 * Platform-agnostic logging interface
 * Actual implementations provided per-platform
 */
expect fun logInfo(tag: String, message: String)

/**
 * Singleton managing the calibrated training grid coordinates
 * The grid defines the actual screen positions for each named target box
 */
object TrainingGrid {

    var gridBoxes: MutableList<Box> = mutableListOf()

    fun logTrainingGrid(tagMessage: String? = null) {
        val tag = tagMessage ?: "TrainingGrid"

        logInfo(tag, "Starting to log")

        if (gridBoxes.count() == 0) {
            logInfo(tag, "No boxes in grid")
        }

        gridBoxes.forEach {
            logInfo(
                tag,
                "${it.detectedLabel}: (${it.left}, ${it.top}), (${it.right}, ${it.bottom})"
            )
        }
    }
}
