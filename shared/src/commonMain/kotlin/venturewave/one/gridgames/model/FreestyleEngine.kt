package venturewave.one.gridgames.model

import kotlin.math.roundToInt

/**
 * Pure scoring/recognition logic for Freestyle Gameplay Mode (no single
 * pre-selected pattern - the player moves freely around the grid and the
 * app recognizes whenever any of the known patterns is completed, chaining
 * recognized patterns into combos). See GAME_MECHANICS_PROPOSAL.md section 2.
 *
 * Kept commonMain/Compose-free and unit-testable so Android and iOS share
 * one scoring formula instead of drifting into two, as the per-platform
 * Training Mode formulas once did (see SCORING_MECHANISM.md history).
 */

const val TRICK_BASE_POINTS = 100 // multiplied by difficultyLevel
const val FLOW_BONUS = 250
const val CHAIN_TIMEOUT_MS = 3000L

/**
 * On a 3x3 grid, a straight path between any two boxes can graze at most
 * two others in between (usually just one) - e.g. moving center(5) to
 * bottom-left(7) grazes middle-left(4), which sits geometrically between
 * them. matchTrick() tolerates up to this many non-matching stray entries
 * between each of a pattern's required steps, rather than requiring an
 * exact contiguous sequence.
 */
const val MAX_TRANSIT_GAP = 2

// A 5-step pattern with MAX_TRANSIT_GAP stray entries tolerated between
// each of its 4 transitions could need up to 5 + 4*MAX_TRANSIT_GAP = 13
// raw entries of lookback to find a full match - leave headroom above that.
const val CHAIN_BUFFER_SIZE = 16

/**
 * 1st trick in a chain = x1, 2nd (different from the last) = x1.5, 3rd
 * (different) = x2, 4th+ (different) = x3 (cap). Repeating the
 * immediately-preceding trick collapses the multiplier back to x1
 * regardless of chain length.
 */
fun chainMultiplier(chainPosition: Int, isRepeatOfPrevious: Boolean): Float = when {
    isRepeatOfPrevious -> 1.0f
    chainPosition <= 1 -> 1.0f
    chainPosition == 2 -> 1.5f
    chainPosition == 3 -> 2.0f
    else -> 3.0f
}

fun trickPoints(difficultyLevel: Int, chainPosition: Int, isRepeatOfPrevious: Boolean): Int =
    (TRICK_BASE_POINTS * difficultyLevel * chainMultiplier(chainPosition, isRepeatOfPrevious)).roundToInt()

/**
 * Checks whether `sequence` matches the tail of `buffer`, allowing up to
 * [maxGap] non-matching stray entries to be skipped between each of
 * `sequence`'s required steps (see MAX_TRANSIT_GAP). The match must end
 * exactly at the buffer's last element - a trick is only recognized the
 * instant its final step actually lands, not retroactively once stale.
 */
private fun matchesWithGapTolerance(buffer: List<Int>, sequence: List<Int>, maxGap: Int): Boolean {
    if (buffer.isEmpty() || sequence.isEmpty()) return false
    if (buffer.last() != sequence.last()) return false

    var bufferIdx = buffer.size - 2
    var seqIdx = sequence.size - 2
    while (seqIdx >= 0) {
        var gap = 0
        var found = false
        while (bufferIdx >= 0 && gap <= maxGap) {
            if (buffer[bufferIdx] == sequence[seqIdx]) {
                found = true
                break
            }
            bufferIdx--
            gap++
        }
        if (!found) return false
        bufferIdx--
        seqIdx--
    }
    return true
}

/**
 * Checks whether the tail of `buffer` (raw 1-9 target-hit history, oldest
 * first) matches any known pattern's targetSequence, within
 * [maxGap]/MAX_TRANSIT_GAP stray-entry tolerance (see
 * matchesWithGapTolerance). When more than one pattern matches
 * simultaneously, the longest matching sequence wins - well-defined (a
 * shorter match is necessarily a suffix of a longer one sharing the same
 * trailing elements) and currently dormant since none of the 13 bundled
 * patterns overlap today, but real once custom patterns can exist.
 */
fun matchTrick(
    buffer: List<Int>,
    patterns: List<TrainingPattern> = TrainingPattern.getBundledPatterns(),
    maxGap: Int = MAX_TRANSIT_GAP
): TrainingPattern? =
    patterns
        .filter { p -> matchesWithGapTolerance(buffer, p.targetSequence, maxGap) }
        .maxByOrNull { it.targetSequence.size }

