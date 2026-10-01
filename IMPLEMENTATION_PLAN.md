# Grid Games Mobile - Implementation Plan
## Android-First Development with iOS Stubs

**Status:** Fresh KMP project scaffolded and verified
**Date:** 20 Sep 2026
**Approach:** Option 1 - Focus on Android, stub iOS for later completion

---

## Current Environment Status

### ✅ Completed Setup
- **Project Structure:** Fresh KMP project from kmp.new
  - Module: `:androidApp` (equivalent to `:composeApp`)
  - Shared code: `:shared` with `commonMain`, `androidMain`, `iosMain`
- **Android Build:** ✅ Verified working
  - `./gradlew :androidApp:assembleDebug` - builds clean
  - Tested on Pixel 9a (Android 17) - installs and runs
- **iOS Framework:** ✅ Builds via Gradle
  - `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` - compiles successfully
  - ⚠️ Full iOS app requires Xcode 16+ (Intel Mac limitation)
- **SDK Configuration:** ✅ Complete
  - Android SDK at `/usr/local/share/android-commandlinetools`
  - JDK 17+ installed
  - adb installed and working

### ⚠️ iOS Limitations (Current Hardware)
- **Xcode:** v15.4 (macOS 26.7, Intel x86_64)
- **Project Format:** Requires Xcode 16+ (objectVersion 77)
- **Strategy:** Build iOS framework via Gradle, defer full iOS UI testing
- **Future Options:** GitHub Actions (free), cloud Mac, or M-series hardware

---

## Architecture Decisions (from Playbook)

### Framework
**Compose Multiplatform** - Already scaffolded and working
- Device consistency already proven
- Same UI code runs on both platforms
- No need for React Native

### Backend
**None for MVP** - Bundle 2-3 patterns locally
- Add Firebase later, only after core loop is proven
- No Amplify/GraphQL in v1

### ML Stack
- **Model:** Int8 QAT via MediaPipe Model Maker
- **Runtime:** LiteRT + MediaPipe Tasks
- **Targets:** CPU-guaranteed, GPU/NPU opportunistic

### Project Structure
```
shared/
  src/
    commonMain/kotlin/          # Shared: UI, scoring logic, bundled patterns
    androidMain/kotlin/         # Android: CameraX, MediaPipe Tasks
    iosMain/kotlin/             # iOS: Stubs now, AVFoundation + MediaPipe later
```

---

## Phased Implementation Plan

### Phase 1: ✅ Empty Shell (COMPLETE)
**Goal:** Verify both targets build before real code

**Status:** DONE
- ✅ Android builds and runs on Pixel 9a
- ✅ iOS framework builds via Gradle
- ✅ Source sets properly structured

**Verification:** Already confirmed via installation on physical device

---

### Phase 2: Port Game Logic (NEXT)
**Goal:** Move scoring/target-sequence logic to `commonMain`

**Tasks:**
1. Copy from existing project → `shared/src/commonMain/kotlin/`:
   - `BallPositionViewModel.kt` - scoring algorithm, cooldown logic
   - `Training.kt`, `TargetBox.kt`, `Path.kt` - state machine
2. **Adapt:** Replace Amplify data source with bundled local patterns (2-3 hardcoded)
3. **Keep:** Scoring logic exactly as-is (already debugged in production)

**Files to Reference:**
- Copy: BallPositionViewModel.kt (light adapt)
- Adapt: Training/TargetBox/Path (swap data source)
- Reference only: DetectorViewModel.kt (queue mechanism)
- Leave behind: TrainingRepo.kt, Amplify config

**Verification Test:**
```kotlin
@Test
fun `scoring advances exactly once per pattern completion`() {
    // Feed hitTarget() scripted ball positions
    // Assert score increments once per full pattern
    // Matches BSDiag HIT/SCORE log behavior
}
```

**AI Assistant Prompt:**
```
Port BallPositionViewModel.kt, Training.kt, TargetBox.kt, and Path.kt
from [existing project path] into shared/src/commonMain/kotlin/.
Keep the scoring/target-sequence state machine logic exactly as it is
(findNextTargetBox, clearAllHits, hitTarget, cooldown timing) - it's
already been debugged through real production issues. Replace the
Amplify/TrainingRepo-backed data source with a bundled local data
source (2-3 hardcoded training patterns as Kotlin data, no network
calls). Write a unit test that feeds hitTarget() a scripted sequence
of fake ball positions and asserts the score increments exactly once
per full pattern completion.
```

---

### Phase 3: Retrain Model
**Goal:** QAT int8 model via MediaPipe Model Maker

**Process:**
1. Use MediaPipe Model Maker (TFLite Model Maker is broken on modern Python)
2. Apply Quantization-Aware Training (QAT)
3. Export directly to int8
4. Output: `ball-tracker-qat-int8.tflite`

**Reference:** Full walkthrough in Rebuild Blueprint report

**Verification:**
- COCO metrics from `evaluate()` - sanity check only
- Real acceptance test comes in Phase 4 (device logs)

---

### Phase 4: Android Camera + Detection
**Goal:** CameraX + MediaPipe Tasks on Android

**Tasks:**
1. Port DetectorViewModel queue architecture to `androidMain`
2. Adapt for MediaPipe Tasks Vision's async API:
   - Use `RunningMode.LIVE_STREAM`
   - Call `detectAsync()` with result listener (not old synchronous `detect()`)
3. Add dependency: `com.google.mediapipe:tasks-vision`
4. Load `ball-tracker-qat-int8.tflite` from assets
5. Keep BSDiag-style structured logging (FRAME, INFER, BALL_DETECT events)

**Critical Difference:**
MediaPipe Tasks uses **async callbacks**, not blocking calls - restructure queue consumer accordingly

