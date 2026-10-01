# BallStars UI Reskin & Game Flow - Implementation Plan

**Version:** 1.0
**Date:** 2026-09-25
**Target Device:** Pixel 9a (Android, Adreno GPU)
**Status:** Awaiting Approval

---

## Executive Summary

This plan outlines a comprehensive UI reskin and feature expansion for the Grid Games mobile app, transforming it into **BallStars** - a street football-themed kids' game. The reskin will preserve all existing detection/tracking systems while building a new, engaging UI layer on top.

**Key Principle:** *Wrap, don't replace.* All OpenCV grid detection and TFLite ball tracking remain untouched, accessed via clean interfaces.

---

## 1. Current Architecture Analysis

### 1.1 Module Structure

**Confirmed Structure:**
```
/androidApp           - Android entry point (minimal)
/shared
  ├─ commonMain      - Shared game logic, UI, navigation, models
  ├─ androidMain     - CameraX, OpenCV, TFLite implementations
  └─ iosMain         - Stubs for future iOS implementation
```

**Current Navigation:**
- Simple sealed class-based (`Screen` sealed class)
- No Jetpack Navigation or third-party libs
- `NavigationState` with mutable back stack
- Platform-specific routing via `expect/actual` for camera screens

**State Management:**
- No DI framework (manual dependency injection)
- ViewModels are Kotlin classes (not androidx.lifecycle)
- StateFlow/MutableStateFlow for reactive state
- `remember { }` for ViewModel creation in Composables

**Data Persistence:**
- `GridRepository`: In-memory singleton (lost on restart)
- `MatProfileRepository`: AndroidX DataStore Preferences (persisted)
- No backend, no network calls
- Training patterns hardcoded in companion object

### 1.2 Critical Detection Systems (DO NOT MODIFY)

**Camera Configuration:**
- **Aspect Ratio:** 4:3 (RATIO_4_3) - **LOCKED, NON-NEGOTIABLE**
- **PreviewView:** FILL_CENTER scale type
- **Zoom:** Linear 0.0-1.0, saved in CalibratedGrid
- **Location:** `ScanTargets2Screen.kt`, `GameScreen.android.kt`

**OpenCV Grid Detection:**
- **Algorithm:** Gaussian blur → binary threshold → morphological closing → contour detection → perspective transform
- **Output:** 9 `GridPoint` objects in normalized 0-1 coordinates
- **File:** `GridDetector.android.kt`
- **Status:** Working, do not touch

**TFLite Ball Detection:**
- **Model:** `ball-tracker.tflite` (4.2 MB float32) in assets
- **New Model Available:** `ball-tracker-float32.tflite` (trained 2026-09-25, 100% AP @ IoU=0.50)
- **Delegate:** GPU (Adreno) with 4 CPU threads fallback
- **Threshold:** 0.20f confidence
- **File:** `BallDetectorHelper.kt`, `BallDetectorViewModel.kt`
- **Status:** Working, do not touch inference code

**Coordinate Transform (CRITICAL):**
- **Location:** `GameScreen.android.kt` lines 186-203
- **Function:** Screen space → camera/tensor space for hit detection
- **Issue:** Must account for FILL_CENTER crop/scale
- **Action Required:** Extract to tested utility function, reuse across all screens

### 1.3 Gameplay Entry Points

**Grid Calibration:**
- **Screen:** `ScanTargets2Screen.kt`
- **Flow:** 9 manual touch points → calculate Nexus Grid → save to GridRepository
- **State:** `touchPositions: List<Offset>`, `isConfirmed: Boolean`, `zoomRatio: Float`
- **Output:** `List<GridTarget>` with x, y, width, height in pixels

**Ball Tracking:**
- **ViewModel:** `BallDetectorViewModel`
- **Flow:** Camera frame → queue → TFLite inference → StateFlow<List<Detection>>
- **Output:** `Detection { boundingBox: RectF, categories: List<Category> }`
- **FPS:** ~28-30 FPS with GPU delegate

**Hit Detection:**
- **Location:** `GameScreen.android.kt` lines 138-254
- **Algorithm:** Ball bounding box intersection with current target in camera space
- **Debouncing:** Track `lastHitTargetNumber` to prevent double-counting
- **Pattern Looping:** Use modulo to repeat infinitely

**Game Mechanics:**
- **Patterns:** 9 hardcoded `TrainingPattern` objects (Messi Triangles, Quick Reflex, etc.)
- **Scoring:** Hits, streak, combo (2x at 5 streak, 3x at 10), stars (every 5 hits), power-up (5s after pattern completion)
- **Issue:** Scoring logic is split between `GameScreen.android.kt` and unused `BallPositionViewModel`

---

## 2. Design System Extraction

### 2.1 Color Palette (from Reference Image)

**Verified against `ballstars-reference.png`:**

| Token | Hex | Usage | Reference |
|---|---|---|---|
| `bgDeep` | `#0A1929` | App background, phone frame | Dark navy in all screens |
| `surface` | `#132A42` | Cards, panels | Profile card, move success card |
| `surfaceRaised` | `#1A3A56` | List rows, nav bar | Bottom nav, recent moves list |
| `outline` | `#2A5073` | Card borders, dividers | Visible on profile card edges |
| `primary` | `#1ED36A` | Primary CTAs, XP bar, headings | "Get Started", "Next", XP bar |
| `glowCyan` | `#00D9FF` | Grid lines, tracking, camera FAB | Grid overlay, bottom nav FAB ring |
| `gold` | `#FFC21A` | Stars, move glyphs, XP text, success CTA | Move glyph dots/arrows, "+100 XP" |
| `flame` | `#FF7A1A` | Streaks, combo heat | Streak icon (fire emoji replacement) |
| `textPrimary` | `#FFFFFF` | Headings, primary text | "Nice move!", "Jamie", move names |
| `textSecondary` | `#B7C7D6` | Body text, subtitles | "Keep the ball in view", timestamps |
| `error` | `#FF4444` | Errors, ball lost state | (Inferred for error states) |
| `success` | `#1ED36A` | Success states | (Same as primary) |

**Gradient Backgrounds:**
- Onboarding: Illustrated street scenes (full-bleed images)
- Splash/Empty: Radial gradient from center (dark navy → deep blue → bgDeep)
- Move Success: Radial burst gradient (gold/cyan rays on bgDeep)

**Glow Effects:**
- Outer glow: Color at 30% alpha, 16dp radius
- Medium glow: Color at 60% alpha, 8dp radius
- Core: Full color, 4dp stroke
- Blend mode: `BlendMode.Screen` for additive glow

### 2.2 Typography

**Font Pairing (Recommendation):**

**Display Font:** **Lilita One** (Google Fonts)
- **Rationale:** Heavy, rounded, playful - perfect for kids. Good readability at distance. Single weight (Regular) keeps bundle small.
- **Usage:** Logo (BallStars), screen headings, CTA labels, move names
- **Sizes:** 48sp (logo), 32sp (screen titles), 24sp (move names), 20sp (buttons)

