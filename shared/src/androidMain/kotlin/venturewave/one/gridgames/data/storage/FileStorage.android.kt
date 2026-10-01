package venturewave.one.gridgames.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of FileStorage using app's files directory
 */
actual class FileStorage {
    private lateinit var context: Context

    /**
     * Initialize with Android context
     * Must be called before using any file operations
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    private fun getFile(filename: String): File {
        return File(context.filesDir, filename)
    }

    actual suspend fun readFile(filename: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = getFile(filename)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            println("FileStorage.readFile error: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    actual suspend fun writeFile(filename: String, content: String) = withContext(Dispatchers.IO) {
        try {
            val file = getFile(filename)
            file.writeText(content)
        } catch (e: Exception) {
            println("FileStorage.writeFile error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    actual suspend fun fileExists(filename: String): Boolean = withContext(Dispatchers.IO) {
        getFile(filename).exists()
    }

    actual suspend fun deleteFile(filename: String): Unit = withContext(Dispatchers.IO) {
        try {
            getFile(filename).delete()
        } catch (e: Exception) {
            println("FileStorage.deleteFile error: ${e.message}")
            e.printStackTrace()
        }
    }
}