**Verification:**
- Capture BSDiag session on physical device
- Compute `resultCount=0` miss rate
- **Target:** Better than 63-70% baseline from previous logs
- Not just "seems to work" - actual metrics

**AI Assistant Prompt:**
```
Port DetectorViewModel.kt's frame-queue architecture from [old project]
into shared/src/androidMain/kotlin/, adapting it to call MediaPipe
Tasks Vision's ObjectDetector (com.google.mediapipe:tasks-vision)
instead of old TFLite Task Library. Use RunningMode.LIVE_STREAM with
detectAsync() and a result listener - this is asynchronous, unlike
old synchronous detect() call, so restructure the queue consumer
around the callback. Load ball-tracker-qat-int8.tflite from assets.
Keep BSDiag-style structured logging (FRAME, INFER, BALL_DETECT,
QUEUE_DEQUEUE events) so detection-rate measurement works the same.
```

---

### Phase 5: Minimal UI (Android Focus)
**Goal:** Home → pattern picker → camera/game → score

**Requirements:**
- **Adaptive Layout:** WindowSizeClass, weight-based sizing from day one
- **Why:** Previous bug fixed by switching from fixed-dp to adaptive tools
- **Screens:** Home, pattern picker, game (camera + targets), score
- **Out of scope:** Shop, league, profile, auth

**Device Coverage:**
- Small screen (phone)
- Large screen/tablet class
- **No fixed dp sizing** - weight-based only

**Verification:**
- Run on 2 device profiles: small-screen + tablet-class
- Confirm 9-target grid never clips/overflows on either
- Test on physical Pixel 9a

**AI Assistant Prompt:**
```
Build a minimal Compose Multiplatform UI in shared/src/commonMain/kotlin/:
home screen listing bundled training patterns, game screen showing
camera preview with current target highlighted and live score, using
WindowSizeClass and weight-based layout instead of any fixed dp sizing
- this project previously had a real bug where custom pixel-based
scaling made UI inconsistent across Android screen sizes, fixed by
switching to these adaptive tools, so don't reintroduce fixed-size
layout. No shop, league, profile, or auth screens - explicitly out
of scope for MVP.
```

---

### Phase 6: iOS Stubs (Deferred Implementation)
**Goal:** Stub out iOS camera/detection layer for future completion

**Current Approach:**
1. Create expect/actual interfaces in `commonMain`
2. Implement Android actuals in Phase 4
3. **iOS actuals:** Stub implementations that compile but don't function
   - Return mock/empty data
   - Log "iOS implementation pending"
   - Allow full project to build

**Future Implementation (when Xcode 16+ available):**
```
Implement iOS actual for camera/detection expect/actual interface
using AVFoundation for capture (auto-exposure only) and
MediaPipeTasksVision CocoaPod for detection, loading same
ball-tracker-qat-int8.tflite. Match Android's option shape
(LIVE_STREAM mode, same maxResults/scoreThreshold) and produce
equivalent structured logs for comparable detection-rate measurement.
```

**Why Stub Now:**
- Keeps architecture consistent
- iOS framework builds successfully
- Can develop and test Android thoroughly
- iOS can be completed when compatible hardware available

---

### Phase 7: Low-End Device Verification
**Goal:** Verify CPU-only performance on mid-range hardware

**Test Device Target:**
- Snapdragon 6/7-series or Dimensity mid-tier
- Representative of 2023+ mid-range Android

**Verification:**
- Same BSDiag session-log methodology
- Confirm acceptable frame rate
- CPU-only path (not relying on GPU/NNAPI delegate)
- Detection rate in acceptable range

---

## Development Workflow

### Build Commands
```bash
# Android
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug

# iOS framework (verify shared code compiles)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Tests
./gradlew :shared:testDebugUnitTest
```

### Current Working Directory
```
/Users/simonparkhouse/Documents/organisation/venturewave/grid-games/grid-games-mobile
```

### Key Files
- **local.properties** - Already configured with SDK path (don't commit)
- **shared/src/commonMain** - All shared game logic goes here
- **shared/src/androidMain** - CameraX, MediaPipe, platform-specific Android
- **shared/src/iosMain** - Stubs for now, full implementation later

---

## Dependencies to Add

### Android (build.gradle.kts)
```kotlin
// Phase 4
implementation("com.google.mediapipe:tasks-vision:0.10.14") // Check latest
implementation("androidx.camera:camera-camera2:1.3.0")
implementation("androidx.camera:camera-lifecycle:1.3.0")
implementation("androidx.camera:camera-view:1.3.0")
```

### iOS (when implementing)
```ruby
# Podfile
pod 'MediaPipeTasksVision'
```

---

## Success Metrics

### Phase 2
- ✅ Unit tests pass with scripted ball positions
- ✅ Scoring logic matches BSDiag log behavior

### Phase 4
- ✅ Detection miss rate < 63-70% baseline
- ✅ BSDiag logs show consistent detection

### Phase 5
- ✅ UI renders correctly on small + large screens
- ✅ No clipping/overflow on either profile
- ✅ App runs smoothly on Pixel 9a

### Phase 7
- ✅ Acceptable frame rate on mid-range device
- ✅ CPU-only path performs adequately

---

## What This Plan Doesn't Remove

A fresh project and current toolchain fix version debt and tooling friction, **not model accuracy**.

**Phase 3's retrain** (with data targeting tonight's failure modes) is still the critical piece that must work for this MVP to be worth shipping.

---

## Next Immediate Steps

1. **Start Phase 2:** Port game logic to `commonMain`
2. **Locate old project:** Need path to existing Ball Tracking project files
3. **Create unit tests:** Verify scoring logic in isolation
4. **Bundle patterns:** Create 2-3 hardcoded training patterns

**Ready to proceed?** Let me know the path to your existing project files and we can start Phase 2!
