package venturewave.one.gridgames.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import venturewave.one.gridgames.model.Training
import venturewave.one.gridgames.model.TargetBox
import venturewave.one.gridgames.model.logInfo

/**
 * ViewModel for ball position tracking and scoring logic
 * Core scoring algorithm ported exactly from production - already debugged through
 * multiple real production issues (cooldown timing, targetHitCounter>0 fix, null-safe
 * path handling)
 */
class BallPositionViewModel {

    /**
     * Data class representing a detected ball position
     */
    data class BallPositionData(
        val left: Float = 0f,
        val top: Float = 0f,
        val right: Float = 0f,
        val bottom: Float = 0f,
        val timestamp: Long,
        val confidence: Float
    ) {
        /**
         * Calculate the center point of the bounding box
         */
        fun calculateCenter(): Pair<Float, Float> {
            val centerX = (left + right) / 2
            val centerY = (top + bottom) / 2
            return Pair(centerX, centerY)
        }
    }

    private val _liveScore = MutableStateFlow(0)
    val liveScore: StateFlow<Int> = _liveScore.asStateFlow()

    private val _hitScore = MutableStateFlow(0)
    val hitScore: StateFlow<Int> = _hitScore.asStateFlow()

    var hitTargetCounter: Int = 0

    /**
     * Minimum real time between one full lap completing and the next being allowed
     * CRITICAL: This is a time gate, not an "observe ball leaving box" gate - the
     * detection stream isn't dense enough to guarantee seeing every frame. Motion
     * blur + confidence threshold drops can produce consecutive frames with no
     * detection (production insight from BSDiag logs)
     */
    private var lastScoreTimestamp: Long = 0L
    private val scoreCooldownMs: Long = 600L

    /**
     * Reset all scores and counters
     */
    fun resetScores() {
        _liveScore.value = 0
        _hitScore.value = 0
        hitTargetCounter = 0
        lastScoreTimestamp = 0L
    }

    /**
     * Update the live score
     */
    fun updateScore(newScore: Int) {
        _liveScore.value = newScore
    }

    /**
     * Update the hit score
     */
    fun updateHitScore(newScore: Int) {
        _hitScore.value = newScore
    }

    /**
     * Check if ball center is within target box (with tolerance)
     */
    fun isBallInTargetBox(
        ballPosition: BallPositionData,
        targetBox: TargetBox,
        tolerance: Int
    ): Boolean {
        val ballCenter = ballPosition.calculateCenter()
        val targetBoxLeft = targetBox.left - tolerance
        val targetBoxTop = targetBox.top - tolerance
        val targetBoxRight = targetBox.right + tolerance
        val targetBoxBottom = targetBox.bottom + tolerance

        val isInTargetBox = ballCenter.first >= targetBoxLeft &&
                ballCenter.first <= targetBoxRight &&
                ballCenter.second >= targetBoxTop &&
                ballCenter.second <= targetBoxBottom

        logInfo(
            "BSDiag",
            "HITCHECK ts=${ballPosition.timestamp} targetSeq=${targetBox.sequence} targetLabel=${targetBox.detectedLabel} " +
                    "targetL=${targetBox.left} targetT=${targetBox.top} targetR=${targetBox.right} targetB=${targetBox.bottom} tol=$tolerance " +
                    "ballCx=${ballCenter.first} ballCy=${ballCenter.second} inBox=$isInTargetBox"
        )

        return isInTargetBox
    }

    /**
     * Main scoring logic - check if ball hit target and update score
     * PRESERVED EXACTLY from production with all critical fixes:
     * - Cooldown time-based gating
     * - targetHitCounter > 0 guard
     * - pathOrNull() for race condition safety
     */
    fun hitTarget(ball: BallPositionData, tolerance: Int) {
        val currentTarget = Training.findNextTargetBox()

        if (currentTarget != null) {
            if (isBallInTargetBox(ball, currentTarget, tolerance)) {

                // currentTarget.hit prevents re-counting same box within a lap
                // findNextTargetBox()'s !it.hit filter ensures this until clearAllHits()
                hitTargetCounter++
                updateHitScore(hitTargetCounter)
                currentTarget.hit = true

                // CRITICAL: Use pathOrNull() not getPath() - concurrent trainingBuilder()
                // calls can null path out mid-frame (production crash fix)
                val currentPath = Training.pathOrNull()
                if (currentPath != null) {
                    if (currentTarget.endBox) {
                        currentPath.targetToHitNumber = 1
                    } else {
                        currentPath.targetToHitNumber++
                    }
                    logInfo(
                        "BSDiag",
                        "HIT ts=${ball.timestamp} targetSeq=${currentTarget.sequence} targetLabel=${currentTarget.detectedLabel} " +
                                "hitTargetCounter=$hitTargetCounter targetHitCounter=${Training.targetHitCounter} " +
                                "nextTargetToHit=${currentPath.targetToHitNumber}"
                    )
                } else {
                    logInfo(
                        "BSDiag",
                        "HIT_SKIPPED_PATH_NULL ts=${ball.timestamp} targetSeq=${currentTarget.sequence} - training likely being rebuilt concurrently"
                    )
                }
            }
        } else {
            logInfo(
                "BSDiag",
                "HITCHECK ts=${ball.timestamp} targetSeq=NONE hitTargetCounter=$hitTargetCounter"
            )
        }

        // CRITICAL: targetHitCounter > 0 guard prevents scoring during async training load
        // 0 == 0 would be trivially true before data arrives (production bug - 65 bogus
        // scores in one session, all before TRAINING_BUILT logged)
        val now = currentTimeMillis()
        if (hitTargetCounter == Training.targetHitCounter && Training.targetHitCounter > 0) {
            val sinceLastScoreMs = now - lastScoreTimestamp
            if (sinceLastScoreMs >= scoreCooldownMs) {
                logInfo(
                    "BSDiag",
                    "SCORE ts=$now newScore=${_liveScore.value + 1} sinceLastScoreMs=$sinceLastScoreMs " +
                            "hitTargetCounter=$hitTargetCounter targetHitCounter=${Training.targetHitCounter}"
                )
                _liveScore.value++
                lastScoreTimestamp = now
                hitTargetCounter = 0
                Training.clearAllHits()

                // Same pathOrNull() safety for concurrent training reselection
                Training.pathOrNull()?.targetToHitNumber = 1
            } else {
                logInfo(
                    "BSDiag",
                    "SCORE_BLOCKED_BY_COOLDOWN ts=$now sinceLastScoreMs=$sinceLastScoreMs"
                )
            }
        }
    }
}

/**
 * Platform-agnostic current time function
 * Actual implementations per platform
 */
expect fun currentTimeMillis(): Long