**Body Font:** **Nunito** (Google Fonts)
- **Rationale:** Rounded sans-serif, highly legible, excellent for long-form text. Multiple weights available.
- **Weights:** Regular (400), SemiBold (600), Bold (700)
- **Usage:** Body text, stats, descriptions, timestamps
- **Sizes:** 16sp (body), 14sp (captions), 12sp (timestamps)

**Alternative Considered:**
- Bungee: Too condensed, readability issues at small sizes
- Rubik Black: Good but less playful than Lilita One

**Type Scale:**
```kotlin
object BallStarsTypography {
    val displayLarge = TextStyle(fontFamily = LilitaOne, fontSize = 48.sp, lineHeight = 56.sp)
    val displayMedium = TextStyle(fontFamily = LilitaOne, fontSize = 32.sp, lineHeight = 40.sp)
    val headlineLarge = TextStyle(fontFamily = LilitaOne, fontSize = 24.sp, lineHeight = 32.sp)
    val headlineMedium = TextStyle(fontFamily = LilitaOne, fontSize = 20.sp, lineHeight = 28.sp)
    val bodyLarge = TextStyle(fontFamily = Nunito, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
    val bodyMedium = TextStyle(fontFamily = Nunito, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal)
    val labelLarge = TextStyle(fontFamily = Nunito, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val labelMedium = TextStyle(fontFamily = Nunito, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
}
```

### 2.3 Shapes & Spacing

**Shapes (from reference):**
```kotlin
object BallStarsShapes {
    val buttonPill = RoundedCornerShape(percent = 50)          // Full pill (buttons)
    val cardLarge = RoundedCornerShape(24.dp)                  // Main cards
    val cardMedium = RoundedCornerShape(16.dp)                 // List items
    val cardSmall = RoundedCornerShape(12.dp)                  // Chips, badges
    val circleGlyph = CircleShape                              // Move glyph background
    val bottomNav = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) // Bottom nav
}
```

**Spacing Scale:**
```kotlin
object BallStarsSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}
```

**Touch Targets:**
- Primary actions: 56dp minimum (buttons, FAB)
- Secondary actions: 48dp minimum (nav items, list items)
- Tertiary actions: 44dp minimum (close buttons, checkboxes)

### 2.4 Move Glyphs (from reference)

**Design Pattern (extracted from image):**
- **Container:** White circle outline (2dp stroke)
- **Dots:** Gold filled circles (12dp diameter)
- **Arrows:** Gold with gradient tail, 4dp stroke
- **Glyph Size:** 80dp container (moves library), 120dp (move success), 40dp (recent moves list)

**Reference Glyphs from Image:**
1. **Messi Triangles:** 3 dots forming triangle (top-left, bottom-left, bottom-right) with arrows in clockwise direction
2. **The Cruyff:** 4 dots forming square with curved arrow creating loop pattern
3. **Penguin Feet:** 3 dots with zig-zag arrows (left-right-left)
4. **Step Overs:** 4 dots in cross pattern with circular arrows
5. **Elastico:** 5 dots forming S-curve with flowing arrows

**Glyph Generation Algorithm:**
- Input: `List<Int>` cell sequence (0-8, row-major)
- Map cells to normalized grid positions (0.0-1.0)
- Draw dots at cell centers
- Draw arrows between consecutive cells using quadratic Bezier curves
- Arrow direction: Calculate angle between points, add 30° for arrowhead
- Scale to target size

---

## 3. Proposed Architecture

### 3.1 Interface Boundaries (commonMain)

**GridTracker Interface (wrap existing detection):**
```kotlin
interface GridTracker {
    val calibrationState: StateFlow<GridCalibrationState>
    val gridData: StateFlow<CalibratedGrid?>
    val ballState: StateFlow<BallState>
    val cellEvents: SharedFlow<CellEvent>

    fun startCalibration()
    fun submitCorner(position: Offset)
    fun confirmGrid()
    fun reset()
}

sealed class GridCalibrationState {
    data object Idle : GridCalibrationState()
    data class CollectingCorners(val count: Int, val required: Int) : GridCalibrationState()
    data class Locked(val quality: Float) : GridCalibrationState() // quality 0-1
    data object Lost : GridCalibrationState()
}

data class BallState(
    val position: Offset?,           // Normalized grid space (0-1, 0-1)
    val cellIndex: Int?,             // 0-8 row-major, null if outside grid
    val confidence: Float,           // 0-1
    val visible: Boolean,
    val timestamp: Long
)

data class CellEvent(
    val cellIndex: Int,              // 0-8
    val timestamp: Long,
    val confidence: Float
)
```

**Implementation (androidMain):**
```kotlin
class AndroidGridTracker(
    private val gridDetectionViewModel: GridDetectionViewModel,
    private val ballDetectorViewModel: BallDetectorViewModel,
    private val gridRepository: GridRepository
) : GridTracker {
    // Adapt existing ViewModels to interface
    // Transform Detection bounding boxes to normalized grid coordinates
    // Emit CellEvent when ball enters new cell
}
```

**FakeGridTracker (commonMain for testing/preview):**
```kotlin
class FakeGridTracker : GridTracker {
    private val _cellEvents = MutableSharedFlow<CellEvent>()
    override val cellEvents = _cellEvents.asSharedFlow()

    // Scripted sequences for testing
    suspend fun playSequence(cells: List<Int>, delayMs: Long = 800) {
        cells.forEach { cell ->
            _cellEvents.emit(CellEvent(cell, System.currentTimeMillis(), 0.95f))
            delay(delayMs)
        }
    }
}
```

### 3.2 Move System (commonMain)

**MoveDefinition (data-driven):**
```kotlin
@Serializable
data class MoveDefinition(
    val id: String,                      // "messi_triangles"
    val name: String,                    // "Messi Triangles"
    val description: String,             // "Quick close-control triangle pattern"
    val cellSequence: List<Int>,         // [4, 6, 3, 4] (0-8, row-major)
    val alternativeSequences: List<List<Int>> = emptyList(), // [[4, 6, 3], [4, 2, 1]]
    val maxTimeBetweenTouchesMs: Long,   // 2000ms
    val xpValue: Int,                    // 100
    val difficulty: Int,                 // 1-5
    val unlockLevel: Int,                // 1
    val tips: List<String>,              // ["Keep the ball close", "Use inside of foot"]
    val glyphPath: String                // SVG path data or serialized Path
)
```

