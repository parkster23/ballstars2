package venturewave.one.gridgames.database

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.annotations.PrimaryKey

/**
 * Realm entity representing a training or game pattern
 */
class PatternEntity() : RealmObject {
    @PrimaryKey
    var id: String = ""
    var name: String = ""
    var description: String = ""
    var targetSequence: RealmList<Int> = realmListOf()
    var difficultyLevel: Int = 1
    var estimatedDuration: Int = 0

    constructor(
        id: String,
        name: String,
        description: String,
        targetSequence: RealmList<Int>,
        difficultyLevel: Int,
        estimatedDuration: Int
    ) : this() {
        this.id = id
        this.name = name
        this.description = description
        this.targetSequence = targetSequence
        this.difficultyLevel = difficultyLevel
        this.estimatedDuration = estimatedDuration
    }
}
