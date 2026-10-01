# BallStars Grid Games - Training & Game Screen Logic

## Overview

This document explains how the training pattern selection, gameplay mechanics, and scoring systems work in the BallStars Grid Games app. Use this as a reference for designing screens and understanding the game flow.

---

## 1. Data Models

### TrainingPattern

The core pattern data structure:

```kotlin
data class TrainingPattern(
    val id: String,                  // Unique identifier
    val name: String,                // Display name (e.g., "Triangles")
    val description: String,         // Brief explanation
    val targetSequence: List<Int>,   // 1-9 positions in order
    val difficultyLevel: Int,        // 1-5 rating
    val estimatedDuration: Int       // Estimated seconds
)
```

**Example Pattern:**
```kotlin
TrainingPattern(
    id = "triangles",
    name = "Triangles",
    description = "Triangle patterns for footwork precision",
    targetSequence = listOf(5, 7, 4, 5),  // Center → Bottom-Left → Middle-Left → Center
    difficultyLevel = 1,
    estimatedDuration = 8
)
```

### GameResult

What gets returned when a game finishes:

```kotlin
data class GameResult(
    val pattern: TrainingPattern,   // Which pattern was played
    val score: Int,                 // Calculated total score
    val totalTime: Long,            // Milliseconds played
    val hitCount: Int,              // Successful target hits
    val missCount: Int,             // Missed targets
    val accuracy: Float             // Hit rate 0-100%
)
```

### Grid System

**GridTarget** (Single box in the 3×3 grid):
```kotlin
data class GridTarget(
    val index: Int,      // 0-8 (internal)
    val x: Float,        // Top-left X coordinate
    val y: Float,        // Top-left Y coordinate
    val width: Float,    // Box width
    val height: Float    // Box height
)
```

**CalibratedGrid** (Complete 9-target grid):
```kotlin
data class CalibratedGrid(
    val targets: List<GridTarget>,  // Exactly 9 targets
    val zoomRatio: Float = 0f       // Camera zoom from calibration
)
```

**Grid Layout:**
```
Grid Indices (0-8):        Sequence Numbers (1-9):
0 - 1 - 2                  1 - 2 - 3
|   |   |                  |   |   |
3 - 4 - 5                  4 - 5 - 6
|   |   |                  |   |   |
6 - 7 - 8                  7 - 8 - 9
```

---

## 2. Available Training Patterns (15 Total)

### Beginner (Difficulty 1)
| Pattern | Sequence | Duration | Description |
|---------|----------|----------|-------------|
| **Triangles** | `[5, 7, 4, 5]` | 8s | Triangle patterns for footwork precision |
| **Rollers** | `[4, 5, 6, 4, 5, 6]` | 9s | Rolling ball control across grid |
| **Penguin Feet** | `[4, 6, 4, 6, 5]` | 8s | Quick side-to-side footwork |
| **Cross Pattern** | `[2, 4, 5, 6, 8, 5]` | 10s | Plus sign cross formation |

### Intermediate (Difficulty 2-3)
| Pattern | Sequence | Duration | Description |
|---------|----------|----------|-------------|
| **The Cruff** | `[5, 2, 4, 5, 6, 8]` | 10s | Cruyff turn inspired movement |
| **V's** | `[7, 5, 9, 5, 7]` | 10s | V-shaped movement pattern |
| **Box Run** | `[1, 2, 3, 6, 9, 8, 7, 4, 1]` | 15s | Square perimeter running |
| **Zig Zag** | `[1, 3, 7, 9, 3, 7]` | 12s | Diagonal zig-zag pattern |
| **Diamond** | `[2, 4, 8, 6, 2]` | 10s | Diamond shape movement drill |
| **Corner to Centre** | `[1, 5, 3, 5, 9, 5, 7, 5]` | 14s | Corners to center repeatedly |
| **Edge to Centre** | `[2, 5, 4, 5, 6, 5, 8, 5]` | 14s | Edges inward to center |

### Advanced (Difficulty 3-4)
| Pattern | Sequence | Duration | Description |
|---------|----------|----------|-------------|
| **Figure 8** | `[1, 2, 5, 8, 9, 6, 5, 4, 1]` | 18s | Flowing figure-eight |
| **Ladder** | `[7, 4, 1, 2, 3, 6, 9, 8, 7]` | 16s | Ladder drill up and down |
| **Spiral** | `[1, 2, 3, 6, 9, 8, 7, 4, 5]` | 18s | Spiral from outside to center |
| **Combo Flow** | `[1, 3, 5, 7, 9, 6, 3, 4, 5, 8, 1]` | 22s | Advanced combination (Diff 4) |