**PatternMatcher (event processor):**
```kotlin
class PatternMatcher(private val moves: List<MoveDefinition>) {
    private val recentCells = mutableListOf<CellEvent>()
    private val _detectedMoves = MutableSharedFlow<MoveDetected>()
    val detectedMoves = _detectedMoves.asSharedFlow()

    suspend fun processCellEvent(event: CellEvent) {
        // Add to recent window
        recentCells.add(event)

        // Remove events older than max timeout
        val cutoff = event.timestamp - maxTimeout
        recentCells.removeAll { it.timestamp < cutoff }

        // Check each move for match
        moves.forEach { move ->
            val match = checkMatch(move, recentCells)
            if (match != null) {
                _detectedMoves.emit(match)
                recentCells.clear()
            }
        }
    }

    private fun checkMatch(move: MoveDefinition, events: List<CellEvent>): MoveDetected? {
        // Check primary sequence
        if (matchesSequence(events, move.cellSequence, move.maxTimeBetweenTouchesMs)) {
            return MoveDetected(
                move = move,
                quality = calculateQuality(events, move),
                timing = calculateTiming(events, move),
                timestamp = events.last().timestamp
            )
        }
        // Check alternative sequences
        move.alternativeSequences.forEach { altSeq ->
            if (matchesSequence(events, altSeq, move.maxTimeBetweenTouchesMs)) {
                return MoveDetected(move, calculateQuality(events, move), calculateTiming(events, move), events.last().timestamp)
            }
        }
        return null
    }
}

data class MoveDetected(
    val move: MoveDefinition,
    val quality: Float,      // 0-1 (accuracy of touches within cells)
    val timing: Float,       // 0-1 (speed, 1.0 = perfect timing)
    val timestamp: Long
)
```

**Unit Tests (commonTest):**
```kotlin
class PatternMatcherTest {
    @Test
    fun `detects Messi Triangles with exact sequence`()
    @Test
    fun `ignores partial sequence`()
    @Test
    fun `rejects sequence with timeout exceeded`()
    @Test
    fun `detects alternative sequence`()
    @Test
    fun `calculates quality based on cell dwell time`()
}
```

### 3.3 Match Scorer (commonMain)

**MatchScorer (Tony Hawk × Just Dance logic):**
```kotlin
class MatchScorer(
    private val moves: List<MoveDefinition>,
    private val namedCombos: List<NamedCombo>
) {
    data class State(
        val score: Int = 0,
        val currentCombo: List<MoveDetected> = emptyList(),
        val comboMeter: Float = 0f,           // 0-1, drains over time
        val specialMeter: Float = 0f,         // 0-1, fills with long combos
        val specialActive: Boolean = false,
        val lastEventTime: Long = 0,
        val bankedScore: Int = 0
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<MatchEvent>()
    val events = _events.asSharedFlow()

    suspend fun onMoveDetected(move: MoveDetected, grade: Grade) {
        val current = _state.value

        // Calculate points with diminishing returns
        val movePoints = calculateMovePoints(move, grade, current.currentCombo)
        val multiplier = if (current.specialActive) 2.0 else 1.0
        val finalPoints = (movePoints * multiplier).toInt()

        // Add to combo
        val newCombo = current.currentCombo + move

        // Fill meters
        val newComboMeter = minOf(1f, current.comboMeter + 0.2f)
        val newSpecialMeter = minOf(1f, current.specialMeter + (newCombo.size * 0.1f))

        // Check for named combos
        val namedCombo = checkNamedCombo(newCombo)
        if (namedCombo != null) {
            _events.emit(MatchEvent.NamedComboTriggered(namedCombo))
        }

        _state.value = current.copy(
            currentCombo = newCombo,
            comboMeter = newComboMeter,
            specialMeter = newSpecialMeter,
            lastEventTime = move.timestamp
        )

        _events.emit(MatchEvent.MoveScored(move.move, finalPoints, grade))
    }

    suspend fun onBallLeftGrid() {
        val current = _state.value
        if (current.currentCombo.isNotEmpty()) {
            // BAIL! Lose pending combo
            _events.emit(MatchEvent.ComboBailed(current.currentCombo.size))
            _state.value = current.copy(
                currentCombo = emptyList(),
                comboMeter = 0f,
                specialActive = false
            )
        }
    }

    suspend fun onBallStoppedInCenter(durationMs: Long) {
        if (durationMs >= 1000) {
            // Bank the combo
            val current = _state.value
            val comboScore = calculateComboScore(current.currentCombo)
            _events.emit(MatchEvent.ComboLanded(comboScore, current.currentCombo))
            _state.value = current.copy(
                score = current.score + comboScore,
                bankedScore = current.bankedScore + comboScore,
                currentCombo = emptyList(),
                comboMeter = 0f,
                specialActive = false
            )
        }
    }

    suspend fun tick(deltaMs: Long) {
        // Drain combo meter over time
        val current = _state.value
        val newComboMeter = maxOf(0f, current.comboMeter - (deltaMs / 5000f)) // 5s to drain

        if (newComboMeter == 0f && current.currentCombo.isNotEmpty()) {
            // Combo timeout bail
            onBallLeftGrid()
        } else {
            _state.value = current.copy(comboMeter = newComboMeter)
        }
    }

    private fun calculateMovePoints(move: MoveDetected, grade: Grade, currentCombo: List<MoveDetected>): Int {
        val basePoints = move.move.xpValue
        val gradeMultiplier = when (grade) {
            Grade.PERFECT -> 1.5
            Grade.GREAT -> 1.2
            Grade.GOOD -> 1.0
            Grade.MISS -> 0.0
        }

        // Diminishing returns for repeating same move
        val repeatCount = currentCombo.count { it.move.id == move.move.id }
        val repeatPenalty = when (repeatCount) {
            0 -> 1.0
            1 -> 0.75
            2 -> 0.50
            else -> 0.25
        }

        return (basePoints * gradeMultiplier * repeatPenalty).toInt()
    }

    private fun calculateComboScore(combo: List<MoveDetected>): Int {
        val sumPoints = combo.sumOf { it.move.xpValue }
        val distinctMoves = combo.map { it.move.id }.distinct().size
        return sumPoints * distinctMoves
    }
}

sealed class MatchEvent {
    data class MoveScored(val move: MoveDefinition, val points: Int, val grade: Grade) : MatchEvent()
    data class ComboBailed(val moveCount: Int) : MatchEvent()
    data class ComboLanded(val score: Int, val moves: List<MoveDetected>) : MatchEvent()
    data class NamedComboTriggered(val combo: NamedCombo) : MatchEvent()
    data object SpecialActivated : MatchEvent()
}

enum class Grade { PERFECT, GREAT, GOOD, MISS }

data class NamedCombo(
    val id: String,
    val name: String,            // "Street Cruyff Special"
    val moveSequence: List<String>, // ["step_overs", "elastico"]
    val bonusPoints: Int
)
```

**Unit Tests (commonTest):**
```kotlin
class MatchScorerTest {
    @Test
    fun `scores move with grade multiplier`()
    @Test
    fun `applies diminishing returns for repeated moves`()
    @Test
    fun `combo multiplier rewards distinct moves`()
    @Test
    fun `bails combo when ball leaves grid`()
    @Test
    fun `banks combo when ball stops in center`()
    @Test
    fun `drains combo meter over time`()
    @Test
    fun `activates special when meter full`()
    @Test
    fun `detects named combos`()
}
```

