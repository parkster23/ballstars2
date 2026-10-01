package venturewave.one.gridgames.viewmodels

import venturewave.one.gridgames.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit test for scoring logic
 * Verification: Feed hitTarget() a scripted sequence of fake ball positions
 * matching a known training pattern and assert score increments exactly once
 * per full pattern completion (matches BSDiag HIT/SCORE log behavior)
 */
class BallPositionViewModelTest {

    @Test
    fun `scoring advances exactly once per pattern completion`() {
        // Setup: Initialize training grid with test coordinates
        setupTestGrid()

        // Create ViewModel instance
        val viewModel = BallPositionViewModel()

        // Load "Simple Triangle" training (3 targets)
        Training.trainingBuilder("Simple Triangle")

        // Verify training loaded correctly
        assertEquals(3, Training.targetHitCounter, "Training should have 3 targets")
        assertEquals(3, Training.targetBoxes?.size, "Training should have 3 boxes")

        // Reset scores
        viewModel.resetScores()

        // Simulate ball positions hitting each target in sequence
        // Target 1: top-left (100, 100, 200, 200)
        val ball1 = BallPositionViewModel.BallPositionData(
            left = 140f,
            top = 140f,
            right = 160f,
            bottom = 160f,
            timestamp = 1000L,
            confidence = 0.9f
        )
        viewModel.hitTarget(ball1, tolerance = 20)

        // After first hit: hitTargetCounter should be 1, but score still 0
        assertEquals(1, viewModel.hitTargetCounter, "Should have hit 1 target")
        assertEquals(0, viewModel.liveScore.value, "Score should still be 0 after 1/3 targets")

        // Target 2: top-right (700, 100, 800, 200)
        val ball2 = BallPositionViewModel.BallPositionData(
            left = 740f,
            top = 140f,
            right = 760f,
            bottom = 160f,
            timestamp = 2000L,
            confidence = 0.9f
        )
        viewModel.hitTarget(ball2, tolerance = 20)

        // After second hit: hitTargetCounter should be 2, but score still 0
        assertEquals(2, viewModel.hitTargetCounter, "Should have hit 2 targets")
        assertEquals(0, viewModel.liveScore.value, "Score should still be 0 after 2/3 targets")

        // Target 3: centre-bottom (400, 700, 500, 800) - this is the end box
        val ball3 = BallPositionViewModel.BallPositionData(
            left = 440f,
            top = 740f,
            right = 460f,
            bottom = 760f,
            timestamp = 3000L,
            confidence = 0.9f
        )
        viewModel.hitTarget(ball3, tolerance = 20)

        // After completing the pattern: score should increment to 1
        assertEquals(1, viewModel.liveScore.value, "Score should be 1 after completing pattern")
        assertEquals(0, viewModel.hitTargetCounter, "hitTargetCounter should reset after scoring")

        // Test completing pattern again (should score again after cooldown)
        // Wait past cooldown (600ms)
        val ball4 = BallPositionViewModel.BallPositionData(
            left = 140f,
            top = 140f,
            right = 160f,
            bottom = 160f,
            timestamp = 4000L, // 1000ms after last score
            confidence = 0.9f
        )
        viewModel.hitTarget(ball4, tolerance = 20)

        val ball5 = BallPositionViewModel.BallPositionData(
            left = 740f,
            top = 140f,
            right = 760f,
            bottom = 160f,
            timestamp = 5000L,
            confidence = 0.9f
        )
        viewModel.hitTarget(ball5, tolerance = 20)

        val ball6 = BallPositionViewModel.BallPositionData(
            left = 440f,
            top = 740f,
            right = 460f,
            bottom = 760f,
            timestamp = 6000L,
            confidence = 0.9f
        )
        viewModel.hitTarget(ball6, tolerance = 20)

        // Score should now be 2
        assertEquals(2, viewModel.liveScore.value, "Score should be 2 after second completion")
    }

    @Test
    fun `cooldown prevents rapid duplicate scoring`() {
        setupTestGrid()
        val viewModel = BallPositionViewModel()
        Training.trainingBuilder("Simple Triangle")
        viewModel.resetScores()

        // Complete pattern once
        hitFullPattern(viewModel, baseTimestamp = 1000L)
        assertEquals(1, viewModel.liveScore.value, "First completion should score")

        // Try to complete again immediately (within 600ms cooldown)
        hitFullPattern(viewModel, baseTimestamp = 1500L) // Only 500ms later
        assertEquals(1, viewModel.liveScore.value, "Should block score within cooldown period")

        // Complete after cooldown expires
        hitFullPattern(viewModel, baseTimestamp = 2000L) // 1000ms after first score (> 600ms)
        assertEquals(2, viewModel.liveScore.value, "Should score after cooldown expires")
    }

    @Test
    fun `targetHitCounter guard prevents scoring before training loads`() {
        setupTestGrid()
        val viewModel = BallPositionViewModel()

        // Don't load training - targetHitCounter will be -1
        viewModel.resetScores()

        // Try to trigger scoring condition
        viewModel.hitTargetCounter = -1 // Matches unloaded state

        val ball = BallPositionViewModel.BallPositionData(
            left = 140f,
            top = 140f,
            right = 160f,
            bottom = 160f,
            timestamp = 1000L,
            confidence = 0.9f
        )

        // This should not score because targetHitCounter is not > 0
        viewModel.hitTarget(ball, tolerance = 20)

        assertEquals(0, viewModel.liveScore.value, "Should not score when training not loaded")
    }

    // Helper function to simulate completing a full pattern
    private fun hitFullPattern(viewModel: BallPositionViewModel, baseTimestamp: Long) {
        val targets = listOf(
            Triple(150f, 150f, baseTimestamp),         // top-left
            Triple(750f, 150f, baseTimestamp + 100),   // top-right
            Triple(450f, 750f, baseTimestamp + 200)    // centre-bottom
        )

        targets.forEach { (x, y, ts) ->
            val ball = BallPositionViewModel.BallPositionData(
                left = x - 10,
                top = y - 10,
                right = x + 10,
                bottom = y + 10,
                timestamp = ts,
                confidence = 0.9f
            )
            viewModel.hitTarget(ball, tolerance = 20)
        }
    }

    // Setup test grid with known coordinates (matches bundled pattern expectations)
    private fun setupTestGrid() {
        TrainingGrid.gridBoxes = mutableListOf(
            Box(100f, 100f, 200f, 200f, "top-left"),
            Box(400f, 100f, 500f, 200f, "centre-top"),
            Box(700f, 100f, 800f, 200f, "top-right"),
            Box(700f, 400f, 800f, 500f, "centre-right"),
            Box(700f, 700f, 800f, 800f, "bottom-right"),
            Box(400f, 700f, 500f, 800f, "centre-bottom"),
            Box(100f, 700f, 200f, 800f, "bottom-left"),
            Box(100f, 400f, 200f, 500f, "centre-left"),
            Box(400f, 400f, 500f, 500f, "centre")
        )
    }
}
