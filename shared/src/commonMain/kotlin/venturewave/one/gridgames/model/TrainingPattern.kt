package venturewave.one.gridgames.model

import kotlinx.serialization.Serializable

/**
 * Represents a training pattern for the grid game
 *
 * @param id Unique identifier
 * @param name Display name (e.g., "Quick Reflex")
 * @param description Brief description
 * @param targetSequence List of target positions (1-9) in order to hit
 * @param difficultyLevel Difficulty rating (1-5)
 * @param estimatedDuration Estimated time to complete in seconds
 * @param isCustom Whether this is a user-created pattern (default: false for bundled patterns)
 */
@Serializable
data class TrainingPattern(
    val id: String,
    val name: String,
    val description: String,
    val targetSequence: List<Int>,
    val difficultyLevel: Int,
    val estimatedDuration: Int,
    val isCustom: Boolean = false
) {
    companion object {
        /**
         * Bundled training patterns included with the app
         */
        fun getBundledPatterns(): List<TrainingPattern> = listOf(
            TrainingPattern(
                id = "triangles",
                name = "Triangles",
                description = "Triangle patterns for footwork precision",
                targetSequence = listOf(5, 7, 4, 5),
                difficultyLevel = 1,
                estimatedDuration = 8
            ),
            TrainingPattern(
                id = "the_cruff",
                name = "The Cruff",
                description = "Cruyff turn inspired movement pattern",
                targetSequence = listOf(5, 2, 4, 5, 6, 8),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "rollers",
                name = "Rollers",
                description = "Rolling ball control across the grid",
                targetSequence = listOf(4, 5, 6, 4, 5, 6),
                difficultyLevel = 1,
                estimatedDuration = 9
            ),
            TrainingPattern(
                id = "penguin_feet",
                name = "Penguin Feet",
                description = "Quick side-to-side footwork drill",
                targetSequence = listOf(4, 6, 4, 6, 5),
                difficultyLevel = 1,
                estimatedDuration = 8
            ),
            TrainingPattern(
                id = "vs",
                name = "V's",
                description = "V-shaped movement pattern",
                targetSequence = listOf(7, 5, 9, 5, 7),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "box_run",
                name = "Box Run",
                description = "Square perimeter running pattern",
                targetSequence = listOf(1, 2, 3, 6, 9, 8, 7, 4, 1),
                difficultyLevel = 2,
                estimatedDuration = 15
            ),
            TrainingPattern(
                id = "zig_zag",
                name = "Zig Zag",
                description = "Diagonal zig-zag pattern",
                targetSequence = listOf(1, 3, 7, 9, 3, 7),
                difficultyLevel = 2,
                estimatedDuration = 12
            ),
            TrainingPattern(
                id = "cross_pattern",
                name = "Cross Pattern",
                description = "Plus sign cross formation",
                targetSequence = listOf(2, 4, 5, 6, 8, 5),
                difficultyLevel = 1,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "diamond",
                name = "Diamond",
                description = "Diamond shape movement drill",
                targetSequence = listOf(2, 4, 8, 6, 2),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "figure_8",
                name = "Figure 8",
                description = "Flowing figure-eight pattern",
                targetSequence = listOf(1, 2, 5, 8, 9, 6, 5, 4, 1),
                difficultyLevel = 3,
                estimatedDuration = 18
            ),
            TrainingPattern(
                id = "ladder",
                name = "Ladder",
                description = "Ladder drill up and down",
                targetSequence = listOf(7, 4, 1, 2, 3, 6, 9, 8, 7),
                difficultyLevel = 3,
                estimatedDuration = 16
            ),
            TrainingPattern(
                id = "spiral",
                name = "Spiral",
                description = "Spiral from outside to center",
                targetSequence = listOf(1, 2, 3, 6, 9, 8, 7, 4, 5),
                difficultyLevel = 3,
                estimatedDuration = 18
            ),
            TrainingPattern(
                id = "corner_to_centre",
                name = "Corner to Centre",
                description = "From corners to center point",
                targetSequence = listOf(1, 5, 3, 5, 9, 5, 7, 5),
                difficultyLevel = 2,
                estimatedDuration = 14
            ),
            TrainingPattern(
                id = "edge_to_centre",
                name = "Edge to Centre",
                description = "From edges inward to center",
                targetSequence = listOf(2, 5, 4, 5, 6, 5, 8, 5),
                difficultyLevel = 2,
                estimatedDuration = 14
            ),
            TrainingPattern(
                id = "combo_flow",
                name = "Combo Flow",
                description = "Advanced combination pattern",
                targetSequence = listOf(1, 3, 5, 7, 9, 6, 3, 4, 5, 8, 1),
                difficultyLevel = 4,
                estimatedDuration = 22
            )
        )
    }
}

/**
 * Represents the result of a completed game session
 *
 * @param pattern The pattern that was played
 * @param score Final score (e.g., based on speed and accuracy)
 * @param totalTime Total time taken in milliseconds
 * @param hitCount Number of successful target hits
 * @param missCount Number of missed targets
 * @param accuracy Hit rate percentage (0-100)
 * @param patternsCompleted Number of full loops through pattern.targetSequence
 *   completed during the session (e.g. Triangles is [5, 7, 4, 5] — hitting
 *   5→7→4→5 once counts as 1, regardless of which pattern was played).
 */
data class GameResult(
    val pattern: TrainingPattern,
    val score: Int,
    val totalTime: Long,
    val hitCount: Int,
    val missCount: Int,
    val accuracy: Float,
    val patternsCompleted: Int = 0
)