### 3.4 Navigation (upgrade to Jetpack Compose Navigation)

**Current:** Simple sealed class navigation
**Proposed:** Jetpack Compose Navigation for better screen transitions, deep linking, and saved state

**Add Dependency:**
```kotlin
// shared/build.gradle.kts
commonMain.dependencies {
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.7.0-alpha07")
}
```

**Navigation Graph:**
```kotlin
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object GridSetup : Screen("grid_setup")
    data object ModeSelect : Screen("mode_select")
    data object MoveLibrary : Screen("move_library")
    data class MoveDetail(val moveId: String) : Screen("move_detail/{moveId}")
    data class LiveTracking(val drillId: String) : Screen("live_tracking/{drillId}")
    data class MoveSuccess(val moveId: String, val score: Int) : Screen("move_success/{moveId}/{score}")
    data object MatchSetup : Screen("match_setup")
    data object LiveMatch : Screen("live_match")
    data class MatchResults(val score: Int) : Screen("match_results/{score}")
    data object Leaderboard : Screen("leaderboard")
    data object Profile : Screen("profile")
}

@Composable
fun BallStarsNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        // ... etc
    }
}
```

### 3.5 Data Persistence (extend existing DataStore)

**Current:** Only `MatProfileRepository` uses DataStore
**Proposed:** Add `UserProgressRepository` for XP, level, unlocks, etc.

```kotlin
// commonMain
expect class UserProgressRepository {
    suspend fun saveProgress(progress: UserProgress)
    suspend fun loadProgress(): UserProgress?
    fun observeProgress(): Flow<UserProgress?>
}

@Serializable
data class UserProgress(
    val playerName: String = "Player",
    val avatarId: String = "avatar_1",
    val level: Int = 1,
    val xp: Int = 0,
    val totalMoves: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val badges: List<String> = emptyList(),
    val unlockedMoveIds: List<String> = emptyList(),
    val masteredMoveIds: List<String> = emptyList(), // 3-star rating
    val bestScores: Map<String, Int> = emptyMap(),
    val totalMatchesPlayed: Int = 0,
    val bestCombo: List<String> = emptyList()
)

// androidMain
actual class UserProgressRepository(private val context: Context) {
    private val Context.dataStore by preferencesDataStore("user_progress")
    private val PROGRESS_KEY = stringPreferencesKey("progress")

    actual suspend fun saveProgress(progress: UserProgress) {
        val json = Json.encodeToString(progress)
        context.dataStore.edit { it[PROGRESS_KEY] = json }
    }

    actual suspend fun loadProgress(): UserProgress? {
        return context.dataStore.data.map { prefs ->
            prefs[PROGRESS_KEY]?.let { Json.decodeFromString<UserProgress>(it) }
        }.first()
    }
}
```

**XP/Level System:**
- Level 1: 0-500 XP
- Level 2: 500-1200 XP
- Level 3: 1200-2500 XP
- Level N: XP = 500 * (level^1.5)
- Max level: 20

---

## 4. Package Layout

```
shared/src/commonMain/kotlin/venturewave/one/gridgames/
├── domain/
│   ├── tracker/
│   │   ├── GridTracker.kt              (interface)
│   │   ├── FakeGridTracker.kt          (test implementation)
│   │   ├── GridCalibrationState.kt
│   │   ├── BallState.kt
│   │   └── CellEvent.kt
│   ├── moves/
│   │   ├── MoveDefinition.kt
│   │   ├── PatternMatcher.kt
│   │   ├── MoveDetected.kt
│   │   └── BundledMoves.kt             (hardcoded move definitions)
│   ├── match/
│   │   ├── MatchScorer.kt
│   │   ├── MatchEvent.kt
│   │   ├── Grade.kt
│   │   └── NamedCombo.kt
│   └── progress/
│       ├── UserProgress.kt
│       ├── UserProgressRepository.kt   (expect)
│       └── XpCalculator.kt
├── ui/
│   ├── theme/
│   │   ├── BallStarsTheme.kt
│   │   ├── Color.kt
│   │   ├── Typography.kt
│   │   ├── Shape.kt
│   │   └── Spacing.kt
│   ├── components/
│   │   ├── buttons/
│   │   │   ├── PrimaryButton.kt
│   │   │   ├── SuccessButton.kt
│   │   │   └── OutlineButton.kt
│   │   ├── grid/
│   │   │   ├── NeonGrid.kt
│   │   │   ├── NeonGlowModifier.kt
│   │   │   └── GridOverlay.kt
│   │   ├── effects/
│   │   │   ├── BurstBackground.kt
│   │   │   ├── MotionTrail.kt
│   │   │   └── ParticleEffect.kt
│   │   ├── glyphs/
│   │   │   ├── MoveGlyph.kt
│   │   │   └── GlyphGenerator.kt
│   │   ├── stats/
│   │   │   ├── XpBar.kt
│   │   │   ├── StatChip.kt
│   │   │   ├── GradeStamp.kt
│   │   │   └── ComboMeter.kt
│   │   ├── nav/
│   │   │   ├── BallStarsBottomNav.kt
│   │   │   └── CameraFAB.kt
│   │   └── common/
│   │       ├── AvatarRing.kt
│   │       ├── ProgressDots.kt
│   │       └── StatusChip.kt
│   ├── screens/
│   │   ├── splash/
│   │   │   ├── SplashScreen.kt
│   │   │   └── SplashViewModel.kt
│   │   ├── onboarding/
│   │   │   ├── OnboardingScreen.kt
│   │   │   └── OnboardingPage.kt
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   └── components/
│   │   │       ├── ProfileCard.kt
│   │   │       └── RecentMovesList.kt
│   │   ├── grid/
│   │   │   ├── GridSetupScreen.kt      (expect/actual)
│   │   │   └── GridSetupViewModel.kt
│   │   ├── mode/
│   │   │   ├── ModeSelectScreen.kt
│   │   │   └── ModeCard.kt
│   │   ├── training/
│   │   │   ├── library/
│   │   │   │   ├── MoveLibraryScreen.kt
│   │   │   │   ├── MoveLibraryViewModel.kt
│   │   │   │   └── MoveTile.kt
│   │   │   ├── detail/
│   │   │   │   ├── MoveDetailScreen.kt
│   │   │   │   ├── MoveDetailViewModel.kt
│   │   │   │   └── AnimatedDemo.kt
│   │   │   ├── tracking/
│   │   │   │   ├── LiveTrackingScreen.kt  (expect/actual)
│   │   │   │   ├── LiveTrackingViewModel.kt
│   │   │   │   └── TrackingHUD.kt
│   │   │   └── success/
│   │   │       ├── MoveSuccessScreen.kt
│   │   │       └── MoveSuccessViewModel.kt
│   │   ├── match/
│   │   │   ├── setup/
│   │   │   │   ├── MatchSetupScreen.kt
│   │   │   │   └── MatchSetupViewModel.kt
│   │   │   ├── live/
│   │   │   │   ├── LiveMatchScreen.kt     (expect/actual)
│   │   │   │   ├── LiveMatchViewModel.kt
│   │   │   │   ├── CueLane.kt
│   │   │   │   └── MatchHUD.kt
│   │   │   └── results/
│   │   │       ├── MatchResultsScreen.kt
│   │   │       └── MatchResultsViewModel.kt
│   │   ├── leaderboard/
│   │   │   ├── LeaderboardScreen.kt
│   │   │   └── LeaderboardViewModel.kt
│   │   └── profile/
│   │       ├── ProfileScreen.kt
│   │       └── ProfileViewModel.kt
│   └── navigation/
│       ├── Screen.kt
│       └── BallStarsNavHost.kt
├── data/
│   └── ... (existing GridRepository, etc.)
└── util/
    ├── CoordinateMapper.kt             (CRITICAL: tested utility)
    └── SoundPlayer.kt                  (interface for audio hooks)

shared/src/androidMain/kotlin/venturewave/one/gridgames/
├── domain/
│   ├── tracker/
│   │   └── AndroidGridTracker.kt       (actual implementation)
│   └── progress/
│       └── UserProgressRepository.android.kt
└── ui/
    └── screens/
        ├── grid/
        │   └── GridSetupScreen.android.kt
        ├── training/
        │   └── tracking/
        │       └── LiveTrackingScreen.android.kt
        └── match/
            └── live/
                └── LiveMatchScreen.android.kt

shared/src/commonTest/kotlin/venturewave/one/gridgames/
├── domain/
│   ├── moves/
│   │   └── PatternMatcherTest.kt
│   ├── match/
│   │   └── MatchScorerTest.kt
│   └── progress/
│       └── XpCalculatorTest.kt
└── util/
    └── CoordinateMapperTest.kt
```