/** Result of a single newly-landed trick, returned by [FreestyleChainState.onBoxEntered]. */
data class TrickLanded(
    val pattern: TrainingPattern,
    val points: Int,
    val chainPosition: Int,
    val flowBonusPoints: Int, // 0 unless this trick completed a 3-distinct window
    val milestoneReached: Int? // 3, 5, 8, or null - fires once per chain per threshold
)

/** Result of banking a completed (or forcibly ended) chain. */
data class ChainBanked(
    val chainScore: Int,
    val chainLength: Int,
    val landedNames: List<String>
)

/**
 * Tracks one freeplay session's in-progress chain: the rolling hit buffer,
 * recognition against the known pattern library, and the chain/combo
 * scoring bookkeeping (multiplier position, flow-bonus window, milestones).
 * No Compose/platform dependency - plain state, driven by the screen.
 */
class FreestyleChainState(private val patterns: List<TrainingPattern> = TrainingPattern.getBundledPatterns()) {
    private var hitBuffer: List<Int> = emptyList()
    var chainLength = 0
        private set
    var currentChainScore = 0
        private set
    private var lastLandedPatternId: String? = null
    private var recentDistinctIds: List<String> = emptyList()
    private var landedNamesThisChain: List<String> = emptyList()
    private var highestMilestoneReached = 0

    // Session-wide tally by pattern name - unlike landedNamesThisChain,
    // this does NOT reset on bankChain(); it's the running total shown in
    // the end-of-session summary ("type and number of patterns completed").
    private val completionCountsByName = mutableMapOf<String, Int>()
    val patternCompletionCounts: Map<String, Int> get() = completionCountsByName

    /**
     * Call when the ball is confirmed to have newly entered a target box
     * (box number 1-9). Returns the landed trick if this completed a known
     * pattern, or null if the buffer doesn't match anything yet.
     */
    fun onBoxEntered(boxNumber: Int): TrickLanded? {
        hitBuffer = (hitBuffer + boxNumber).takeLast(CHAIN_BUFFER_SIZE)
        val matched = matchTrick(hitBuffer, patterns) ?: return null

        val isRepeat = matched.id == lastLandedPatternId
        chainLength++
        val points = trickPoints(matched.difficultyLevel, chainLength, isRepeat)
        currentChainScore += points

        recentDistinctIds = (recentDistinctIds + matched.id).takeLast(3)
        val flowBonus = recentDistinctIds.size == 3 && recentDistinctIds.toSet().size == 3
        val flowPoints = if (flowBonus) FLOW_BONUS else 0
        currentChainScore += flowPoints

        landedNamesThisChain = landedNamesThisChain + matched.name
        lastLandedPatternId = matched.id
        completionCountsByName[matched.name] = (completionCountsByName[matched.name] ?: 0) + 1

        val milestone = when {
            chainLength >= 8 && highestMilestoneReached < 8 -> 8
            chainLength >= 5 && highestMilestoneReached < 5 -> 5
            chainLength >= 3 && highestMilestoneReached < 3 -> 3
            else -> null
        }
        if (milestone != null) highestMilestoneReached = milestone

        // Retain the pivot box (last element) instead of fully clearing:
        // every bundled pattern starts and ends on the same box within
        // itself (e.g. Right Peak [5,7,4,5], V's [5,9,5,7,5]), so a
        // continuous freestyle chain flows through that shared center
        // pivot without an artificial "leave and re-enter" transition.
        // Fully clearing here would permanently lose that leading digit
        // and make most chained patterns unmatchable.
        hitBuffer = hitBuffer.takeLast(1)

        return TrickLanded(matched, points, chainLength, flowPoints, milestone)
    }

    /** Banks whatever chain is currently in progress (no-op if none is). */
    fun bankChain(): ChainBanked? {
        if (chainLength == 0) return null
        val banked = ChainBanked(currentChainScore, chainLength, landedNamesThisChain)
        chainLength = 0
        currentChainScore = 0
        lastLandedPatternId = null
        recentDistinctIds = emptyList()
        landedNamesThisChain = emptyList()
        highestMilestoneReached = 0
        hitBuffer = emptyList()
        return banked
    }
}

/** Summary handed back when a Freestyle session ends. */
data class FreestyleResult(
    val sessionScore: Int,
    val longestChain: Int,
    val totalTricksLanded: Int,
    val totalTime: Long,
    val patternBreakdown: List<Pair<String, Int>> = emptyList() // pattern name to times completed, most-completed first
)
