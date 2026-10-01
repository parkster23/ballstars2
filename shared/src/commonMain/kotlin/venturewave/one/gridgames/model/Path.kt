package venturewave.one.gridgames.model

/**
 * Represents the path through a training pattern
 * Tracks the current position in the target sequence
 */
class Path(val root: TargetBox) {
    var targetToHitNumber: Int = 1
}