---

## 5. Screen Flow & Navigation Graph

```
┌─────────────────────────────────────────────────────────────┐
│                        SPLASH (1.2s)                        │
│                     Check first run?                        │
└──────────────┬──────────────────────────┬───────────────────┘
               │ First run                │ Returning user
               ▼                          ▼
      ┌────────────────┐          ┌──────────────┐
      │  ONBOARDING    │          │     HOME     │
      │  (4 pages)     │          │              │
      └────────┬───────┘          └──────┬───────┘
               │ Join BallStars          │
               └─────────────────────────┤
                                         │
                ┌────────────────────────┴────────────────────┐
                │                  HOME                       │
                │  - Profile card (avatar, level, XP)        │
                │  - Stats (moves, streak, badges)           │
                │  - Recent moves list                       │
                │  - [Play] → Grid Setup or Mode Select      │
                │                                             │
                │  Bottom Nav: Home · Moves · [FAB] · LB · P │
                └────────┬───────────┬────────┬─────┬─────┬──┘
                         │           │        │     │     │
          ┌──────────────┘           │        │     │     └───────────┐
          │                          │        │     │                 │
          ▼                          ▼        │     ▼                 ▼
   ┌─────────────┐          ┌──────────────┐ │ ┌────────────┐  ┌──────────┐
   │ GRID SETUP  │          │MOVE LIBRARY  │ │ │LEADERBOARD │  │ PROFILE  │
   │             │          │              │ │ │            │  │          │
   │ - Camera    │          │ - Move tiles │ │ │ - Local    │  │ - Avatar │
   │ - 9 touches │          │ - Filter by  │ │ │ - Family   │  │ - Stats  │
   │ - Confirm   │          │   difficulty │ │ │ - Best     │  │ - Badge  │
   └──────┬──────┘          └──────┬───────┘ │ │   scores   │  │   wall   │
          │                        │         │ └────────────┘  └──────────┘
          │ Grid confirmed         │ Tap tile│
          ▼                        ▼         │
   ┌─────────────┐          ┌──────────────┐ │
   │MODE SELECT  │          │ MOVE DETAIL  │ │
   │             │          │              │ │
   │ - Training  │          │ - Animated   │ │
   │ - Match     │          │   demo       │ │
   │   (locked)  │          │ - Tips       │ │
   └──┬────┬─────┘          │ - [Start     │ │
      │    │                │    Drill]    │ │
      │    │                └──────┬───────┘ │
      │    │                       │         │
      │    │ Match                 │ Start   │ Camera FAB
      │    │ unlocked              │ drill   │ (from anywhere)
      │    │                       ▼         │
      │    │             ┌──────────────────┐│
      │    │             │ LIVE TRACKING    ││
      │    │             │                  ││
      │    │             │ - Camera preview ││
      │    │             │ - Neon grid      ││
      │    │             │ - Target cells   ││
      │    │             │ - Ball trail     ││
      │    │             │ - Rep counter    ││
      │    │             │ - [Pause][Quit]  ││
      │    │             └─────────┬────────┘│
      │    │                       │         │
      │    │                       │ Drill   │
      │    │                       │ complete│
      │    │                       ▼         │
      │    │             ┌──────────────────┐│
      │    │             │  MOVE SUCCESS    ││
      │    │             │                  ││
      │    │             │ - Burst bg       ││
      │    │             │ - Move glyph     ││
      │    │             │ - "+100 XP"      ││
      │    │             │ - Star rating    ││
      │    │             │ - [Continue]     ││
      │    │             └─────────┬────────┘│
      │    │                       │         │
      │    │                       │ Continue│
      │    │                       └─────────┤
      │    │                                 │
      │    │ Training                        │
      │    └─────────────────────────────────┘
      │
      │ Match mode
      ▼
┌─────────────┐
│MATCH SETUP  │
│             │
│ - Duration  │
│ - Setlist   │
│ - [Start]   │
└──────┬──────┘
       │
       │ Start match
       ▼
┌─────────────┐
│ LIVE MATCH  │
│             │
│ - Camera    │
│ - Cue lane  │
│ - HUD       │
│ - Combo     │
│   meter     │
│ - Special   │
│   meter     │
└──────┬──────┘
       │
       │ Time up
       ▼
┌─────────────┐
│  MATCH      │
│  RESULTS    │
│             │
│ - Score     │
│ - Best      │
│   combo     │
│ - Grade     │
│   breakdown │
│ - XP earned │
│ - [Play     │
│    Again]   │
│ - [Home]    │
└─────────────┘
```

---

## 6. Critical Technical Challenges

### 6.1 Coordinate Mapping (Preview ↔ Analysis)

**Problem:**
- Camera analysis: 4:3 aspect (e.g., 1080x1440 or 640x480)
- PreviewView: FILL_CENTER on screen (e.g., 1080x2400)
- Grid overlay must render at correct positions accounting for crop/scale

