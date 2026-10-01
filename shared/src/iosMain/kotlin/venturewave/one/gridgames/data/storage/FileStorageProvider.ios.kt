package venturewave.one.gridgames.data.storage

/**
 * Singleton provider for FileStorage on iOS.
 *
 * Unlike Android, iOS's FileStorage needs no platform Context to locate its
 * storage directory (NSSearchPathForDirectoriesInDomains needs none), so
 * there's no separate initialize() step here - the instance is always ready.
 */
actual object FileStorageProvider {
    private val instance = FileStorage()

    actual fun get(): FileStorage = instance
}
