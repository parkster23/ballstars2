package venturewave.one.gridgames.model

/**
 * Represents a target box in a training pattern sequence
 * Extends Box with sequence information and hit tracking
 */
class TargetBox(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    val sequence: Int,
    var hit: Boolean,
    var endBox: Boolean = false,
    detectedLabel: String,
) : Box(
    left,
    top,
    right,
    bottom,
    detectedLabel,
) {
    /**
     * Overloaded constructor with default hit=false
     * Used when creating targets from training data
     */
    constructor(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        sequence: Int,
        endBox: Boolean,
        detectedLabel: String,
    ) : this(
        left,
        top,
        right,
        bottom,
        sequence,
        hit = false,
        endBox = endBox,
        detectedLabel = detectedLabel,
    )
}