**Solution:**
```kotlin
// util/CoordinateMapper.kt
object CoordinateMapper {
    /**
     * Maps a point from analysis image space to preview display space.
     *
     * @param analysisPoint Point in analysis image (0 to analysisWidth/Height)
     * @param analysisSize Size of analysis image (e.g., 1080x1440)
     * @param previewSize Size of PreviewView on screen (e.g., 1080x2400)
     * @param scaleType PreviewView.ScaleType (FILL_CENTER assumed)
     * @return Point in preview display space
     */
    fun mapAnalysisToPreview(
        analysisPoint: Offset,
        analysisSize: Size,
        previewSize: Size,
        scaleType: ScaleType = ScaleType.FILL_CENTER
    ): Offset {
        // Calculate scale to fill preview while maintaining aspect
        val scaleX = previewSize.width / analysisSize.width
        val scaleY = previewSize.height / analysisSize.height
        val scale = maxOf(scaleX, scaleY) // FILL_CENTER uses max

        // Calculate scaled analysis size
        val scaledWidth = analysisSize.width * scale
        val scaledHeight = analysisSize.height * scale

        // Calculate crop offset (centered)
        val offsetX = (previewSize.width - scaledWidth) / 2f
        val offsetY = (previewSize.height - scaledHeight) / 2f

        // Transform point
        val x = analysisPoint.x * scale + offsetX
        val y = analysisPoint.y * scale + offsetY

        return Offset(x, y)
    }

    /**
     * Inverse: maps preview point to analysis space (for touch handling).
     */
    fun mapPreviewToAnalysis(
        previewPoint: Offset,
        analysisSize: Size,
        previewSize: Size,
        scaleType: ScaleType = ScaleType.FILL_CENTER
    ): Offset {
        val scaleX = previewSize.width / analysisSize.width
        val scaleY = previewSize.height / analysisSize.height
        val scale = maxOf(scaleX, scaleY)

        val scaledWidth = analysisSize.width * scale
        val scaledHeight = analysisSize.height * scale

        val offsetX = (previewSize.width - scaledWidth) / 2f
        val offsetY = (previewSize.height - scaledHeight) / 2f

        val x = (previewPoint.x - offsetX) / scale
        val y = (previewPoint.y - offsetY) / scale

        return Offset(x, y)
    }
}

// Unit test
class CoordinateMapperTest {
    @Test
    fun `maps center point correctly`() {
        val analysisSize = Size(640f, 480f) // 4:3
        val previewSize = Size(1080f, 2400f) // 9:20
        val center = Offset(320f, 240f)

        val mapped = CoordinateMapper.mapAnalysisToPreview(center, analysisSize, previewSize)

        // Center should map to center
        assertEquals(540f, mapped.x, 0.1f)
        assertEquals(1200f, mapped.y, 0.1f)
    }

    @Test
    fun `roundtrip preserves position`() {
        val analysisSize = Size(1080f, 1440f)
        val previewSize = Size(1080f, 2400f)
        val original = Offset(540f, 720f)

        val toPreview = CoordinateMapper.mapAnalysisToPreview(original, analysisSize, previewSize)
        val backToAnalysis = CoordinateMapper.mapPreviewToAnalysis(toPreview, analysisSize, previewSize)

        assertEquals(original.x, backToAnalysis.x, 0.1f)
        assertEquals(original.y, backToAnalysis.y, 0.1f)
    }
}
```

**Usage in all camera screens:**
```kotlin
// GridSetupScreen.android.kt
val mappedGridPoints = calibratedGrid.targets.map { target ->
    CoordinateMapper.mapAnalysisToPreview(
        analysisPoint = Offset(target.x, target.y),
        analysisSize = Size(tensorWidth, tensorHeight),
        previewSize = Size(previewWidth, previewHeight)
    )
}

Canvas(modifier = Modifier.fillMaxSize()) {
    mappedGridPoints.forEach { point ->
        drawCircle(color = Color.Cyan, radius = 20f, center = point)
    }
}
```

### 6.2 Performance (60 FPS UI + 28 FPS Detection)

**Challenge:**
- Ball detection runs at ~28 FPS
- UI must stay at 60 FPS for smooth animations
- Recomposing entire screen per camera frame kills performance

**Solution:**
```kotlin
// Separate state holders for camera data vs UI
@Stable
class CameraOverlayState {
    var ballPosition by mutableStateOf<Offset?>(null)
    var gridPoints by mutableStateOf<List<Offset>>(emptyList())
    var currentTargetIndex by mutableStateOf(0)

    // Only these change per frame, isolated from UI tree
}

@Composable
fun LiveTrackingScreen() {
    val overlayState = remember { CameraOverlayState() }
    val uiState by viewModel.uiState.collectAsState() // Throttled, 2-4 FPS updates

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview (AndroidView, no recomposition)
        AndroidView(factory = { PreviewView(it) })

        // Overlay (isolated, only recomposes when overlayState changes)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw grid, ball, targets
            // Reads from overlayState, which updates at detection FPS
        }

        // UI elements (throttled updates)
        TrackingHUD(uiState) // Only updates when score/timer changes
    }

    // Update overlay state from detection results (28 FPS)
    LaunchedEffect(Unit) {
        viewModel.detectionResults.collect { results ->
            overlayState.ballPosition = results.firstOrNull()?.boundingBox?.center
        }
    }
}
```

**Throttle UI updates:**
```kotlin
fun <T> Flow<T>.throttleLatest(periodMillis: Long): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmitTime >= periodMillis) {
            emit(value)
            lastEmitTime = currentTime
        }
    }
}

// Usage
val uiState by viewModel.uiState
    .throttleLatest(250) // 4 FPS for UI updates
    .collectAsState(initial = UiState())
```

### 6.3 Reduced Motion

**Implementation:**
```kotlin
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        val resolver = context.contentResolver
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
fun BurstBackground(reducedMotion: Boolean = rememberReducedMotion()) {
    if (reducedMotion) {
        // Static gradient
        Box(modifier = Modifier.background(Brush.radialGradient(...)))
    } else {
        // Animated burst with particles
        AnimatedBurstWithParticles()
    }
}
```

---

## 7. Conflicts & Risks

### 7.1 Existing Code Conflicts

| Issue | Current State | Proposed Solution | Risk |
|---|---|---|---|
| **Navigation system** | Simple sealed class | Migrate to Jetpack Compose Navigation | **MEDIUM** - Need to preserve existing screen state, add migration path |
| **Scoring logic split** | GameScreen has inline logic, BallPositionViewModel unused | Consolidate into MatchScorer, deprecate BallPositionViewModel | **LOW** - BallPositionViewModel not wired |
| **Pattern model** | TrainingPattern has only cell sequence | Extend to MoveDefinition with XP, unlock level, glyph | **LOW** - Additive change, keep TrainingPattern for compat |
| **Grid calibration** | 9-point manual touch | Keep as-is, wrap in GridTracker interface | **NONE** - Wrapper only |
| **Camera aspect** | Hardcoded 4:3 | Keep 4:3, enforce in interface docs | **NONE** - Preserving existing |
| **No persistence** | GameResult not saved | Add UserProgressRepository | **LOW** - New code path |
| **Theme already exists** | GridGamesTheme with similar colors | Replace with BallStarsTheme, migrate screens | **MEDIUM** - Need to update all existing screens |

