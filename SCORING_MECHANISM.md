# Scoring Mechanism (current behavior, as implemented)

This documents how scoring actually works in the code today — not how it was
originally spec'd (see `TRAINING_AND_GAME_LOGIC.md` for the original design
doc, which predates the current pattern set and doesn't reflect several bugs
and platform divergences described below). Everything here was confirmed by
reading `GameScreen.android.kt`, `GameScreen.ios.kt`, `TrainingPattern.kt`,
`ScoreScreen.kt`, and `PatternRepository.kt` directly.

## Overview

Score is **not** an accumulating per-hit counter. It's a formula computed
from two inputs — elapsed time and accuracy — re-evaluated from scratch both
continuously during play (for the live HUD) and once more at game end (for
the final `GameResult`). Landing more hits only affects score indirectly, by
changing `accuracy`.

## Lifecycle

```
TIME_SELECTION (pick 30s/60s)
  -> COUNTDOWN (10 -> 1 -> GO)
  -> ACTIVE (per-frame hit test against camera detections, timer ticking down)
  -> COMPLETE (timer hits 0 -> GameResult built -> onGameComplete(result))
  -> ScoreScreen (displays the result)
```

The game only ever ends via the countdown timer reaching `0` — there's no
early-finish/success condition. **Nothing is persisted after `ScoreScreen`**
— navigating away loses the result entirely (see [Persistence](#persistence---currently-none)).

## Hit detection

Source: `shared/src/androidMain/kotlin/venturewave/one/gridgames/ui/GameScreen.android.kt`, ~lines 196–341 (a `LaunchedEffect(detectionResults, calibratedGrid)` that runs on every new camera-detection frame while `gamePhase == ACTIVE`).

A detection counts as a hit on the current target only if:
- its confidence `score >= 0.20f`, and
- its bounding-box center — after transforming calibration-viewport space → game-aperture space → detector-tensor space — falls inside the current target box.

When a hit registers (and it's not a repeat of the same target already
counted this loop, tracked via `hitPositionsInIteration`):
- `hitCount++`
- `currentTargetIndex++` (advances to the next position in `targetSequence`)
- `currentStreak++`, `bestStreak` updated if beaten
- every 5th hit: `stars++`
- streak ≥ 5 → 2x combo, streak ≥ 10 → 3x combo
- completing a full loop through `targetSequence` triggers a 5-second "power-up" HUD state

When the ball is **not** detected inside the target, nothing is scored as a
miss. The only consequence is `currentStreak` resets to `0` and `combo`
resets to `1x`.

**`missCount` is declared (`var missCount by remember { mutableStateOf(0) }`,
line 154) and read in several places, but is never incremented anywhere in
the file** — confirmed via grep, the only writes to it are the initial `0`
and passing it straight through into `GameResult`. There is no timeout
penalty, no wrong-target penalty, no miss detection path at all. This means
`accuracy` (`hitCount / (hitCount + missCount) * 100`) can only ever be
exactly `0%` or exactly `100%` for the entire session.

## The score formula

`shared/src/androidMain/kotlin/venturewave/one/gridgames/ui/GameScreen.android.kt`, lines 1646–1652:

```kotlin
private fun calculateScore(totalTimeMs: Long, accuracy: Float): Int {
    val baseScore = 1000
    val timeBonus = maxOf(0, (30000 - totalTimeMs) / 100).toInt() // Bonus for finishing under 30s
    val accuracyBonus = (accuracy * 10).roundToInt()

    return baseScore + timeBonus + accuracyBonus
}
```

Breaking down each term:

- **`baseScore = 1000`** — flat, always awarded regardless of performance.
- **`timeBonus`** — pegged to a hardcoded `30000`ms (30s) threshold, regardless of whether the player chose a 30s or 60s session. Since games always run the full selected duration (no early finish), this term is `0` for the entire second half of any 60s game, and decays from `~300` down to `~0` over a 30s game. In practice it has almost no real effect on score — it reads like a leftover constant from before the 30s/60s duration selector existed.
- **`accuracyBonus = round(accuracy * 10)`** — because `accuracy` can only be `0` or `100` (see above), this term is only ever `0` or exactly `1000`.
- **`difficultyLevel`** (1–5, present on every `TrainingPattern`) is never referenced anywhere in the scoring path — it's metadata only, not a multiplier.
- The **live HUD score** (`GameScreen.android.kt` line 592, `val liveScore = calculateScore(elapsedForHud, accuracy)`) is the exact same formula, continuously re-evaluated from elapsed-time-so-far — not an incrementing running total.

## HUD-only mechanics that don't affect score

`currentStreak`, `bestStreak`, `combo` (1x/2x/3x), `stars` (+1 every 5 hits),
and `powerUpActive`/`powerUpTimeRemaining` are all tracked purely for
in-game visual feedback. **None of them are read by `calculateScore`, and
none of them are fields on `GameResult`** — they vanish the moment the game
ends. They also only exist on Android; iOS has none of this.

## `GameResult` fields

`shared/src/commonMain/kotlin/venturewave/one/gridgames/model/TrainingPattern.kt`:

```kotlin
data class GameResult(
    val pattern: TrainingPattern,
    val score: Int,
    val totalTime: Long,
    val hitCount: Int,
    val missCount: Int,
    val accuracy: Float,
    val patternsCompleted: Int = 0
)
```

Built at game end (`GameScreen.android.kt`, lines 410–420):

```kotlin
onGameComplete(
    GameResult(
        pattern = pattern,
        score = score,
        totalTime = totalTime,
        hitCount = hitCount,
        missCount = missCount,
        accuracy = accuracy,
        patternsCompleted = patternsCompleted
    )
)
```

`patternsCompleted = currentTargetIndex / pattern.targetSequence.size` —
full completed loops through the pattern's sequence only (e.g. Right Peak is
`[5,7,4,5]`; hitting `5→7→4→5` once counts as 1). No partial credit for an
in-progress loop.

## iOS divergence

`shared/src/iosMain/kotlin/venturewave/one/gridgames/ui/GameScreen.ios.kt` reimplements the same hit-detection/coordinate-transform logic, but its score formula is simpler and has silently diverged from Android's (line 200):

```kotlin
val score = 1000 + (accuracy * 10).toInt()
```

Differences from Android's `calculateScore()`:
- **No `timeBonus` term at all** — dropped entirely.
- Uses `.toInt()` truncation instead of Android's `.roundToInt()`.
- No dedicated `calculateScore` function — the formula is inlined directly at the game-end call site.
- No live HUD score shown during play at all (the ACTIVE/PAUSED HUD only shows Time, Hits, and Step X/Y).
- No streak/combo/stars/power-up system exists on iOS.
- Same `missCount`-never-incremented issue (line 75 declares it, never incremented) — accuracy is still only ever 0% or 100%.

Given Android's `timeBonus` is usually near-zero anyway, the practical score
gap between platforms is usually small — but it's a real, unintentional
formula mismatch, not just a UI/polish difference.

## `ScoreScreen` presentation

`shared/src/commonMain/kotlin/venturewave/one/gridgames/ui/ScoreScreen.kt` displays, directly from `GameResult`, with no further derived metrics:

- **Score** — raw `result.score` integer, no formatting/grading.
- **Time** — `result.totalTime / 1000f` formatted to 1 decimal place ("Xs").
- **Accuracy** — `result.accuracy` formatted to 0 decimal places ("X%").
- **Hits** / **Misses** — `result.hitCount` / `result.missCount` (misses always `0`).
- **Patterns Completed** — `result.patternsCompleted`.

`formatDecimal()` is a small hand-rolled fixed-decimal formatter (used
because `String.format` is JVM-only and doesn't compile for iOS) — purely a
display helper. **There is no star rating, grade, or badge shown here** —
despite the in-game `stars` counter existing during play, it's never passed
into `GameResult` or shown on this screen.

## Persistence — currently none

`PatternRepository.updatePlayerProgress()` and the `PlayerProgress` data
class (`personalBest`, `timesPlayed`, `lastPlayedTimestamp`) exist and are
fully implemented, but **are never called anywhere in the live app flow**.
`App.kt` routes `GameResult` straight from `GameScreen` to `ScoreScreen`
(`onGameComplete = { result -> navigationState.navigateToScore(result) }`)
with no repository call in between. Every `GameResult` is lost the moment
the player navigates away from `ScoreScreen` — no personal best, no
times-played count, nothing is ever written to disk after a game.

There's also a second, parallel, Realm-based progress implementation
(`TrainingPatternRepository`/`PlayerProgressEntity`), but its only caller is
`database_disabled/DatabaseInitializer.kt.disabled` — excluded from
compilation. Along with further disabled SQLDelight files
(`GameResultManager.kt.disabled`, `GameSessionRepository.kt.disabled`,
`PlayerRepository.kt.disabled`, `GameSession.sq`, `PlayerProfile.sq`,
`PlayerStats.sq`), this confirms an abandoned prior attempt at persistence —
worth knowing so it isn't mistaken for a working code path.

Downstream of this, both score-display surfaces outside `ScoreScreen` are
non-functional:
- **Best Scores** (Android) is a literal placeholder: `Text("Leaderboard Coming Soon!")`, with a comment noting "Will be implemented in Phase 6."
- **`ProfileScreen`**'s "Games Played" / "Best Score" stat cards are hard-coded literal strings (`"12"`, `"95%"`), not derived from any real data.

## Known issues (quick reference)

- `missCount` is dead on both platforms — never incremented, so `accuracy` can only be 0% or 100%, which also caps `accuracyBonus` to only ever being `0` or `1000`.
- No miss/timeout/wrong-target penalty mechanic exists at all, despite `GameResult.missCount`'s docstring describing it as "Number of missed targets."
- Android's `timeBonus` is effectively inert — hardcoded to a 30s threshold regardless of the chosen session length, and games never finish early.
- iOS's score formula has diverged from Android's (no `timeBonus` term, truncation vs. rounding) — same gameplay performance can yield different scores per platform.
- `difficultyLevel` on `TrainingPattern` is unused for scoring — display metadata only.
- Streak/combo/stars/power-up mechanics are Android-only, HUD-only, and don't affect `score` or persist in `GameResult`.
- No progress is ever persisted — personal bests, times played, Best Scores, and Profile stats are either dead code paths or hard-coded placeholders.
