package venturewave.one.gridgames.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import venturewave.one.gridgames.data.storage.FileStorage
import venturewave.one.gridgames.data.storage.PatternStorage
import venturewave.one.gridgames.data.storage.PlayerProgress
import venturewave.one.gridgames.model.TrainingPattern
import venturewave.one.gridgames.viewmodels.currentTimeMillis

/**
 * Repository for managing training patterns with JSON file persistence
 */
class PatternRepository(private val fileStorage: FileStorage) {
    private val filename = "patterns.json"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Load all patterns from storage, seeding with bundled patterns if file doesn't exist
     */
    suspend fun getAllPatterns(): List<TrainingPattern> {
        return try {
            val storage = loadStorage()
            if (storage.patterns.isEmpty()) {
                // First time - seed with bundled patterns
                val bundled = TrainingPattern.getBundledPatterns()
                savePatterns(bundled)
                bundled
            } else {
                storage.patterns
            }
        } catch (e: Exception) {
            println("PatternRepository.getAllPatterns error: ${e.message}")
            e.printStackTrace()
            // Fallback to bundled patterns
            TrainingPattern.getBundledPatterns()
        }
    }

    /**
     * Save patterns to storage
     */
    suspend fun savePatterns(patterns: List<TrainingPattern>) {
        try {
            val storage = loadStorage()
            val updated = storage.copy(patterns = patterns)
            saveStorage(updated)
        } catch (e: Exception) {
            println("PatternRepository.savePatterns error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Add a new custom pattern
     */
    suspend fun addPattern(pattern: TrainingPattern) {
        val patterns = getAllPatterns().toMutableList()
        patterns.add(pattern.copy(isCustom = true))
        savePatterns(patterns)
    }

    /**
     * Delete a pattern by ID (only custom patterns can be deleted)
     */
    suspend fun deletePattern(patternId: String) {
        val patterns = getAllPatterns()
        val pattern = patterns.find { it.id == patternId }

        if (pattern?.isCustom == true) {
            val updated = patterns.filter { it.id != patternId }
            savePatterns(updated)
        } else {
            throw IllegalArgumentException("Cannot delete bundled pattern")
        }
    }

    /**
     * Get player progress for a specific pattern
     */
    suspend fun getPlayerProgress(patternId: String): PlayerProgress? {
        return try {
            val storage = loadStorage()
            storage.playerProgress[patternId]
        } catch (e: Exception) {
            println("PatternRepository.getPlayerProgress error: ${e.message}")
            null
        }
    }

    /**
     * Update player progress for a pattern
     */
    suspend fun updatePlayerProgress(
        patternId: String,
        score: Int,
        updateBest: Boolean = true
    ) {
        try {
            val storage = loadStorage()
            val current = storage.playerProgress[patternId]

            val updated = if (current != null) {
                current.copy(
                    personalBest = if (updateBest && score > current.personalBest) score else current.personalBest,
                    timesPlayed = current.timesPlayed + 1,
                    lastPlayedTimestamp = currentTimeMillis()
                )
            } else {
                PlayerProgress(
                    patternId = patternId,
                    personalBest = score,
                    timesPlayed = 1,
                    lastPlayedTimestamp = currentTimeMillis()
                )
            }

            val newProgress = storage.playerProgress.toMutableMap()
            newProgress[patternId] = updated

            val newStorage = storage.copy(playerProgress = newProgress)
            saveStorage(newStorage)
        } catch (e: Exception) {
            println("PatternRepository.updatePlayerProgress error: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Load storage from JSON file
     */
    private suspend fun loadStorage(): PatternStorage {
        val content = fileStorage.readFile(filename)
        return if (content != null) {
            json.decodeFromString<PatternStorage>(content)
        } else {
            PatternStorage()
        }
    }

    /**
     * Save storage to JSON file
     */
    private suspend fun saveStorage(storage: PatternStorage) {
        val content = json.encodeToString(storage)
        fileStorage.writeFile(filename, content)
    }
}