---

## 3. Pattern Selection Screen

### UI Components

**Filter Tabs:**
- ALL (shows all 15 patterns)
- BEGINNER (difficulty 1)
- INTERMEDIATE (difficulty 2-3)
- ADVANCED (difficulty 3-4)

**Pattern Card Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ [Image]  Pattern Name                    [Mini Grid] [→]│
│  72x72   Description text                   56x56       │
│          ⭐⭐⭐⭐⭐ (difficulty)                          │
│          ⏱ 12s (estimated duration)                     │
└─────────────────────────────────────────────────────────┘
```

**Mini Grid Preview:**
- Dynamic 3×3 grid visualization
- Shows numbered sequence path with dashed lines
- Nodes at each position in sequence
- Larger node for starting position
- Colors: Cyan grid + nodes, Gold path

**Interaction:**
- Tap any pattern card → Navigate to Game Screen with selected pattern

---

## 4. Game Screen Flow

### Phase 1: PREVIEW MODE

**Initial State:**
- Message: "Watch the pattern..."
- Animates through target sequence with 800ms delay per target
- Highlights next target with orange pulsing glow
- Shows "START GAME" button at bottom

**What User Sees:**
```
┌─────────────────────────────────────┐
│   [Camera Preview with Grid]       │
│                                     │
│   Target 5 glows orange → 800ms    │
│   Target 7 glows orange → 800ms    │
│   Target 4 glows orange → 800ms    │
│   (repeats sequence)                │
│                                     │
│   [ START GAME ]                    │
└─────────────────────────────────────┘
```

### Phase 2: ACTIVE GAME MODE

**Initialization:**
```kotlin
startTime = System.currentTimeMillis()
gameActive = true
currentTargetIndex = 0
hitCount = 0
missCount = 0
```

**Camera Setup:**
- Applies `zoomRatio` from calibrated grid
- Starts TensorFlow Lite ball detection
  - Confidence threshold: 0.20f
  - Max detections: 1
  - GPU accelerated (Adreno GPU)
  - 4 CPU threads

**Grid Overlay:**
- Draws all 9 target boxes
- Current target: Cyan breathing glow (pulsing 0.4 → 0.8 alpha)
- Other targets: Subtle cyan outlines

---

## 5. Gameplay Mechanics

### Ball Detection Pipeline

**Step 1: TensorFlow Detection**
```
Camera Frame → TensorFlow Lite Model → Ball Coordinates + Confidence
```

**Step 2: Coordinate Transformation**
```kotlin
// Ball coords are in tensor space, targets are in screen space
scaleX = tensorWidth / screenWidth
scaleY = tensorHeight / screenHeight

transformedBallX = detectedX / scaleX
transformedBallY = detectedY / scaleY
```

**Step 3: Hit Detection**
```kotlin
val currentTargetBox = calibratedGrid[currentTargetNumber - 1]
val ballHitsTarget = currentTargetBox.contains(transformedBallX, transformedBallY)
```

### Hit Registration Logic

**Pattern Loop System:**
```kotlin
// Pattern repeats indefinitely - player does multiple laps
val actualTargetIndex = currentTargetIndex % pattern.targetSequence.size
val currentTargetNumber = pattern.targetSequence[actualTargetIndex]
```

**Hit Deduplication:**
```kotlin
// Prevents counting same target twice in one iteration
val alreadyHitThisPosition = hitPositionsInIteration.contains(actualTargetIndex)

if (ballHitsTarget && lastHitTargetNumber != currentTargetNumber && !alreadyHitThisPosition) {
    // VALID HIT - Register it
    hitPositionsInIteration = hitPositionsInIteration + actualTargetIndex
    hitCount++
    lastHitTargetNumber = currentTargetNumber
    currentTargetIndex++  // Move to next target

    // Visual feedback: Yellow burst effect on target
}
```

**Miss Detection:**
```kotlin
// Ball left target area before hitting correct target
if (!ballHitsTarget && wasInTargetBefore) {
    missCount++
    currentStreak = 0  // Reset streak
}
```

### Streak & Combo System

**Streak Tracking:**
```kotlin
// Increments on each successful hit
onHit: currentStreak++
if (currentStreak > bestStreak) bestStreak = currentStreak

