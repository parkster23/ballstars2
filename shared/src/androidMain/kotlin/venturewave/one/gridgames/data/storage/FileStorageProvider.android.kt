package venturewave.one.gridgames.data.storage

import android.content.Context

/**
 * Singleton provider for FileStorage on Android
 */
actual object FileStorageProvider {
    private var instance: FileStorage? = null

    fun initialize(context: Context) {
        if (instance == null) {
            instance = FileStorage().apply {
                initialize(context)
            }
        }
    }

    actual fun get(): FileStorage {
        return instance ?: throw IllegalStateException(
            "FileStorageProvider not initialized. Call initialize() first."
        )
    }
}
