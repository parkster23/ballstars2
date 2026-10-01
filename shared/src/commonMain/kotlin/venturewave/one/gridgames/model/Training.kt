package venturewave.one.gridgames.model

/**
 * Training status enumeration
 */
enum class TrainingStatus {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED
}

/**
 * Data class for bundled training patterns (legacy format)
 * Replaces Amplify-backed TrainingRepo data source
 * TODO: Migrate to new TrainingPattern format
 */
data class LegacyTrainingPattern(
    val name: String,
    val utterance: String,
    val targets: List<TrainingTarget>
)

data class TrainingTarget(
    val targetName: String,
    val order: Int,
    val endFlag: Boolean = false
)

/**
 * Singleton managing the current training state and scoring logic
 * Core scoring algorithm ported exactly as-is from production - already debugged
 */
object Training {
    // Hardcoded for testing
    var trainingStatus: TrainingStatus = TrainingStatus.NOT_STARTED
    var targetHitCounter = -1
    var trainingStarted: Boolean = false

    // From training data
    var trainingName: String = ""
    var trainingUtterance: String = ""

    var targetBoxes: MutableList<TargetBox>? = null
    private var path: Path? = null

    /**
     * Reset all hit flags on target boxes
     * Called when a lap completes
     */
    fun clearAllHits() {
        targetBoxes?.forEach { it.hit = false }
    }

    /**
     * Invert horizontal target positions
     * Used for mirror-mode training variants
     */
    fun invertHorizontalTargets() {
        val newTrainingGrid = mutableListOf<TargetBox>()
        targetBoxes?.forEach {
            when (it.detectedLabel) {
                "centre-left" -> {
                    newTrainingGrid.add(
                        targetBoxSelektor(
                            "centre-right",
                            it.sequence,
                            it.endBox
                        )
                    )
                }
                "top-left" -> {
                    newTrainingGrid.add(targetBoxSelektor("top-right", it.sequence, it.endBox))
                }
                "bottom-left" -> {
                    newTrainingGrid.add(
                        targetBoxSelektor(
                            "bottom-right",
                            it.sequence,
                            it.endBox
                        )
                    )
                }
                "centre-right" -> {
                    newTrainingGrid.add(
                        targetBoxSelektor(
                            "centre-left",
                            it.sequence,
                            it.endBox
                        )
                    )
                }
                "bottom-right" -> {
                    newTrainingGrid.add(
                        targetBoxSelektor(
                            "bottom-left",
                            it.sequence,
                            it.endBox
                        )
                    )
                }
                "top-right" -> {
                    newTrainingGrid.add(targetBoxSelektor("top-left", it.sequence, it.endBox))
                }
                "centre" -> {
                    newTrainingGrid.add(targetBoxSelektor("centre", it.sequence, it.endBox))
                }
                "centre-bottom" -> {
                    newTrainingGrid.add(targetBoxSelektor("centre-bottom", it.sequence, it.endBox))
                }
                "centre-top" -> {
                    newTrainingGrid.add(targetBoxSelektor("centre-top", it.sequence, it.endBox))
                }
            }
        }
        targetBoxes?.clear()
        newTrainingGrid.forEach {
            targetBoxes?.add(it)
        }
    }

    /**
     * Find the next target box to hit in the sequence
     * Returns null if training data not ready or no match found
     *
     * CRITICAL: This null-safe handling prevents crashes when gameplay starts
     * before training data has loaded - preserved exactly from production fix
     */
    fun findNextTargetBox(): TargetBox? {
        val boxes = targetBoxes
        val currentPath = path
        if (boxes.isNullOrEmpty() || currentPath == null) {
            // trainingBuilder() hasn't finished yet - used to crash here by calling
            // getPath() which throws when path is null. Now just waits for data.
            logInfo(
                "BSDiag",
                "FIND_NEXT_TARGET not ready: boxes=${boxes?.size} pathSet=${currentPath != null}"
            )
            return null
        }
        val nextTargetbox =
            boxes.find { it.sequence == currentPath.targetToHitNumber && !it.hit }
        if (nextTargetbox != null) {
            logInfo("Next TargetBox#:", nextTargetbox.detectedLabel)
        } else {
            logInfo("Next TargetBox#:", "Not Found")
        }
        return nextTargetbox
    }

