package venturewave.one.gridgames.model

import android.util.Log

/**
 * Android-specific logging implementation
 * Falls back to println in unit test environment where Log is not mocked
 */
actual fun logInfo(tag: String, message: String) {
    try {
        Log.i(tag, message)
    } catch (e: RuntimeException) {
        // Log not available in unit tests - fall back to println
        if (e.message?.contains("not mocked") == true) {
            println("[$tag] $message")
        } else {
            throw e
        }
    }
}