### 7.2 Technical Risks

| Risk | Impact | Mitigation |
|---|---|---|
| **Coordinate mapping bugs** | Grid/ball positions wrong, game unplayable | **Unit tests** for CoordinateMapper, visual debug mode to show analysis vs preview coords |
| **Performance regression** | Frame drops, laggy UI | **Baseline FPS measurement** before changes, Canvas-based overlays, throttled UI updates, performance tests on Pixel 9 |
| **Font loading issues** | Missing fonts, fallback to system | Use Compose Resources with bundled fonts, test on emulator and device |
| **Pattern matching false positives** | Moves detected incorrectly | **Unit tests** with edge cases, tunable timeout/tolerance, visual feedback during dev |
| **Match scoring bugs** | Combo math wrong, exploit combos | **Comprehensive unit tests**, manual QA with scripted sequences |
| **Memory leaks from ViewModels** | App crashes after extended play | Proper lifecycle cleanup, leak detection in CI |
| **4:3 aspect not enforced** | Someone changes to 16:9, detection breaks | **Runtime assertion** in AndroidGridTracker, crash in debug with clear error |

### 7.3 UX Risks

| Risk | Impact | Mitigation |
|---|---|---|
| **Touch targets too small** | Kids can't hit buttons from distance | **Enforce 56dp minimum** in design system, test on real device propped 2m away |
| **Text too small** | Unreadable at distance | **16sp minimum body text**, high contrast, test at distance |
| **Animations distracting** | Kids miss visual feedback | **Reduced motion support**, clear visual hierarchy, test with kids |
| **Onboarding too long** | Users skip before understanding | **4 pages max**, "Skip" button on page 2+, remember preference |
| **Match mode too complex** | Combo system confusing | **Tutorial mode**, simplified HUD, clear visual feedback, playtesting |

---

## 8. Implementation Phases

### Phase 1: Plan & Design System (1-2 days)
**Deliverables:**
- ✅ `docs/ui-plan.md` (this document)
- `docs/assets.md` (asset inventory and specs)
- **Awaiting approval before proceeding**

### Phase 2: Theme & Components (2-3 days)
**Tasks:**
1. Add Lilita One + Nunito fonts to Compose Resources
2. Implement `BallStarsTheme` with complete color/typography/shape system
3. Build reusable components:
   - Buttons (Primary, Success, Outline)
   - `NeonGrid` + `neonGlow` modifier
   - `BurstBackground`
   - `MoveGlyph` + `GlyphGenerator`
   - `XpBar`, `StatChip`, `GradeStamp`, `ComboMeter`
   - `BallStarsBottomNav` with Camera FAB
4. Create `@Preview` gallery screen showing all components
5. **Commit:** "feat: add BallStars theme and component library"

**Acceptance:**
- Gallery screen shows all components with BallStars styling
- Fonts load correctly
- No hardcoded colors/dimensions

### Phase 3: Navigation Shell with FakeGridTracker (3-4 days)
**Tasks:**
1. Add Jetpack Compose Navigation dependency
2. Implement `FakeGridTracker` with scripted sequences
3. Create `Screen` sealed class and `BallStarsNavHost`
4. Build screens in commonMain (UI only, no camera):
   - Splash
   - Onboarding (4 pages with pager)
   - Home (with bottom nav)
   - Mode Select
   - Move Library
   - Move Detail
   - Move Success
   - Empty states
5. Wire all navigation with `FakeGridTracker` for testing
6. Add placeholder illustrations (gradient + neon grid + ball)
7. **Commit:** "feat: add navigation shell and core screens"

**Acceptance:**
- Full flow navigable on emulator without camera
- FakeGridTracker emits scripted cell events
- All screens use BallStarsTheme
- Bottom nav works with camera FAB

### Phase 4: Grid Setup with AndroidGridTracker (2-3 days)
**Tasks:**
1. Implement `GridTracker` interface in commonMain
2. Implement `AndroidGridTracker` wrapping existing detection
3. Build `CoordinateMapper` utility with unit tests
4. Reskin Grid Setup screen (expect/actual):
   - Camera preview
   - Neon grid overlay using CoordinateMapper
   - Status chips (Grid found, Good light, Ball in view)
   - Confirm/Redo buttons
   - Success burst animation
5. Test coordinate mapping on Pixel 9 with real tape grid
6. **Commit:** "feat: integrate grid calibration with AndroidGridTracker"

**Acceptance:**
- Grid overlay renders at correct positions
- CoordinateMapper unit tests pass
- Grid calibration flow unchanged
- Success burst plays on confirm

### Phase 5: Training Loop End-to-End (3-4 days)
**Tasks:**
1. Implement `MoveDefinition` and `BundledMoves` (map from TrainingPattern)
2. Implement `PatternMatcher` with unit tests
3. Implement `UserProgressRepository` (expect/actual with DataStore)
4. Build Live Tracking screen (expect/actual):
   - Camera preview with neon grid
   - Target cell highlighting (pulsing gold)
   - Ball marker with motion trail
   - Rep counter and timer
   - Status card ("Tracking…" / "Ball lost")
   - Pause/Quit controls
5. Build Move Success screen with XP animation
6. Wire move library → detail → live tracking → success → XP persisted
7. Test on device with real ball
8. **Commit:** "feat: implement training loop with move detection"

**Acceptance:**
- PatternMatcher unit tests pass
- Move detection works on device
- XP saves to DataStore and persists across restarts
- Live tracking runs at 60 FPS UI + 28 FPS detection
- No frame drops

### Phase 6: Match Mode (4-5 days)
**Tasks:**
1. Implement `MatchScorer` with comprehensive unit tests
2. Implement `NamedCombo` definitions
3. Build Match Setup screen (duration, setlist picker)
4. Build Live Match screen (expect/actual):
   - Cue lane with scrolling move glyphs
   - Match HUD (score, combo chain, meters, timer)
   - Grade stamp (PERFECT/GREAT/GOOD/MISS)
   - Combo bail/bank/special effects
5. Build Match Results screen
6. Wire match setup → live → results
7. Test combo system, scoring math, named combos
8. **Commit:** "feat: implement match mode with combo system"

**Acceptance:**
- MatchScorer unit tests pass (100% coverage of scoring rules)
- Combo system works on device
- Diminishing returns, banking, special, named combos all functional
- Grade stamps appear with correct timing

