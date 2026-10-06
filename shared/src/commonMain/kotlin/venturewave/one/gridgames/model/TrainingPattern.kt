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
                id = "right_peak",
                name = "Right Peak",
                description = "Peak move finishing to the right",
                targetSequence = listOf(5, 7, 4, 5),
                difficultyLevel = 1,
                estimatedDuration = 8
            ),
            TrainingPattern(
                id = "left_peak",
                name = "Left Peak",
                description = "Peak move finishing to the left",
                targetSequence = listOf(5, 9, 6, 5),
                difficultyLevel = 1,
                estimatedDuration = 8
            ),
            TrainingPattern(
                id = "right_cruyff",
                name = "Right Cruyff",
                description = "Cruyff turn finishing to the right",
                targetSequence = listOf(5, 4, 1, 3, 5),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "left_cruyff",
                name = "Left Cruyff",
                description = "Cruyff turn finishing to the left",
                targetSequence = listOf(5, 6, 3, 1, 5),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "right_drift",
                name = "Right Drift",
                description = "Drift footwork finishing to the right",
                targetSequence = listOf(1, 3, 9, 1),
                difficultyLevel = 1,
                estimatedDuration = 8
            ),
            TrainingPattern(
                id = "left_drift",
                name = "Left Drift",
                description = "Drift footwork finishing to the left",
                targetSequence = listOf(3, 1, 7, 3),
                difficultyLevel = 1,
                estimatedDuration = 8
            ),
            TrainingPattern(
                id = "right_box_blast",
                name = "Right Box Blast",
                description = "Box Blast movement finishing to the right",
                targetSequence = listOf(1, 7, 9, 3, 1),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "left_box_blast",
                name = "Left Box Blast",
                description = "Box Blast movement finishing to the left",
                targetSequence = listOf(3, 9, 7, 1, 3),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "vs",
                name = "V's",
                description = "V-shaped movement pattern",
                targetSequence = listOf(5, 9, 5, 7, 5),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "dap_up",
                name = "Dap Up",
                description = "Dap Up footwork pattern",
                targetSequence = listOf(1, 5, 3, 5, 1),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "penguin_feet",
                name = "Penguin Feet",
                description = "Quick side-to-side footwork drill",
                targetSequence = listOf(5, 4, 5, 6, 5),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "right_crossover",
                name = "Right Crossover",
                description = "Crossover step finishing to the right",
                targetSequence = listOf(1, 9, 7, 3, 1),
                difficultyLevel = 2,
                estimatedDuration = 10
            ),
            TrainingPattern(
                id = "left_crossover",
                name = "Left Crossover",
                description = "Crossover step finishing to the left",
                targetSequence = listOf(3, 7, 9, 1, 3),
                difficultyLevel = 2,
                estimatedDuration = 10
            )
        )
    }
}

/**
 * Represents the result of a completed game session
 *
 * @param pattern The pattern that was played
 * @param score Final score: points per hit, plus a 3x completion bonus for
 *   each clean pattern lap, minus a penalty for each wrong-target hit.
 * @param totalTime Total time taken in milliseconds
 * @param hitCount Number of successful target hits
 * @param missCount Number of wrong-target hits
 * @param accuracy Hit rate percentage (0-100)
 * @param patternsCompleted Number of full loops through pattern.targetSequence
 *   completed during the session (e.g. Triangles is [5, 7, 4, 5] — hitting
 *   5→7→4→5 once counts as 1, regardless of which pattern was played).
 * @param bonusPoints Total extra points earned from the 3x completion
 *   multiplier on patterns finished without a wrong-target hit.
 * @param penaltyPoints Total points lost to wrong-target hits.
 */
data class GameResult(
    val pattern: TrainingPattern,
    val score: Int,
    val totalTime: Long,
    val hitCount: Int,
    val missCount: Int,
    val accuracy: Float,
    val patternsCompleted: Int = 0,
    val bonusPoints: Int = 0,
    val penaltyPoints: Int = 0
)