    /**
     * Build a training from bundled local data (no network calls)
     * Replaces Amplify-backed TrainingRepo approach
     */
    fun trainingBuilder(trainingName: String) {
        this.targetBoxes = null
        this.targetHitCounter = 0

        // Clear path to prevent stale sequence numbers from previous training
        // (Critical fix: was causing hits to stop registering entirely - see original comments)
        this.path = null

        // Get training pattern from bundled data
        val trainingPattern = BundledLegacyTrainings.getTraining(trainingName)
        if (trainingPattern == null) {
            logInfo("BSDiag", "TRAINING_NOT_FOUND name=\"$trainingName\"")
            return
        }

        this.trainingName = trainingPattern.name
        this.trainingUtterance = trainingPattern.utterance

        val trainingBoxes = mutableListOf<TargetBox>()

        trainingPattern.targets.forEach { target ->
            val trainingBox = targetBoxSelektor(target.targetName, target.order, target.endFlag)
            if (target.endFlag) {
                setPath(trainingBox)
            }
            this.targetHitCounter++
            trainingBoxes.add(trainingBox)
            logInfo(
                "tb#",
                "added:$trainingName, ${trainingBox.detectedLabel}, ${trainingBox.sequence}, ${trainingBox.endBox}, ${target.endFlag}"
            )
        }

        if (this.path == null && trainingBoxes.isNotEmpty()) {
            // None of this training's targets were flagged as end of sequence
            // Fall back to highest-sequence box (production fix preserved)
            logInfo(
                "BSDiag",
                "TRAINING_NO_ENDFLAG name=\"$trainingName\" - falling back to highest-sequence box as end"
            )
            setPath(trainingBoxes.maxByOrNull { it.sequence } ?: trainingBoxes.last())
        }

        // Log the resolved pattern (critical for debugging detection issues)
        logInfo(
            "BSDiag",
            "TRAINING_BUILT name=\"$trainingName\" boxCount=${trainingBoxes.size} targetHitCounter=$targetHitCounter"
        )
        trainingBoxes.forEachIndexed { index, box ->
            logInfo(
                "BSDiag",
                "TRAINING_BOX idx=$index seq=${box.sequence} label=${box.detectedLabel} endBox=${box.endBox} " +
                        "l=${box.left} t=${box.top} r=${box.right} b=${box.bottom} w=${box.right - box.left} h=${box.bottom - box.top}"
            )
        }

        this.targetBoxes = trainingBoxes
    }

    private fun targetBoxSelektor(boxName: String, sequence: Int, endFlag: Boolean): TargetBox {
        TrainingGrid.logTrainingGrid()
        val box = TrainingGrid.gridBoxes
            .filter { it.detectedLabel == boxName }
            .map {
                TargetBox(
                    it.left,
                    it.top,
                    it.right,
                    it.bottom,
                    sequence,
                    endFlag,
                    it.detectedLabel
                )
            }.firstOrNull()

        if (box == null) {
            logInfo("tb#:", "Box could not be found $boxName")
            throw NullPointerException("Training Box cannot be null, check the Grid")
        }
        return box
    }

    /**
     * Get the current path (throws if null)
     * Use pathOrNull() instead where throwing is not appropriate
     */
    fun getPath(): Path {
        if (path != null) {
            logInfo("path£", "root ${path!!.root.detectedLabel}, tth: ${path!!.targetToHitNumber}")
            return path as Path
        } else {
            throw IllegalStateException("Path can not be null")
        }
    }

    /**
     * Non-throwing path accessor
     * CRITICAL: Prevents crashes from race conditions when training is reselected
     * mid-frame (production fix - see original extensive comments)
     */
    fun pathOrNull(): Path? = path

    fun setPath(startAndEndBox: TargetBox) {
        path = Path(startAndEndBox)
    }
}

/**
 * Bundled training patterns - replaces Amplify data source (legacy format)
 * Start with 2-3 simple patterns for MVP testing
 */
object BundledLegacyTrainings {
    private val patterns = listOf(
        LegacyTrainingPattern(
            name = "Simple Triangle",
            utterance = "Hit three corners in sequence",
            targets = listOf(
                TrainingTarget("top-left", 1, false),
                TrainingTarget("top-right", 2, false),
                TrainingTarget("centre-bottom", 3, true) // end flag
            )
        ),
        LegacyTrainingPattern(
            name = "Center Cross",
            utterance = "Hit all four sides through center",
            targets = listOf(
                TrainingTarget("centre-top", 1, false),
                TrainingTarget("centre", 2, false),
                TrainingTarget("centre-right", 3, false),
                TrainingTarget("centre", 4, false),
                TrainingTarget("centre-bottom", 5, true)
            )
        ),
        LegacyTrainingPattern(
            name = "Full Square",
            utterance = "Hit all nine positions",
            targets = listOf(
                TrainingTarget("top-left", 1, false),
                TrainingTarget("centre-top", 2, false),
                TrainingTarget("top-right", 3, false),
                TrainingTarget("centre-right", 4, false),
                TrainingTarget("bottom-right", 5, false),
                TrainingTarget("centre-bottom", 6, false),
                TrainingTarget("bottom-left", 7, false),
                TrainingTarget("centre-left", 8, false),
                TrainingTarget("centre", 9, true)
            )
        )
    )

    fun getTraining(name: String): LegacyTrainingPattern? {
        return patterns.find { it.name == name }
    }

    fun getAllTrainingNames(): List<String> {
        return patterns.map { it.name }
    }
}
