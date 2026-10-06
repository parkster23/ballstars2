package venturewave.one.gridgames.data

/**
 * Tracks whether the player has tried to enter Training or Game Mode
 * without a calibrated grid at least once this session. Used so the Setup
 * Zone tile only starts pulsating reactively, after a blocked attempt -
 * not just because the grid happens to be unconfigured on first launch.
 * Simple in-memory singleton, same pattern as GridRepository.
 */
object SetupPromptState {
    var hasBeenPrompted: Boolean = false
}
