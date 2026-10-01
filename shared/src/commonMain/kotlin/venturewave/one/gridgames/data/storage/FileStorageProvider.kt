package venturewave.one.gridgames.data.storage

/**
 * Platform-specific provider for FileStorage instance
 */
expect object FileStorageProvider {
    fun get(): FileStorage
}