// Resets when ball leaves any target box
onMiss: currentStreak = 0
```

**Combo Multiplier:**
```kotlin
when {
    currentStreak >= 10 -> combo = "3x" (Magenta color)
    currentStreak >= 5  -> combo = "2x" (Cyan color)
    else                -> combo = "1x" (White color)
}
```

### Star Rewards

**Earning Stars:**
```kotlin
// Awards 1 star every 5 hits
val earnedStars = hitCount / 5

// Shows animation when new star is earned
if (hitCount % 5 == 0) {
    showMessage("⭐ +STAR! ⭐")
}
```

### Power-Up System

**Activation:**
```kotlin
// Triggers when completing one full pattern iteration
if (currentTargetIndex % pattern.targetSequence.size == 0) {
    powerUpActive = true
    powerUpStartTime = currentTimeMs
    hitPositionsInIteration = emptySet()  // Reset for next lap
}
```

**Display:**
- Shows "⚡ POWER-UP ACTIVE" banner
- Countdown timer: 5 seconds
- Auto-deactivates after duration

---

## 6. HUD (Heads-Up Display)

**Bottom Score Overlay:**
```
┌─────────────────────────────────────────────────────┐
│ HITS: 24 | STREAK: 🔥 12 | COMBO: 3x | STARS: ⭐ 4 │
└─────────────────────────────────────────────────────┘
```

**Color Coding:**

| Element | Color Logic |
|---------|-------------|
| **HITS** | Green (#4CAF50) |
| **STREAK** | White (0-4), Orange 🔥 (5-9), Red 🔥 (10+) |
| **COMBO** | White (1x), Cyan (2x), Magenta (3x) |
| **STARS** | Gold with shadow |

---

## 7. Scoring System

### Score Calculation Formula

```kotlin
fun calculateScore(totalTimeMs: Long, accuracy: Float): Int {
    val baseScore = 1000
    val timeBonus = maxOf(0, (30000 - totalTimeMs) / 100).toInt()
    val accuracyBonus = (accuracy * 10).roundToInt()

    return baseScore + timeBonus + accuracyBonus
}
```

**Breakdown:**
- **Base Score:** 1000 points (everyone gets this)
- **Time Bonus:** Faster completion = higher bonus
  - Formula: `(30 seconds - actualTime) / 100ms = bonus points`
  - Example: Finish in 20 seconds → +100 bonus
  - No bonus if over 30 seconds
- **Accuracy Bonus:** Hit percentage × 10
  - Example: 90% accuracy → +900 bonus points

**Example Score:**
```
Time: 20 seconds (20,000ms)
Accuracy: 90% (hit 27/30 targets)

Base Score:     1000
Time Bonus:      100  [(30000 - 20000) / 100]
Accuracy Bonus:  900  [90 * 10]
─────────────────────
TOTAL:          2000 points
```

### Accuracy Calculation

```kotlin
val totalTargets = hitCount + missCount
val accuracy = if (totalTargets > 0) {
    (hitCount.toFloat() / totalTargets.toFloat()) * 100f
} else {
    0f
}
```

---

## 8. Game Completion

### End Game Trigger

Player manually ends game by pressing STOP or back button:

```kotlin
onGameComplete {
    val totalTime = System.currentTimeMillis() - startTime
    val accuracy = calculateAccuracy(hitCount, missCount)
    val score = calculateScore(totalTime, accuracy)

    val result = GameResult(
        pattern = currentPattern,
        score = score,
        totalTime = totalTime,
        hitCount = hitCount,
        missCount = missCount,
        accuracy = accuracy
    )

    navigateToScoreScreen(result)
}
```

---

## 9. Score Screen

**Displays:**
- Pattern name and description
- Final score (large, prominent)
- Accuracy percentage
- Hit/Miss breakdown
- Total time
- Earned stars
- Best streak achieved

**Actions:**
- **Play Again** → Back to Pattern Selection
- **Back to Home** → Navigate to Home/PlayChoice screen

---

## 10. Visual Effects Reference

### Target Box States

**Preview Mode (Orange):**
- Triple-layer glow
- Pulsing intensity: 0.2x → 0.8x alpha
- 1500ms animation cycle
- Active target glows brightly

**Hit State (Yellow Burst):**
- Outer glow: Yellow semi-transparent (Screen blend)
- Medium layer: Orange (Screen blend)
- Core: Bright yellow line
- Fill: Orange 0.4 alpha
- Explosive visual feedback

**Idle State (Cyan Breathing):**
- Multi-layer cyan glow
- Breathing scale: 1.0 → 1.05
- Pulsing alpha: 0.4 → 0.8
- 1500ms breathing cycle

---

## 11. Technical Implementation Notes

### Performance Considerations

**TensorFlow Lite Configuration:**
- GPU delegate for hardware acceleration
- 4 threads for CPU processing
- Single detection mode (max 1 ball)
- 0.20f confidence threshold

**Frame Processing:**
- Real-time camera feed analysis
- Coordinate transformation per frame
- Hit detection runs every frame

**State Management:**
```kotlin
// Core tracking variables
var currentTargetIndex by remember { mutableStateOf(0) }
var hitCount by remember { mutableStateOf(0) }
var missCount by remember { mutableStateOf(0) }
var currentStreak by remember { mutableStateOf(0) }
var bestStreak by remember { mutableStateOf(0) }
var lastHitTargetNumber by remember { mutableStateOf<Int?>(null) }
var hitPositionsInIteration by remember { mutableStateOf(setOf<Int>()) }
```

### Grid Calibration Integration

**Before Game Starts:**
1. Load calibrated grid from `GridRepository`
2. Apply `zoomRatio` to camera
3. Use target box coordinates for hit detection

**During Gameplay:**
- Ball coordinates transformed to match grid space
- Hit detection uses calibrated box boundaries
- No re-calibration needed mid-game

---

## 12. Navigation Flow

```
Splash Screen
     ↓
