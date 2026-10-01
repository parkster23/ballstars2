package venturewave.one.gridgames.database

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Realm entity representing player's personal best for a pattern
 */
class PlayerProgressEntity() : RealmObject {
    @PrimaryKey
    var patternId: String = ""
    var personalBest: Int = 0  // Best count achieved
    var lastPlayedTimestamp: Long = 0

    constructor(
        patternId: String,
        personalBest: Int,
        lastPlayedTimestamp: Long
    ) : this() {
        this.patternId = patternId
        this.personalBest = personalBest
        this.lastPlayedTimestamp = lastPlayedTimestamp
    }
}
