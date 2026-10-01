package venturewave.one.gridgames.data.storage

import kotlinx.serialization.Serializable
import venturewave.one.gridgames.model.TrainingPattern

/**
 * Root container for all pattern-related data stored in JSON
 */
@Serializable
data class PatternStorage(
    val patterns: List<TrainingPattern> = emptyList(),
    val playerProgress: Map<String, PlayerProgress> = emptyMap()
)

/**
 * Player progress for a specific pattern
 */
@Serializable
data class PlayerProgress(
    val patternId: String,
    val personalBest: Int = 0,
    val timesPlayed: Int = 0,
    val lastPlayedTimestamp: Long = 0
)
