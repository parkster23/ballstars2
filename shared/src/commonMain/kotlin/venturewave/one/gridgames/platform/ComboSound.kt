package venturewave.one.gridgames.platform

/**
 * Combo-highlight milestones in Freestyle Gameplay Mode that get an
 * escalating audio stinger (sound effects only, deliberately no haptics -
 * a vibration buzz would physically move the phone mid-shot and throw off
 * ball tracking).
 */
enum class ComboMilestone { CHAIN_3, CHAIN_5, CHAIN_8_PLUS, FLOW_BONUS }

/**
 * Plays a short, zero-bundled-asset placeholder stinger for the given
 * milestone (system tones on both platforms). Stands in for real custom
 * "Block Blast"-style escalating chime audio - swapping that in later only
 * touches the two platform `actual` bodies, not call sites.
 */
expect fun playComboSound(milestone: ComboMilestone)

/** Short tick played once per second during the 10->1 pre-game countdown, both Training and Freestyle modes. */
expect fun playCountdownBeep()

/** Distinct sound played once when the countdown hits "GO!" and play begins. */
expect fun playGoSound()