Play Choice
     ↓
Pattern Selection (15 patterns, filterable)
     ↓ [User selects pattern]
Game Screen
     │
     ├─ Preview Mode (watch pattern)
     │      ↓ [User presses START]
     ├─ Active Game (ball detection + scoring)
     │      ↓ [User ends game]
     └─ Game Complete
             ↓ [Create GameResult]
Score Screen
     ↓ [Play Again or Home]
Pattern Selection / Home
```

---

## Design Considerations

### For Pattern Selection Screen
- Show thumbnail images for each pattern
- Visual difficulty indicator (star rating)
- Estimated duration badge
- Mini grid preview showing sequence path
- Filter by difficulty level
- Easy tap interaction

### For Game Screen
- Large, clear camera preview
- Visible grid overlay with good contrast
- Bottom HUD for real-time stats
- Clear visual feedback on hits (yellow burst)
- Streak/combo indicators with exciting colors
- Power-up activation messaging
- "Watch pattern" preview before starting

### For Score Screen
- Celebrate the score (large number, animation)
- Show breakdown: accuracy, time, hits/misses
- Display earned stars prominently
- Encourage replay with "Play Again" button
- Option to return home

---

## Key Files Reference

**Data Models:**
- `shared/src/commonMain/kotlin/venturewave/one/gridgames/model/TrainingPattern.kt`
- `shared/src/commonMain/kotlin/venturewave/one/gridgames/model/GridTarget.kt`

**UI Screens:**
- `shared/src/commonMain/kotlin/venturewave/one/gridgames/ui/screens/training/TrainingPatternSelectionScreen.kt`
- `shared/src/androidMain/kotlin/venturewave/one/gridgames/ui/GameScreen.android.kt`
- `shared/src/commonMain/kotlin/venturewave/one/gridgames/ui/ScoreScreen.kt`

**Components:**
- `shared/src/commonMain/kotlin/venturewave/one/gridgames/ui/components/pattern/MiniGridPreview.kt`
- `shared/src/commonMain/kotlin/venturewave/one/gridgames/ui/components/pattern/DifficultyStars.kt`

**Navigation:**
- `shared/src/commonMain/kotlin/venturewave/one/gridgames/navigation/NavigationState.kt`
- `shared/src/commonMain/kotlin/venturewave/one/gridgames/App.kt`

---

## Summary

The BallStars training system provides a complete game loop:

1. **Select Pattern** - Choose from 15 pre-built patterns with varying difficulty
2. **Preview** - Watch the sequence animate before playing
3. **Play** - Use real ball to hit targets in sequence, indefinitely looping
4. **Track Progress** - Hits, streaks, combos, stars, power-ups
5. **Score** - Based on time, accuracy, with bonuses
6. **Review** - See detailed results and play again

The system is designed for engaging, competitive gameplay with clear progression and rewarding feedback systems.
