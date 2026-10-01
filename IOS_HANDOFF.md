# iOS porting — handoff for a machine with working Xcode

This file exists because the Mac this work was done on (Intel, macOS 26.7,
Xcode 15.4) **cannot open `iosApp/iosApp.xcodeproj`** — it was saved in a
newer Xcode project format (`objectVersion = 77`, using
`PBXFileSystemSynchronizedRootGroup`, an Xcode 16+ feature). Everything
below was written and verified to **Kotlin-compile and link** via Gradle
command-line tasks, but **never run on a Simulator or device**, because
that requires actually opening/building the Xcode project — which this
machine can't do.

If you're reading this on a machine with a current Xcode (or a rented cloud
Mac), you should be able to pick up from exactly here.

## What's done (verified: compiles + links for both `iosArm64` and `iosSimulatorArm64`)

1. **`shared/src/iosMain/kotlin/venturewave/one/gridgames/ui/CameraCapture.ios.kt`**
   — `AVCaptureSession`-based camera capture + a Compose `UIKitView` wrapper
   (`IosCameraPreview`), mirroring the role of Android's CameraX
   `PreviewView`/`ImageAnalysis` (`GameScreen.android.kt`'s `CameraPreview`,
   `ScanTargets3Screen.kt`'s `CameraPreviewLayer`). Configures
   `kCVPixelFormatType_32BGRA` output, as `GMLImage`/`TFLObjectDetector`
   require.

2. **`shared/src/iosMain/kotlin/venturewave/one/gridgames/detection/BallDetector.ios.kt`**
   — ball detection via Google's `TensorFlowLiteTaskVision` pod's
   `TFLObjectDetector`, the direct iOS counterpart to Android's
   `BallDetectorHelper.kt` (`org.tensorflow.lite.task.vision.detector.ObjectDetector`
   — same API shape: `createFromFile+options` → `detect(image)` →
   `[category/score/box]`).

3. **`shared/src/iosMain/kotlin/venturewave/one/gridgames/ui/GameScreen.ios.kt`**
   — replaces the old "iOS Game Implementation Pending" stub with a real
   (visually simplified) game loop: camera preview, live ball detection,
   hit-testing against a `CalibratedGrid` (reusing the exact same
   touch-tap calibration model `GridRepository`/`CalibratedGrid`/`GridTarget`
   that Android's `GameScreen.android.kt` already uses — unaffected by this
   session's removal of the old, dead automated CV grid-detector), a
   countdown/timer/score flow, and `onGameComplete`/`onBackPressed` wiring.
   **Visual design is deliberately simpler than Android's BallStars HUD** —
   this pass prioritized a correct, wired core loop over matching Android's
   polish. Porting the visual styling/animations is a reasonable next step
   once the core loop is confirmed working on real hardware.

4. **CocoaPods integration**: `iosApp/Podfile` added, `pod install` run
   (creates `iosApp/Pods/`, `iosApp/iosApp.xcworkspace`). Interestingly,
   CocoaPods' own Ruby `xcodeproj` gem parser was able to read/modify
   `project.pbxproj` even though Apple's Xcode 15.4 couldn't — so
   `pod install` succeeded on this machine despite the Xcode-open blocker.

5. **Kotlin/Native cinterop**: `shared/src/nativeInterop/cinterop/TensorFlowLiteTaskVision.def`
   binds the pod's Objective-C headers; `shared/build.gradle.kts` wires it
   into both iOS targets. `gradle.properties` has
   `kotlin.mpp.enableCInteropCommonization=true` added (needed for the
   intermediate `iosMain` source set to see this per-target cinterop).

## What's NOT done / genuinely unverified

- **Nothing has run on a Simulator or device.** All of the above is
  "the Kotlin compiler and linker accept this," not "this actually works."
  There will very likely be real bugs once it's actually run — camera
  permission handling, the exact capture resolution/orientation, whether
  the hit-detection coordinate math is right, whether the model file is
  even found, etc.
- **`ball-tracker.tflite` is not yet bundled into the iOS app.** It's at
  `androidApp/src/main/assets/ball-tracker.tflite` — needs to be added as
  a resource to `iosApp` in Xcode (drag into the project, "Copy items if
  needed," add to the `iosApp` target) so
  `NSBundle.mainBundle.pathForResource("ball-tracker", ofType: "tflite")`
  finds it.
- **Camera permission (`NSCameraUsageDescription`)**: needs adding to
  `iosApp/iosApp/Info.plist`, or the app will crash on first camera access.
- **Grid calibration is NOT ported to iOS.** `PlatformSpecificScanTargets3`'s
  iOS actual (`iosMain/kotlin/venturewave/one/gridgames/ScanTargets3ScreenPlatform.kt`)
  is still the "iOS Grid Calibration Coming Soon" placeholder. Without it,
  `GridRepository.getGrid()` is always `null` on iOS, so `GameScreen.ios.kt`'s
  hit-detection path can't be exercised at all yet — you'll see the camera
  preview and the "no calibrated grid yet" message, but no target overlay.
  Porting the touch-tap calibration screen (`ScanTargets3Screen.kt`'s
  `ScanTargets2Screen` composable, `com.ballstars.mobile` package) is the
  natural next piece of work, and can reuse `IosCameraPreview` directly —
  it doesn't need `BallDetector` at all, just camera + touch handling.
- **GPU/Neural Engine acceleration**: Android's `BallDetectorHelper` can use
  a GPU delegate; `TFLComputeSettings` on iOS only exposes CPU thread count
  and an optional Core ML (Neural Engine) delegate — no direct GPU switch.
  CPU is what's wired up now; Core ML delegate is a possible follow-up once
  this is running and can be benchmarked.
- Two known Kotlin/Native cinterop quirks hit while writing this (both
  documented inline in code comments, in case they recur elsewhere):
  - `NSDate.timeIntervalSince1970` doesn't resolve in this project's
    `iosMain` compilation despite the selector existing in Foundation's
    klib metadata (cause unclear) — worked around via POSIX `time()` in
    `PlatformTime.ios.kt`.
  - A cinterop `.def` file's `headerFilter` must actually match your
    headers' filenames, or **silently** zero symbols get bound (no error -
    the klib just ends up empty). This cost real debugging time; if you
    add more cinterops, skip `headerFilter` unless you've confirmed the
    glob matches.

## Build/run steps on this machine

```bash
# Verify the shared Kotlin framework still builds (should already pass):
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared:linkDebugFrameworkIosArm64

# Open the WORKSPACE (not the .xcodeproj directly - CocoaPods requires this):
open iosApp/iosApp.xcworkspace
```

Then in Xcode: select an iPhone Simulator, Run. The Xcode build has an
existing Run Script phase that calls
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` automatically, so
the Kotlin framework gets embedded without extra steps.

If anything doesn't compile/link that did here, check:
`iosApp/Podfile` version pins (`TensorFlowLiteTaskVision ~> 0.4.3`), and
the absolute paths in `shared/src/nativeInterop/cinterop/TensorFlowLiteTaskVision.def`
and `shared/build.gradle.kts`'s `tfliteFrameworksDir` — both currently
hardcode this checkout's path
(`/Users/simonparkhouse/Documents/organisation/venturewave/grid-games/grid-games-mobile`)
and **will need updating** if the repo lives somewhere else on the new
machine.

## Getting this repo onto a cloud Mac

This is a git repo with full history (`git log`), but **no remote is
configured** (`git remote -v` is empty) and `gh` isn't installed here, so
a GitHub remote couldn't be created automatically. Before moving to a cloud
Mac, either:
- Push to a new GitHub/GitLab remote yourself and `git clone` there, or
- Transfer the directory some other way (zip, rsync, AirDrop, etc.)

**Uncommitted at the time of writing** (the file paths above are correct
today, but if you're on a fresh clone/transfer, `git status` first to
confirm these changes actually made it across):

```
shared/build.gradle.kts
gradle.properties
shared/src/iosMain/kotlin/venturewave/one/gridgames/ui/CameraCapture.ios.kt       (new)
shared/src/iosMain/kotlin/venturewave/one/gridgames/detection/BallDetector.ios.kt (new)
shared/src/iosMain/kotlin/venturewave/one/gridgames/ui/GameScreen.ios.kt          (rewritten)
shared/src/nativeInterop/cinterop/TensorFlowLiteTaskVision.def                    (new)
iosApp/Podfile                                                                    (new)
iosApp/Pods/ , iosApp/iosApp.xcworkspace                                          (new, from pod install)
IOS_HANDOFF.md                                                                    (this file)
```

## Prompt to paste into a fresh Claude Code session on the cloud Mac

```
I'm continuing iOS work on a Kotlin Multiplatform + Compose Multiplatform
project (BallStars Grid Games). Read IOS_HANDOFF.md at the repo root first
- it explains exactly what's done, what's not, and two real cinterop
gotchas I hit. Everything described there was verified to compile and
link via Gradle command-line tasks only (this machine's earlier Xcode
couldn't open iosApp.xcodeproj at all) - nothing has actually been run on
a Simulator or device yet, so treat all of it as "should work" rather than
"works."

Please:
1. Run `pod install` in iosApp/ if iosApp/Pods/ isn't already present or
   looks stale.
2. Open iosApp/iosApp.xcworkspace (not .xcodeproj) in Xcode.
3. Fix the hardcoded absolute paths in
   shared/src/nativeInterop/cinterop/TensorFlowLiteTaskVision.def and
   shared/build.gradle.kts (search for "podsRoot" / "tfliteFrameworksDir")
   - they point at the old machine's checkout path and need to match
   wherever this repo actually lives now.
4. Add ball-tracker.tflite (currently only in
   androidApp/src/main/assets/) as a bundled resource on the iosApp Xcode
   target, and add NSCameraUsageDescription to iosApp/iosApp/Info.plist.
5. Try building and running on an iPhone Simulator, then fix whatever
   actually breaks - camera permission flow, capture resolution/
   orientation, the hit-detection coordinate math in GameScreen.ios.kt,
   whatever else comes up. I have no way to have tested any of this
   myself, so go in expecting real bugs, not just last-mile polish.
6. Grid calibration itself isn't ported to iOS yet (still a placeholder
   screen) - IOS_HANDOFF.md explains why that blocks testing the
   hit-detection path specifically, and that IosCameraPreview from
   CameraCapture.ios.kt is directly reusable for building that screen too.
```
