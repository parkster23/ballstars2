package venturewave.one.gridgames.model

/**
 * Represents a single box in the calibrated Nexus Grid
 *
 * @param index Box position (0-8) in the grid layout:
 *   0 - 1 - 2
 *   |   |   |
 *   3 - 4 - 5
 *   |   |   |
 *   6 - 7 - 8
 * @param x Top-left X coordinate
 * @param y Top-left Y coordinate
 * @param width Box width in pixels
 * @param height Box height in pixels
 */
data class GridTarget(
    val index: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    /**
     * Check if a point (e.g., ball center) is within this box
     */
    fun contains(pointX: Float, pointY: Float): Boolean {
        return pointX >= x && pointX <= (x + width) &&
               pointY >= y && pointY <= (y + height)
    }

    /**
     * Get the center point of this box
     */
    fun center(): Pair<Float, Float> {
        return Pair(x + width / 2f, y + height / 2f)
    }
}

/**
 * The complete calibrated grid with all 9 boxes
 */
data class CalibratedGrid(
    val targets: List<GridTarget>,
    val zoomRatio: Float = 0f, // Zoom level used during calibration (0.0 to 1.0)
    val sourceViewportWidth: Float = 0f, // Width of calibration camera viewport
    val sourceViewportHeight: Float = 0f // Height of calibration camera viewport
) {
    init {
        require(targets.size == 9) { "Grid must have exactly 9 targets" }
    }

    /**
     * Get target by index (0-8)
     */
    operator fun get(index: Int): GridTarget {
        return targets.first { it.index == index }
    }

    /**
     * Find which target (if any) contains a given point
     */
    fun findTargetAt(x: Float, y: Float): GridTarget? {
        return targets.firstOrNull { it.contains(x, y) }
    }
}
