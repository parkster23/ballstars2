package venturewave.one.gridgames.database

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

/**
 * Provides configured Realm instance for the application
 */
object RealmConfig {
    private var realmInstance: Realm? = null

    fun getRealm(): Realm {
        println("RealmConfig.getRealm(): Checking realm instance...")
        if (realmInstance == null || realmInstance?.isClosed() == true) {
            println("RealmConfig.getRealm(): Creating new Realm instance")
            try {
                val config = RealmConfiguration.Builder(
                    schema = setOf(
                        PatternEntity::class,
                        PlayerProgressEntity::class
                    )
                )
                    .name("ballstars.realm")
                    .schemaVersion(1)
                    .build()

                println("RealmConfig.getRealm(): Opening Realm with config")
                realmInstance = Realm.open(config)
                println("RealmConfig.getRealm(): Realm opened successfully")
            } catch (e: Exception) {
                println("RealmConfig.getRealm(): ERROR opening Realm: ${e.message}")
                e.printStackTrace()
                throw e
            }
        } else {
            println("RealmConfig.getRealm(): Returning existing Realm instance")
        }
        return realmInstance!!
    }

    fun close() {
        realmInstance?.close()
        realmInstance = null
    }
}
