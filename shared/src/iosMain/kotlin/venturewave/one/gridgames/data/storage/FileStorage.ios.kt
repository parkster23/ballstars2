package venturewave.one.gridgames.data.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

/**
 * iOS implementation of FileStorage using the app's Documents directory.
 *
 * Reads/writes go through NSData rather than NSString's own
 * stringWithContentsOfFile/writeToFile factory methods - those class-side
 * factory methods didn't resolve cleanly through this project's Kotlin/Native
 * Objective-C interop (likely selector-overload disambiguation), whereas the
 * plain NSFileManager + NSData instance methods used here are unambiguous.
 * Kotlin's String and Foundation's NSString are toll-free bridged in
 * Kotlin/Native. The `as NSString`/`as String?` casts below are load-bearing
 * despite the compiler flagging them as suspicious ("can never succeed" /
 * "only succeeds when null") - removing them breaks compilation entirely
 * (tried it; the static checker is overly conservative about this specific
 * bridging case). Leave them in place.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class FileStorage {
    private fun documentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        return paths.firstOrNull() as? String ?: ""
    }

    private fun filePath(filename: String): String = documentsDirectory() + "/" + filename

    actual suspend fun readFile(filename: String): String? {
        return try {
            val path = filePath(filename)
            val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
            NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
        } catch (e: Exception) {
            println("FileStorage.readFile error: ${e.message}")
            null
        }
    }

    actual suspend fun writeFile(filename: String, content: String) {
        try {
            val path = filePath(filename)
            val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            NSFileManager.defaultManager.createFileAtPath(path, contents = data, attributes = null)
        } catch (e: Exception) {
            println("FileStorage.writeFile error: ${e.message}")
            throw e
        }
    }

    actual suspend fun fileExists(filename: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(filePath(filename))
    }

    actual suspend fun deleteFile(filename: String) {
        try {
            NSFileManager.defaultManager.removeItemAtPath(filePath(filename), error = null)
        } catch (e: Exception) {
            println("FileStorage.deleteFile error: ${e.message}")
        }
    }
}
