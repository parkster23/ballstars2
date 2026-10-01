package venturewave.one.gridgames.model

/**
 * Base class representing a box with screen coordinates and a label
 */
open class Box(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val detectedLabel: String
)

/**
 * Constants for box position labels in the training grid
 */
object BoxLabel {
    const val CENTRE = "centre"
    const val TOP_RIGHT = "top-right"
    const val CENTRE_RIGHT = "centre-right"
    const val BOTTOM_RIGHT = "bottom-right"
    const val CENTRE_BOTTOM = "centre-bottom"
    const val BOTTOM_LEFT = "bottom-left"
    const val CENTRE_TOP = "centre-top"
    const val TOP_LEFT = "top-left"
    const val CENTRE_LEFT = "centre-left"
}