### Phase 7: Polish & Accessibility (2-3 days)
**Tasks:**
1. Add sound hooks (interface with no-op default)
2. Implement reduced motion support
3. Accessibility pass:
   - Content descriptions on all icon buttons
   - WCAG AA contrast check
   - Touch target size audit (56dp/48dp)
4. Performance testing:
   - Measure baseline FPS before UI changes
   - Measure FPS with new overlays
   - Optimize if >2 FPS regression
5. Build illustrations or refine placeholders
6. Final device testing on Pixel 9
7. **Commit:** "polish: add sound hooks, accessibility, performance tuning"

**Acceptance:**
- FPS regression <2 FPS vs baseline
- Reduced motion mode works
- All buttons have content descriptions
- Touch targets meet 56dp/48dp requirements
- Builds with `./gradlew build` with no warnings

---

## 9. Assets Inventory (to be detailed in docs/assets.md)

**Placeholder specifications:**

| Asset | Size | Format | Purpose |
|---|---|---|---|
| `onboarding_1.png` | 1080x2400 | PNG | Onboarding page 1 illustration |
| `onboarding_2.png` | 1080x2400 | PNG | Onboarding page 2 illustration |
| `onboarding_3.png` | 1080x2400 | PNG | Onboarding page 3 illustration |
| `onboarding_4.png` | 1080x2400 | PNG | Onboarding page 4 illustration |
| `hero_tracking.png` | 1080x1080 | PNG | Live tracking background/hero |
| `empty_state_ball.png` | 400x400 | PNG | Empty state icon (ball with question mark) |
| `app_icon.png` | 512x512 | PNG | App launcher icon |
| `avatar_1.png` ... `avatar_8.png` | 200x200 | PNG | Avatar picker options |
| `badge_*.png` | 120x120 | PNG | Badge icons (first move, 10 streak, etc.) |

**Font files:**
- `LilitaOne-Regular.ttf` (from Google Fonts)
- `Nunito-Regular.ttf`, `Nunito-SemiBold.ttf`, `Nunito-Bold.ttf`

**All assets will be placed in:**
- `shared/src/commonMain/composeResources/drawable/`
- `shared/src/commonMain/composeResources/font/`

---

## 10. Open Questions

1. **Onboarding persistence:** Should we use DataStore to remember "onboarding complete" flag? Or check if UserProgress exists?
   - **Recommendation:** Check if `UserProgressRepository.loadProgress()` returns non-null. If null, show onboarding.

2. **Avatar picker:** Should avatars be illustrated characters or abstract icons?
   - **Recommendation:** Illustrated cartoon kids (match onboarding art style), 8 options with diverse representation.

3. **Leaderboard future:** Is online multiplayer planned?
   - **Assumption:** Keep local-only for now, design interface to support future backend.

4. **Sound effects:** Which library? (Compose Multiplatform has no official audio API)
   - **Recommendation:** Use platform-specific `MediaPlayer` (Android) / `AVAudioPlayer` (iOS) behind `SoundPlayer` interface. No-op defaults for now.

5. **Move unlock progression:** Should moves unlock automatically by level, or require completing previous moves?
   - **Recommendation:** Level-based unlock (simple), with 3-star mastery for completionists.

6. **Match mode unlock:** Require 3 learned moves or 3 mastered (3-star) moves?
   - **Recommendation:** 3 learned (completed drill once), lower barrier to unlock headline feature.

---

## 11. Success Metrics

**Definition of Done:**
- [ ] All screens visually match `ballstars-reference.png` design
- [ ] Existing ball detection frame rate maintained (±2 FPS)
- [ ] Full flow runnable on emulator with `FakeGridTracker`
- [ ] `PatternMatcher`, `MatchScorer`, `CoordinateMapper` have 100% test coverage
- [ ] No hardcoded colors/dimensions in screens
- [ ] Builds with `./gradlew build` with zero warnings
- [ ] Touch targets meet 56dp/48dp requirements
- [ ] WCAG AA contrast on all text
- [ ] Reduced motion support functional
- [ ] Content descriptions on all icon buttons
- [ ] XP/level/unlocks persist across app restarts

**Performance Targets:**
- UI: 60 FPS on Pixel 9
- Detection: 28-30 FPS maintained
- Memory: No leaks after 30-minute session
- Startup: Splash → Home in <2s on device

---

## 12. Timeline Estimate

**Total:** ~17-26 days (3.5-5 weeks) for one developer

| Phase | Days | Notes |
|---|---|---|
| 1. Plan & Design System | 1-2 | **AWAITING APPROVAL** |
| 2. Theme & Components | 2-3 | Parallel with asset creation |
| 3. Navigation Shell | 3-4 | Can demo all screens without camera |
| 4. Grid Setup Integration | 2-3 | First real camera integration |
| 5. Training Loop | 3-4 | Core gameplay functional |
| 6. Match Mode | 4-5 | Most complex feature |
| 7. Polish & Accessibility | 2-3 | Final QA and tuning |
| **Buffer** | 3-5 | Contingency for unknowns |

**Critical Path:**
1. Approval of this plan
2. Theme + Components (blocks all UI)
3. AndroidGridTracker (blocks camera screens)
4. CoordinateMapper (blocks all overlays)
5. PatternMatcher (blocks move detection)
6. MatchScorer (blocks match mode)

---

## 13. Next Steps

**Immediate:**
1. ✅ Submit `docs/ui-plan.md` for review
2. Create `docs/assets.md` with detailed asset specs and placeholder generation plan
3. **Wait for approval before writing any code**

**After Approval:**
1. Start Phase 2: Theme & Components
2. Set up fonts in Compose Resources
3. Build component gallery screen
4. Request asset creation handoff (illustrations, avatars, badges)

---

## Appendix A: Comparison of Current vs. Proposed

| Aspect | Current | Proposed |
|---|---|---|
| **Navigation** | Sealed class, manual stack | Jetpack Compose Navigation |
| **Patterns** | TrainingPattern (9 hardcoded) | MoveDefinition (extensible, XP, unlock) |
| **Scoring** | Inline in GameScreen | MatchScorer (tested, reusable) |
| **Grid Detection** | Direct ViewModel access | GridTracker interface |
| **Ball Tracking** | Direct ViewModel access | GridTracker interface |
| **Persistence** | MatProfile only | UserProgress (XP, level, unlocks) |
| **Theme** | GridGamesTheme (similar colors) | BallStarsTheme (refined palette) |
| **Typography** | System fonts | Lilita One + Nunito |
| **Coord Mapping** | Inline in GameScreen | CoordinateMapper utility (tested) |
| **Move Detection** | Manual streak/combo tracking | PatternMatcher (event-driven) |
| **Match Mode** | Not implemented | Full Tony Hawk × Just Dance system |
| **Screens** | 7 screens | 15+ screens (onboarding, library, match, etc.) |

---

**End of Plan**

**Status:** Awaiting stakeholder approval to proceed to Phase 2.
