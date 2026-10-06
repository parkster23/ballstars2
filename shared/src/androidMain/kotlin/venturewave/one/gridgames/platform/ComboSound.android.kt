package venturewave.one.gridgames.platform

import android.media.AudioManager
import android.media.ToneGenerator

// Kept alive for the process lifetime (a ToneGenerator instance is cheap
// and this is a plain stateless top-level function, not scoped to a
// Composable, so there's no natural per-screen handle to release it
// through - this is simpler and avoids audio hiccups from repeatedly
// creating/releasing an instance per stinger).
private val toneGenerator: ToneGenerator by lazy {
    ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
}

/**
 * Zero-bundled-asset placeholder stingers: escalating DTMF tones for the
 * chain-length ladder (they do rise in pitch 1 -> 5 -> 9), a distinct
 * "prop beep" for the Flow Bonus so it's audibly different from the
 * length-based ladder. Stand-in for real custom audio assets.
 */
actual fun playComboSound(milestone: ComboMilestone) {
    val tone = when (milestone) {
        ComboMilestone.CHAIN_3 -> ToneGenerator.TONE_DTMF_1
        ComboMilestone.CHAIN_5 -> ToneGenerator.TONE_DTMF_5
        ComboMilestone.CHAIN_8_PLUS -> ToneGenerator.TONE_DTMF_9
        ComboMilestone.FLOW_BONUS -> ToneGenerator.TONE_PROP_BEEP2
    }
    toneGenerator.startTone(tone, 150)
}

actual fun playCountdownBeep() {
    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
}

actual fun playGoSound() {
    toneGenerator.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 300)
}
