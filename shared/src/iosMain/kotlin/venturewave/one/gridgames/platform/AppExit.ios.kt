package venturewave.one.gridgames.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.exit

/**
 * iOS implementation of app exit functionality.
 *
 * Apple's HIG discourages apps from quitting themselves, but this mirrors
 * the Android actual's behavior (a hard process exit via exitProcess(0))
 * for parity, since exitApp() is wired to an explicit user-facing "Exit"
 * action elsewhere in the app (FloatingNavIcons), not something iOS does
 * automatically.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun exitApp() {
    exit(0)
}
