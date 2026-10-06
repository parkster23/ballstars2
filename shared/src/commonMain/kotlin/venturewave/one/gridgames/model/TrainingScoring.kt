package venturewave.one.gridgames.model

/**
 * Pure scoring math for Training Mode, shared by both platforms so Android
 * and iOS can't drift into two different formulas again.
 *
 * Rules:
 * - Every correct target hit scores POINTS_PER_HIT immediately.
 * - Completing a full pattern cycle (a "lap") with no wrong-target hit
 *   anywhere during it triples the points earned from that lap's hits -
 *   patternCompletionBonus() is the extra top-up needed on top of the 1x
 *   already paid out per-hit to reach that 3x total.
 * - Hitting any target other than the current one costs WRONG_TARGET_PENALTY
 *   immediately, and disqualifies the in-progress lap from the completion
 *   bonus (it still banks its per-hit points, just no multiplier).
 */

const val POINTS_PER_HIT = 10
const val WRONG_TARGET_PENALTY = 10
const val PATTERN_COMPLETE_MULTIPLIER = 3

/**
 * Extra points awarded when a lap completes clean. Per-hit points are
 * already paid out as each hit lands (1x), so this adds the remaining
 * (PATTERN_COMPLETE_MULTIPLIER - 1)x to bring that lap's total up to a full
 * 3x multiplier.
 */
fun patternCompletionBonus(hitsThisLap: Int): Int =
    hitsThisLap * POINTS_PER_HIT * (PATTERN_COMPLETE_MULTIPLIER - 1)
