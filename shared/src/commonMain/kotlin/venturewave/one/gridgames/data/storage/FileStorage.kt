package venturewave.one.gridgames.data.storage

/**
 * Platform-specific file storage interface
 */
expect class FileStorage() {
    /**
     * Read text content from a file
     * @param filename The name of the file
     * @return The file content, or null if file doesn't exist
     */
    suspend fun readFile(filename: String): String?

    /**
     * Write text content to a file
     * @param filename The name of the file
     * @param content The content to write
     */
    suspend fun writeFile(filename: String, content: String)

    /**
     * Check if a file exists
     * @param filename The name of the file
     * @return true if file exists, false otherwise
     */
    suspend fun fileExists(filename: String): Boolean

    /**
     * Delete a file
     * @param filename The name of the file
     */
    suspend fun deleteFile(filename: String)
}
