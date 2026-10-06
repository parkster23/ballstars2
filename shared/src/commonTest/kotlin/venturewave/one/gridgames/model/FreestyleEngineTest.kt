package venturewave.one.gridgames.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FreestyleEngineTest {

    // --- chainMultiplier ---

    @Test
    fun chainMultiplier_firstTrick_isX1() {
        assertEquals(1.0f, chainMultiplier(1, isRepeatOfPrevious = false))
    }

    @Test
    fun chainMultiplier_secondDistinctTrick_isX1point5() {
        assertEquals(1.5f, chainMultiplier(2, isRepeatOfPrevious = false))
    }

    @Test
    fun chainMultiplier_thirdDistinctTrick_isX2() {
        assertEquals(2.0f, chainMultiplier(3, isRepeatOfPrevious = false))
    }

    @Test
    fun chainMultiplier_fourthAndBeyondDistinctTrick_capsAtX3() {
        assertEquals(3.0f, chainMultiplier(4, isRepeatOfPrevious = false))
        assertEquals(3.0f, chainMultiplier(10, isRepeatOfPrevious = false))
    }

    @Test
    fun chainMultiplier_repeatOfPrevious_alwaysCollapsesToX1() {
        assertEquals(1.0f, chainMultiplier(2, isRepeatOfPrevious = true))
        assertEquals(1.0f, chainMultiplier(3, isRepeatOfPrevious = true))
        assertEquals(1.0f, chainMultiplier(8, isRepeatOfPrevious = true))
    }

    // --- trickPoints ---

    @Test
    fun trickPoints_difficulty1_firstTrick() {
        // 100 * 1 * 1.0 = 100
        assertEquals(100, trickPoints(difficultyLevel = 1, chainPosition = 1, isRepeatOfPrevious = false))
    }

    @Test
    fun trickPoints_difficulty2_thirdDistinctTrick() {
        // 100 * 2 * 2.0 = 400
        assertEquals(400, trickPoints(difficultyLevel = 2, chainPosition = 3, isRepeatOfPrevious = false))
    }

    @Test
    fun trickPoints_difficulty2_fourthPlusDistinctTrick_capMultiplier() {
        // 100 * 2 * 3.0 = 600
        assertEquals(600, trickPoints(difficultyLevel = 2, chainPosition = 4, isRepeatOfPrevious = false))
    }

    // --- matchTrick ---

    private val syntheticPatterns = listOf(
        TrainingPattern(
            id = "short_suffix",
            name = "Short Suffix",
            description = "test",
            targetSequence = listOf(5, 7, 4),
            difficultyLevel = 1,
            estimatedDuration = 5
        ),
        TrainingPattern(
            id = "long_overlapping",
            name = "Long Overlapping",
            description = "test",
            targetSequence = listOf(9, 5, 7, 4),
            difficultyLevel = 2,
            estimatedDuration = 8
        )
    )

    @Test
    fun matchTrick_longestOverlappingMatchWins() {
        // Buffer tail matches BOTH "short_suffix" ([5,7,4]) and
        // "long_overlapping" ([9,5,7,4]) simultaneously - the longer one
        // must win.
        val buffer = listOf(1, 9, 5, 7, 4)
        val matched = matchTrick(buffer, syntheticPatterns)
        assertEquals("long_overlapping", matched?.id)
    }

    @Test
    fun matchTrick_noOverlap_shorterPatternStillMatchesAlone() {
        val buffer = listOf(2, 5, 7, 4)
        val matched = matchTrick(buffer, syntheticPatterns)
        assertEquals("short_suffix", matched?.id)
    }

    @Test
    fun matchTrick_noMatch_returnsNull() {
        val buffer = listOf(1, 2, 3)
        assertNull(matchTrick(buffer, syntheticPatterns))
    }

    @Test
    fun matchTrick_emptyBuffer_returnsNull() {
        assertNull(matchTrick(emptyList(), syntheticPatterns))
    }

    // --- matchTrick gap tolerance (the real on-device failure this was added for) ---

    @Test
    fun matchTrick_toleratesStrayGrazeOnBothSidesOfARealStep() {
        // Real captured on-device sequence: deliberately traced Right Peak
        // (5,7,4,5) but the ball's straight-line path from 5 to 7 grazed
        // the geometrically-between box 4 twice (once on the way to 7,
        // once on the way back to the final 5). Must still recognize it.
        val buffer = listOf(5, 4, 7, 4, 5)
        val matched = matchTrick(buffer, listOf(rightPeak))
        assertEquals("right_peak", matched?.id)
    }

    @Test
    fun matchTrick_penguinFeetCenterPivot_matchesWithZeroGapNeeded() {
        // Box 5 sits geometrically between 4 and 6 (the exact transit cell
        // for that pair), but it's also a genuine required step in Penguin
        // Feet - must match on its own merits with no tolerance invoked,
        // confirming gap tolerance never swallows a legitimate pivot hit.
        val buffer = listOf(5, 4, 5, 6, 5)
        val matched = matchTrick(buffer, listOf(penguinFeet))
        assertEquals("penguin_feet", matched?.id)
    }

    @Test
    fun matchTrick_moreThanMaxGapStrayEntries_failsToMatch() {
        // Three stray entries between the real "5" and "7" steps exceeds
        // MAX_TRANSIT_GAP (2) - tolerance has a real ceiling.
        val buffer = listOf(5, 1, 2, 3, 7, 4, 5)
        assertNull(matchTrick(buffer, listOf(rightPeak)))
    }

    @Test
    fun matchTrick_abandonedAttemptDoesNotInterfereWithALaterDifferentPattern() {
        // Player starts Right Peak (5,7,...) then gives up after "5,7",
        // wanders to an irrelevant box (2), then cleanly traces Dap Up
        // (1,5,3,5,1) from scratch. The abandoned "5,7" must not cause a
        // false Right Peak match (no 4 ever appears) and must not prevent
        // Dap Up from matching correctly off the true buffer tail.
        val buffer = listOf(5, 7, 2, 1, 5, 3, 5, 1)
        val matched = matchTrick(buffer, listOf(rightPeak, dapUp))
        assertEquals("dap_up", matched?.id)
    }

    @Test
    fun matchTrick_newPatternFromADifferentStartingBoxStillMatches() {
        // Not all patterns start/end on the center: right_drift ends on 1.
        // Player finishes it, moves to 5 (not continuing from the retained
        // "1" pivot), and traces left_cruyff (5,6,3,1,5) from there. The
        // leftover pivot "1" at the front, plus left_cruyff's own "1" near
        // the end, must not be confused with each other.
        val buffer = listOf(1, 5, 6, 3, 1, 5)
        val matched = matchTrick(buffer, listOf(rightDrift, leftCruyff))
        assertEquals("left_cruyff", matched?.id)
    }

    // --- FreestyleChainState ---

    private val rightPeak = TrainingPattern("right_peak", "Right Peak", "", listOf(5, 7, 4, 5), 1, 8)
    private val leftPeak = TrainingPattern("left_peak", "Left Peak", "", listOf(5, 9, 6, 5), 1, 8)
    private val vs = TrainingPattern("vs", "V's", "", listOf(5, 9, 5, 7, 5), 2, 10)
    private val penguinFeet = TrainingPattern("penguin_feet", "Penguin Feet", "", listOf(5, 4, 5, 6, 5), 2, 10)
    private val dapUp = TrainingPattern("dap_up", "Dap Up", "", listOf(1, 5, 3, 5, 1), 2, 10)
    private val rightDrift = TrainingPattern("right_drift", "Right Drift", "", listOf(1, 3, 9, 1), 1, 8)
    private val leftCruyff = TrainingPattern("left_cruyff", "Left Cruyff", "", listOf(5, 6, 3, 1, 5), 2, 10)
    private val patterns = listOf(rightPeak, leftPeak, vs)

    @Test
    fun onBoxEntered_noMatchYet_returnsNull() {
        val state = FreestyleChainState(patterns)
        assertNull(state.onBoxEntered(1))
        assertNull(state.onBoxEntered(2))
        assertEquals(0, state.chainLength)
    }

    @Test
    fun onBoxEntered_landsFirstTrick_atPositionOneFullMultiplier() {
        val state = FreestyleChainState(patterns)
        listOf(5, 7, 4).forEach { state.onBoxEntered(it) }
        val landed = state.onBoxEntered(5) // completes Right Peak [5,7,4,5]

        assertEquals("right_peak", landed?.pattern?.id)
        assertEquals(1, landed?.chainPosition)
        assertEquals(100, landed?.points) // 100 * difficulty(1) * x1
        assertEquals(1, state.chainLength)
    }

    @Test
    fun onBoxEntered_pivotRetained_allowsImmediateChainingThroughSharedCenterBox() {
        // Right Peak [5,7,4,5] then, continuing straight through the
        // shared pivot box 5, V's [5,9,5,7,5] - without ever "leaving and
        // re-entering" box 5 in between. This is exactly the bug fixed by
        // retaining the pivot digit instead of fully clearing the buffer.
        val state = FreestyleChainState(patterns)
        listOf(5, 7, 4, 5).forEach { state.onBoxEntered(it) }
        assertEquals(1, state.chainLength)

        val secondTrick = listOf(9, 5, 7, 5).map { state.onBoxEntered(it) }.lastOrNull { it != null }
        assertEquals("vs", secondTrick?.pattern?.id)
        assertEquals(2, state.chainLength)
    }

    @Test
    fun onBoxEntered_repeatOfPreviousTrick_forcesMultiplierToX1() {
        val state = FreestyleChainState(patterns)
        listOf(5, 7, 4, 5).forEach { state.onBoxEntered(it) } // Right Peak (1st)
        // Right Peak again immediately (repeat)
        val repeat = listOf(7, 4, 5).map { state.onBoxEntered(it) }.lastOrNull { it != null }
        assertEquals("right_peak", repeat?.pattern?.id)
        assertEquals(100, repeat?.points) // still base x1, not scaled by chain position
    }

    @Test
    fun onBoxEntered_threeDistinctTricksInARow_triggersFlowBonus() {
        val state = FreestyleChainState(patterns)
        val events = mutableListOf<TrickLanded?>()
        // Right Peak, Left Peak, V's - three distinct patterns landed back to back.
        listOf(5, 7, 4, 5, 9, 6, 5, 9, 5, 7, 5).forEach { events.add(state.onBoxEntered(it)) }

        val landedEvents = events.filterNotNull()
        assertEquals(3, landedEvents.size)
        assertTrue(landedEvents.last().flowBonusPoints > 0)
    }

    @Test
    fun onBoxEntered_milestonesFireExactlyOncePerThreshold() {
        val state = FreestyleChainState(patterns)
        val milestones = mutableListOf<Int>()
        // Alternate Right Peak / Left Peak to build an 8-long chain of
        // distinct-from-previous tricks without tripping the repeat rule.
        val sequence = listOf(5, 7, 4, 5, 9, 6, 5, 7, 4, 5, 9, 6, 5, 7, 4, 5, 9, 6, 5, 7, 4, 5, 9, 6, 5)
        sequence.forEach { box ->
            state.onBoxEntered(box)?.milestoneReached?.let { milestones.add(it) }
        }
        assertEquals(listOf(3, 5, 8), milestones)
    }

    @Test
    fun bankChain_onEmptyChain_returnsNull() {
        val state = FreestyleChainState(patterns)
        assertNull(state.bankChain())
    }

    @Test
    fun bankChain_resetsStateForNextChain() {
        val state = FreestyleChainState(patterns)
        listOf(5, 7, 4, 5).forEach { state.onBoxEntered(it) }
        val banked = state.bankChain()

        assertEquals(1, banked?.chainLength)
        assertEquals(listOf("Right Peak"), banked?.landedNames)
        assertEquals(0, state.chainLength)
        assertEquals(0, state.currentChainScore)

        // A fresh chain afterwards starts clean, unaffected by the banked one.
        assertNull(state.onBoxEntered(1))
    }

    @Test
    fun patternCompletionCounts_survivesBankChainAndTalliesAcrossTheWholeSession() {
        val state = FreestyleChainState(patterns)

        // Land Right Peak, then (without banking) continue the chain
        // straight into Left Peak via the shared pivot.
        listOf(5, 7, 4, 5).forEach { state.onBoxEntered(it) }
        listOf(9, 6, 5).forEach { state.onBoxEntered(it) }
        assertEquals(mapOf("Right Peak" to 1, "Left Peak" to 1), state.patternCompletionCounts)

        // Banking the chain must not wipe the session-wide tally.
        state.bankChain()
        assertEquals(mapOf("Right Peak" to 1, "Left Peak" to 1), state.patternCompletionCounts)

        // Landing Right Peak again (a fresh chain) increments its count
        // rather than overwriting it.
        listOf(5, 7, 4, 5).forEach { state.onBoxEntered(it) }
        assertEquals(mapOf("Right Peak" to 2, "Left Peak" to 1), state.patternCompletionCounts)
    }
}
