package venturewave.one.gridgames.viewmodels

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

/**
 * iOS-specific time implementation.
 *
 * Uses POSIX time() (whole seconds since epoch) rather than
 * NSDate().timeIntervalSince1970: that member is present in the Foundation
 * klib's selector metadata but doesn't resolve as a Kotlin property in this
 * project's iosMain compilation (cause unclear - not a code typo). POSIX
 * time() is a much smaller, unambiguous surface with no such binding
 * quirks. Sub-second precision (lost by the *1000 here) isn't needed for
 * this app's use of currentTimeMillis() - timestamp bookkeeping for player
 * progress (lastPlayedTimestamp), not high-resolution timing.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long {
    return time(null) * 1000L
}
