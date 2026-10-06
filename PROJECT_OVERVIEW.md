# BallStars Grid Games — Project Overview

This file is written so a fresh machine or a fresh Claude Code session (no
prior context) can understand what this app is, how it's built, and how to
pick up work on it — including finishing the iOS port. Read this first;
it links out to the more detailed docs that already exist in the repo
rather than repeating them.

## What the app is

A football/soccer skills-training mobile game. The player places their
phone so its camera looks down at a 3x3 grid of targets taped/marked on
the ground, calibrates that grid once, then does ball-control tricks
(moving a ball between the 9 boxes in specific sequences) while the phone
detects which box the ball is in and scores the player in real time.

Three play modes, reachable from the Home screen:
- **Setup Zone** — calibrate the 3x3 grid (one-time, in-memory only — see
  "Known quirks" below).
- **Training Mode** — pick one named pattern (e.g. "Right Peak") and
  repeat it; scored on hits/misses/clean-lap bonuses.
- **Game Mode ("Freestyle")** — no pre-selected pattern. The player moves
  freely around the grid; the app recognizes *any* of the 13 bundled
  patterns whenever its sequence completes, and chains consecutive
  recognized tricks into a combo for bonus scoring. This is the newer,
  more actively-developed mode.

Both Training and Game Mode require a calibrated grid; Setup Zone does
not (it's how you create one).

## Tech stack

- **Kotlin Multiplatform (KMP)** + **Compose Multiplatform** — one shared
  UI/logic module (`shared/`) targeting Android and iOS.
- Android is the primary, fully-working platform. iOS is a genuine but
  incomplete port — see "iOS port status" below.
- Gradle (`./gradlew`), version catalog in `gradle/libs.versions.toml`.
- No remote persistence/backend — everything is local/in-memory or on
  device. Realm and SQLDelight dependencies exist in `build.gradle.kts`
  but are largely unused scaffolding (SQLDelight's `sqldelight { }` config
  block is commented out); don't assume a working database layer exists
  without checking current usage first.

## Module/package layout (`shared/src/`)

- `commonMain/` — shared across both platforms: all screens (Compose
  UI), navigation (`navigation/NavigationState.kt`), the grid/pattern data
  model, Freestyle scoring engine, theme. This is where most logic lives
  and where new cross-platform features should go.
- `androidMain/` — Android `actual`s: camera capture (CameraX), ball
  detection (TensorFlow Lite), platform sound (`ToneGenerator`), the
  touch-tap grid calibration screen.
- `iosMain/` — iOS `actual`s: camera capture (AVFoundation), ball
  detection (TensorFlowLiteTaskVision pod via cinterop), platform sound
  (`AudioServicesPlaySystemSound`). Grid calibration is **not yet ported**
  (placeholder screen) — this is the main gap.
- `commonTest/` — pure-Kotlin unit tests (e.g. `FreestyleEngineTest.kt`).
  **Note**: there is no configured JVM/Android test runner in this
  environment and no working local iOS simulator either, so tests here
  have historically been verified by compiling
  (`./gradlew :shared:compileTestKotlinIosSimulatorArm64` or
  `:shared:allTests`) and hand-tracing logic, not by actually running
  them. Check whether a real test runner is available on your machine
  before assuming otherwise.

## Core data model

- **`GridTarget`** (`model/GridTarget.kt`): one of the 9 calibrated boxes.
  Has a **0-8 `index`** and pixel bounds (`x`, `y`, `width`, `height`)
  from calibration, plus `contains(x, y)` hit-testing.
- **`CalibratedGrid`**: the 9 `GridTarget`s plus calibration viewport
  metadata. `findTargetAt(x, y)` maps a detected ball position to a box.
- **`GridRepository`** (`data/GridRepository.kt`): an in-memory singleton
  `object` holding the current `CalibratedGrid?` (`saveGrid`/`getGrid`/
  `hasGrid`/`clearGrid`). **Not persisted to disk** — killing the process
  (including every `adb install -r`) wipes it, so recalibration is needed
  after every fresh install during testing. If you need calibration to
  survive app restarts, this is the file to add real persistence to.
- **`TrainingPattern`** (`model/TrainingPattern.kt`): a named trick —
  `targetSequence: List<Int>` is a sequence of **1-9 numpad-style box
  numbers** (see layout diagram below), e.g. Right Peak =
  `[5, 7, 4, 5]`. 13 patterns are bundled in
  `TrainingPattern.getBundledPatterns()`.
- **Important index mismatch to know about**: `GridTarget.index` is
  **0-8**, but every pattern (`TrainingPattern.targetSequence`) and the
  Freestyle engine's `onBoxEntered(boxNumber: Int)` use **1-9**. The
  conversion point is `grid.findTargetAt(x, y)?.index` → **`+ 1`** to get
  the pattern-space box number. Search `findTargetAt` call sites
  (`GameScreen.android.kt`, `FreestyleGameScreen.android.kt`) for the
  exact spot before touching this — getting the +1 wrong silently breaks
  all pattern matching.

Numpad box layout (this is the convention `targetSequence` numbers use,
*not* `GridTarget.index` which is this minus 1):
```
1 - 2 - 3
|   |   |
4 - 5 - 6
|   |   |
7 - 8 - 9
```
(Cross-reference against `GridTarget.kt`'s own doc comment, which
documents the 0-8 `index` layout — same grid, numbers offset by 1.)

## How a game session actually works (both modes, same mechanism)

1. Camera preview + ball detector (TFLite on Android, TFLiteTaskVision
   pod on iOS) runs continuously, producing a ball pixel position per
   frame.
2. `calibratedGrid.findTargetAt(ballX, ballY)` maps that position to a
   `GridTarget`, `+1` to get the 1-9 box number.
3. A debounce (`lastEnteredBoxNumber`/`lastHitTargetNumber`) only acts
   when the detected box number **changes** — this is deliberate so a
   single dwell doesn't fire repeated hits, and so a no-detection frame
   doesn't reset the debounce (only a genuinely different box does).
4. **Training Mode**: checks the new box against the single active
   pattern's next expected step; see `TRAINING_AND_GAME_LOGIC.md` and
   `SCORING_MECHANISM.md` for the full hit/miss/bonus scoring rules (not
   repeated here since those docs are current and detailed).
5. **Freestyle/Game Mode**: feeds the box number into
   `FreestyleChainState.onBoxEntered()` (`model/FreestyleEngine.kt`),
   which checks the rolling hit buffer against *all* 13 patterns at once
   via `matchTrick`. See "Freestyle Mode internals" below for how
   matching and chaining work — this is the most algorithmically
   interesting part of the codebase and the thing most likely to need
   care if ported or modified.

## Freestyle Mode internals (`model/FreestyleEngine.kt`)

This file is pure Kotlin (no Compose/platform dependency) and fully
unit-testable — see `commonTest/.../FreestyleEngineTest.kt` for the
authoritative examples of intended behavior; read those tests before
changing matching logic, they encode real decisions, not incidental
coverage.

- **Gap-tolerant matching** (`matchesWithGapTolerance`, `MAX_TRANSIT_GAP =
  2`): on a 3x3 grid, moving the ball in a straight line between two
  non-adjacent boxes physically grazes the box(es) geometrically between
  them, producing spurious extra hits. Rather than filtering those by
  timing (an earlier approach that was explicitly rejected — see the
  comment at `MAX_TRANSIT_GAP`'s declaration), `matchTrick` walks the
  pattern backward against the hit buffer and tolerates up to 2
  non-matching stray entries between each required step, while still
  requiring the match to end exactly on the buffer's most recent entry
  (a trick is recognized the instant it completes, not retroactively).
- **Chaining**: `FreestyleChainState` keeps a rolling buffer
  (`CHAIN_BUFFER_SIZE = 16`, sized for worst-case gap-tolerant lookback on
  the longest bundled pattern). After a match, it keeps only the buffer's
  last element (the shared pivot box) rather than clearing fully — most
  bundled patterns start and end on the same box, so chained tricks flow
  through that shared pivot without an artificial "leave and re-enter."
  This is also why a player can safely abandon a pattern mid-attempt and
  start a completely different one, or move to a totally different
  starting box for the next trick — the matcher always anchors to the
  buffer's tail and only looks back as far as the specific pattern it's
  checking needs, so stale/leftover digits earlier in the buffer don't
  interfere (regression-tested explicitly in
  `matchTrick_abandonedAttemptDoesNotInterfereWithALaterDifferentPattern`
  and `matchTrick_newPatternFromADifferentStartingBoxStillMatches`).
- **Scoring**: `chainMultiplier` — 1st trick x1, 2nd (different from
  previous) x1.5, 3rd x2, 4th+ x3 (cap); repeating the immediately
  preceding trick collapses the multiplier back to x1. A "flow bonus"
  (`FLOW_BONUS = 250`) fires when the last 3 landed tricks are all
  distinct. Milestones fire once per chain at `chainLength` 3/5/8.
- **Chain timeout**: a chain banks itself (`bankChain()`) automatically
  after `CHAIN_TIMEOUT_MS` (3s) of no new box-entry activity (ticker lives
  in the screen composable, not in `FreestyleEngine.kt` itself).
- **Session-wide pattern tally**: `completionCountsByName` tracks total
  completions per pattern name across the *whole session*, independent of
  `bankChain()` resets (which only clear the in-progress chain's
  bookkeeping). This feeds `FreestyleResult.patternBreakdown` shown on the
  end-of-session summary screen.

## Platform-specific pieces (what you'd need to redo per-platform)

| Concern | Android | iOS |
|---|---|---|
| Camera capture | CameraX (`GameScreen.android.kt`'s `CameraPreview`, `ScanTargets3Screen.kt`'s `CameraPreviewLayer`) | AVFoundation (`ui/CameraCapture.ios.kt`'s `IosCameraPreview`) — done |
| Ball detection | TFLite `ObjectDetector` (`detection/BallDetectorHelper.kt`, model files in `androidApp/src/main/assets/*.tflite`) | `TFLObjectDetector` via CocoaPods + cinterop (`detection/BallDetector.ios.kt`) — done, unverified on device |
| Grid calibration (touch-tap 9-point setup) | `ui/ScanTargets3Screen.kt` — done | `ScanTargets3ScreenPlatform.kt` is still a placeholder — **not ported** |
| Platform audio (`ComboSound.kt` expect/actual) | `ToneGenerator` | `AudioServicesPlaySystemSound` — done |
| Game loop screens | `GameScreen.android.kt`, `FreestyleGameScreen.android.kt` — full BallStars HUD/visual polish | `GameScreen.ios.kt`, `FreestyleGameScreen.ios.kt` — wired and compiling, deliberately simplified visuals, **never run on simulator/device** |

## iOS port status — read `IOS_HANDOFF.md` for the full detail

`IOS_HANDOFF.md` (repo root) is a previous, still-accurate handoff written
specifically for continuing the iOS camera/ball-detection port on a
machine with working Xcode (the machine this was developed on could only
*compile and link* the Kotlin side via Gradle CLI tasks — its Xcode was
too old to even open `iosApp.xcodeproj`, which was saved in a newer
project format). Don't duplicate that file's content when working on iOS
— read it directly. In short, as of that doc:
- Camera capture, ball detection, and the core game loop compile/link for
  both iOS targets but have **never actually run** on a simulator or
  device.
- Grid calibration is the single biggest missing piece — until it's
  ported, `GridRepository.getGrid()` is always `null` on iOS, so the game
  loop can't be exercised past "camera preview, no targets."
  `IosCameraPreview` is directly reusable for building it; it needs no
  ball-detection, just camera + touch handling, mirroring
  `ScanTargets3Screen.kt`'s Android implementation.
- Two real Kotlin/Native cinterop gotchas are documented there
  (`NSDate.timeIntervalSince1970` not resolving; `.def` file
  `headerFilter` silently zeroing out bound symbols if it doesn't match).
- Hardcoded absolute paths (this machine's checkout path) in
  `shared/build.gradle.kts` and the cinterop `.def` file will need
  updating on a new machine/checkout location.
- `ball-tracker.tflite` isn't yet bundled as an iOS resource;
  `NSCameraUsageDescription` isn't yet in `iosApp/Info.plist`.

Everything added in the session *after* that handoff was written
(Freestyle Mode's scoring engine, countdown/GO sounds, the Home-screen
grid-required gating/pulsing UI) lives in `commonMain` or has both
`androidMain`/`iosMain` actuals already — it does not add new iOS gaps
beyond what `IOS_HANDOFF.md` already describes, **except** that
`FreestyleGameScreen.ios.kt` and `GameScreen.ios.kt`'s countdown-beep
wiring and pattern-breakdown display were added without any device
testing, same caveat as the rest of the iOS game loop.

## Other docs in this repo (read these for depth, not repeated here)

- `IOS_HANDOFF.md` — iOS port handoff, see above.
- `TRAINING_AND_GAME_LOGIC.md` — Training Mode's detailed hit/miss/combo
  rules.
- `SCORING_MECHANISM.md` — scoring formulas and history across both modes.
- `GAME_MECHANICS_PROPOSAL.md` — design rationale for Freestyle/Game Mode.
- `GRID_DETECTION_PROBLEM_REPORT.md`, `OPTIMIZATION_LOG.md` — historical
  debugging logs for the old automated (non-touch-tap) grid-detection
  approach; useful for context on why calibration is touch-tap-based now,
  not necessarily current.
- `DATABASE_GUIDE.md`, `DATABASE_IMPLEMENTATION_SUMMARY.md` — Realm/
  SQLDelight notes; cross-check against actual current usage in
  `build.gradle.kts` before trusting as current state, per the "largely
  unused scaffolding" note above.

## Known quirks / gotchas worth knowing before changing things

- **`GridRepository` and `SetupPromptState` are both in-memory-only
  singletons** — no disk persistence. Expect calibration state to reset
  on every process death (including every reinstall during testing).
- **The 0-8 vs 1-9 grid-index offset** (see Core data model above) is the
  single easiest thing to get wrong when touching detection/matching
  code.
- **This repo had its git history rewritten once already** (a large
  accidentally-committed TensorFlow training-data zip, ~120MB, was
  stripped out by squashing to a new root commit) — if you ever see a
  push rejected as non-fast-forward against `origin/main`, check whether
  history was rewritten again before force-pushing or merging; `git log
  --oneline --all` and comparing commit parents is the first thing to
  check, not `git pull --rebase` on autopilot.
- There is no configured JVM/Android unit test runner and no working
  local iOS simulator in the environment this was developed in — don't
  assume `./gradlew test` or similar actually executes anything without
  checking first; `commonTest` code has so far only ever been verified by
  compiling + hand-tracing.

## Build commands that are known to work

```bash
# Android debug build
./gradlew assembleDebug -q

# iOS shared framework — compiles/links only, does NOT run/test anything
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64 -q

# Compile-check commonTest code (no execution — see "Known quirks")
./gradlew :shared:compileTestKotlinIosSimulatorArm64
```

Verify these still work on whatever machine you're on — the above was
confirmed as of the commit history in this repo, not guaranteed forever.
