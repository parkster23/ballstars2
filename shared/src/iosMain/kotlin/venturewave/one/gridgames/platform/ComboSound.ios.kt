package venturewave.one.gridgames.platform

import platform.AudioToolbox.AudioServicesPlaySystemSound

/**
 * Zero-bundled-asset placeholder stingers using built-in iOS system sound
 * IDs. Note: AudioServicesPlaySystemSound (unlike AVAudioPlayer with a
 * category override) respects the device's silent switch, so no sound on
 * a muted device is expected behavior, not a bug. Stand-in for real custom
 * audio assets.
 */
actual fun playComboSound(milestone: ComboMilestone) {
    val soundId: UInt = when (milestone) {
        ComboMilestone.CHAIN_3 -> 1103u // "Tink"
        ComboMilestone.CHAIN_5 -> 1104u // "Tock"
        ComboMilestone.CHAIN_8_PLUS -> 1057u // "Tweet Sent"-style chime
        ComboMilestone.FLOW_BONUS -> 1025u // distinct fanfare-ish system sound
    }
    AudioServicesPlaySystemSound(soundId)
}

actual fun playCountdownBeep() {
    AudioServicesPlaySystemSound(1105u) // short "Tock"-style tick
}

actual fun playGoSound() {
    AudioServicesPlaySystemSound(1016u) // distinct longer confirm-style tone
}
