package venturewave.one.gridgames.data.repository

import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.toRealmList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import venturewave.one.gridgames.database.PatternEntity
import venturewave.one.gridgames.database.PlayerProgressEntity
import venturewave.one.gridgames.database.RealmConfig
import venturewave.one.gridgames.model.TrainingPattern
import venturewave.one.gridgames.viewmodels.currentTimeMillis

/**
 * Repository for managing training patterns with Realm persistence
 */
class TrainingPatternRepository {
    private val realm: Realm = RealmConfig.getRealm()

    /**
     * Initialize database with bundled patterns if empty
     */
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.Default) {
        val count = realm.query<PatternEntity>().count().find()

        if (count == 0L) {
            val bundledPatterns = TrainingPattern.getBundledPatterns()

            realm.write {
                bundledPatterns.forEach { pattern ->
                    val entity = PatternEntity().apply {
                        id = pattern.id
                        name = pattern.name
                        description = pattern.description
                        targetSequence = pattern.targetSequence.toRealmList()
                        difficultyLevel = pattern.difficultyLevel
                        estimatedDuration = pattern.estimatedDuration
                    }
                    copyToRealm(entity)
                }
            }
        }
    }

    /**
     * Get all training patterns from database
     */
    suspend fun getAllPatterns(): List<TrainingPattern> = withContext(Dispatchers.Default) {
        val entities = realm.query<PatternEntity>().find()
        entities.map { it.toTrainingPattern() }
    }

    /**
     * Get personal best for a specific pattern
     */
    suspend fun getPersonalBest(patternId: String): Int = withContext(Dispatchers.Default) {
        val progress = realm.query<PlayerProgressEntity>("patternId == $0", patternId).first().find()
        progress?.personalBest ?: 0
    }

    /**
     * Update personal best for a pattern
     */
    suspend fun updatePersonalBest(patternId: String, score: Int) = withContext(Dispatchers.Default) {
        realm.write {
            val progress = query<PlayerProgressEntity>("patternId == $0", patternId).first().find()

            if (progress != null) {
                // Update existing record
                if (score > progress.personalBest) {
                    progress.personalBest = score
                    progress.lastPlayedTimestamp = currentTimeMillis()
                }
            } else {
                // Create new progress record
                val newProgress = PlayerProgressEntity().apply {
                    this.patternId = patternId
                    this.personalBest = score
                    this.lastPlayedTimestamp = currentTimeMillis()
                }
                copyToRealm(newProgress)
            }
        }
    }

    /**
     * Convert Realm entity to domain model
     */
    private fun PatternEntity.toTrainingPattern(): TrainingPattern {
        return TrainingPattern(
            id = this.id,
            name = this.name,
            description = this.description,
            targetSequence = this.targetSequence.toList(),
            difficultyLevel = this.difficultyLevel,
            estimatedDuration = this.estimatedDuration
        )
    }
}
