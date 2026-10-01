# Phase 4: Android Camera + Target Detection - Implementation Summary

## Overview

Phase 4 implements real-time target detection on Android using CameraX and MediaPipe Tasks Vision. This replaces the deprecated TFLite Task Library with the modern MediaPipe async API while preserving the proven frame-queue architecture and BSDiag logging.

## What Was Built

### 1. TargetDetectorHelper ([TargetDetectorHelper.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/detection/TargetDetectorHelper.kt))

**Modern MediaPipe Tasks Vision wrapper** with async detection API.

**Key Features:**
- Uses `RunningMode.LIVE_STREAM` for video/camera input
- Async `detectAsync()` instead of blocking `detect()`
- Result listener callback for async results
- BSDiag-style structured logging preserved
- Automatic delegate selection (CPU/GPU)

**Critical Differences from Old Code:**
```kotlin
// OLD (TFLite Task Library - synchronous)
val results = objectDetector?.detect(tensorImage)  // Blocks until complete
listener.onResults(results)

// NEW (MediaPipe Tasks Vision - asynchronous)
objectDetector?.detectAsync(mpImage, timestamp)  // Returns immediately
// Results arrive later via callback configured in setup
```

**BSDiag Logging Events:**
- `TARGET_DETECTOR_INIT` - Detector initialization
- `FRAME` - Frame submitted for detection
- `TARGET_DETECT` - Detection result received
- `TARGET_DETECTOR_CLOSE` - Detector cleanup

### 2. TargetDetectorViewModel ([TargetDetectorViewModel.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/detection/TargetDetectorViewModel.kt))

**Frame queue architecture** ported from ballstars, adapted for async API.

**Architecture:**
```
Camera → Analyzer (copy frame) → Queue → Processor Thread → detectAsync()
                                                                    ↓
                                                            (async callback)
                                                                    ↓
                                                            onResults() → UI
```

**Queue Design Rationale:**
- **Problem**: CameraX `STRATEGY_KEEP_ONLY_LATEST` drops frames if detection is busy
- **Solution**: Analyzer copies and enqueues all frames (fast, keeps up with camera)
- **Trade-off**: Queue can back up if detection can't keep pace (bounded by capacity)
- **Measurement**: `QUEUE_DEQUEUE` logging shows exactly how much lag exists

**Key Components:**
- `frameQueue`: LinkedBlockingQueue<QueuedFrame>(capacity=6)
- `queueProcessorExecutor`: Dedicated thread for dequeuing and calling detectAsync()
- `analysisExecutor`: CameraX analyzer runs here
- Bitmap allocation per frame (queued) + immediate recycling after detectAsync()

**Async API Adaptation:**
The queue processor now pumps frames into `detectAsync()` as fast as it can dequeue them, rather than waiting for synchronous results:

```kotlin
// Queue processor loop (adapted for async)
while (!stopQueueProcessor) {
    val frame = frameQueue.poll(200, TimeUnit.MILLISECONDS) ?: continue

    // Log queue delay
    Log.i("BSDiag", "QUEUE_DEQUEUE queueDelayMs=${now - frame.capturedTs}")

    // Async call - returns immediately
    targetDetectorHelper.detectAsync(frame.bitmap, frame.rotationDegrees)

    // Recycle immediately (MediaPipe has copied data)
    frame.bitmap.recycle()
}
```

### 3. Build Configuration ([shared/build.gradle.kts](shared/build.gradle.kts))

**Dependencies Added:**
```kotlin
androidMain.dependencies {
    // MediaPipe Tasks Vision for target detection
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // CameraX dependencies
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
}
```

## UI Implementation Complete

### Camera Preview Screen

Created [TargetDetectionScreen.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/ui/TargetDetectionScreen.kt) with:

**Features:**
- Full-screen camera preview using CameraX
- Real-time detection overlay rendering
- Bounding boxes with color-coded confidence levels:
  - Green: > 80% confidence
  - Yellow: 60-80% confidence
  - Red: < 60% confidence
- Detection stats overlay (target count, inference time, FPS)

**Architecture:**
```
CameraPermissionRequired (permission handling)
  └─ TargetDetectionScreen (main UI)
      ├─ CameraPreview (AndroidView with PreviewView)
      ├─ DetectionOverlay (Canvas drawing bounding boxes)
      └─ DetectionStats (performance metrics)
```

### Permission Handling

Created [CameraPermission.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/ui/CameraPermission.kt):
- Runtime permission request using ActivityResultContracts
- User-friendly permission denied screen
- Automatic permission check on app launch

**AndroidManifest.xml updated** with:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
```

### App Integration

Updated [App.kt](shared/src/commonMain/kotlin/venturewave/one/gridgames/App.kt) with expect/actual pattern:
- **Android:** Shows TargetDetectionScreen with camera detection
- **iOS:** Shows placeholder (target detection not implemented yet)

Main app now launches directly into target detection mode on Android.

## Next Steps

### 1. Add TFLite Model to Assets

Once `target-detector-qat-int8.tflite` export completes:

```bash
# Copy model to Android assets
mkdir -p androidApp/src/main/assets
cp resources/TensorFlow/TFModelOutput/target-detector-qat-int8.tflite \
   androidApp/src/main/assets/
```

### 2. Testing & Verification

**On Physical Device:**
1. Build and deploy to Android device
2. Point camera at grid training patterns
3. Capture BSDiag log session:
   ```bash
   adb logcat -s BSDiag > target_detection_session.log
   ```
4. Analyze detection rate:
   ```bash
   grep "TARGET_DETECT" target_detection_session.log | wc -l
   grep "FRAME" target_detection_session.log | wc -l
   # Calculate: (detections / frames) = detection hit rate
   ```
5. Check queue performance:
   ```bash
   grep "QUEUE_DEQUEUE" target_detection_session.log | \
       awk '{print $4}' | \  # queueDelayMs
       awk '{sum+=$1; count++} END {print "Avg queue delay:", sum/count, "ms"}'
   ```

**Success Criteria:**
- Detection rate > 70% (better than ballstars 63-70% baseline)
- Avg queue delay < 100ms (low latency)
- No crashes or SIGSEGV (fixed by proper cleanup in queue processor)

## Architecture Decisions

### Why Async API?

MediaPipe Tasks Vision uses async callbacks because:
1. Modern non-blocking design
2. Better resource utilization (don't block threads waiting)
3. Supports hardware acceleration without blocking

### Why Keep Frame Queue?

Even with async API, the queue is still valuable:
1. **Prevents frame drops**: Camera never blocks waiting for detection
2. **Preserves order**: Frames processed in capture order
3. **Bounded backpressure**: Queue capacity limits memory usage
4. **Measurable lag**: QUEUE_DEQUEUE logging quantifies delay

### Threading Model

- **Main thread**: UI, lifecycle callbacks
- **analysisExecutor**: CameraX analyzer callback (frame copying)
- **queueProcessorExecutor**: Dequeue and call detectAsync()
- **MediaPipe callbacks**: Run on MediaPipe's internal threads
- **Flow updates**: Results flow back to UI via StateFlow

## BSDiag Logging Compatibility

All critical events preserved for detection rate analysis:

| Event | Purpose | Old vs New |
|-------|---------|------------|
| `DETECTOR_CREATED` → `TARGET_DETECTOR_VM_CREATED` | Track instances | ✓ Preserved |
| `DETECTOR_INIT` → `TARGET_DETECTOR_INIT` | Initialization time | ✓ Preserved |
| `FRAME` | Frame submitted | ✓ Preserved |
| `QUEUE_DEQUEUE` | Queue lag measurement | ✓ Preserved |
| `INFER` → `TARGET_DETECT` | Detection results | ✓ Preserved (renamed) |
| `QUEUE_PROCESSOR_START/STOP` | Lifecycle | ✓ Preserved |

Analysis scripts from ballstars should work with minimal changes (update event names).

## Model Details

**Model:** target-detector-qat-int8.tflite
**Training Results:**
- Epochs: 40/40 complete
- AP@IoU=0.50: 71.1%
- No overfitting (val better than train)
- Size: ~4-5 MB (int8 quantized)

**Input:** Camera frames (variable resolution, auto-scaled by MediaPipe)
**Output:** List of detections with:
- Bounding box (x, y, width, height)
- Class label (target type/number)
- Confidence score

## Known Issues & Limitations

1. **Model export pending**: Waiting for export to complete, then copy to assets folder
2. **Untested**: Requires physical device testing to verify end-to-end functionality
3. **iOS not implemented**: Target detection only available on Android currently

## Files Created/Modified

**Created:**
- [TargetDetectorHelper.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/detection/TargetDetectorHelper.kt) - MediaPipe async detection wrapper
- [TargetDetectorViewModel.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/detection/TargetDetectorViewModel.kt) - Frame queue architecture
- [TargetDetectionScreen.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/ui/TargetDetectionScreen.kt) - Camera preview UI with overlays
- [CameraPermission.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/ui/CameraPermission.kt) - Runtime permission handling
- [App.android.kt](shared/src/androidMain/kotlin/venturewave/one/gridgames/App.android.kt) - Android-specific app entry
- [App.ios.kt](shared/src/iosMain/kotlin/venturewave/one/gridgames/App.ios.kt) - iOS placeholder
- [PHASE4_IMPLEMENTATION_SUMMARY.md](PHASE4_IMPLEMENTATION_SUMMARY.md) - This documentation

**Modified:**
- [shared/build.gradle.kts](shared/build.gradle.kts) - Added MediaPipe and CameraX dependencies
- [App.kt](shared/src/commonMain/kotlin/venturewave/one/gridgames/App.kt) - Added expect/actual pattern
- [AndroidManifest.xml](androidApp/src/main/AndroidManifest.xml) - Added camera permissions

**Pending:**
- `androidApp/src/main/assets/target-detector-qat-int8.tflite` - Model file (export in progress)

## References

- [MediaPipe Tasks Vision Documentation](https://developers.google.com/mediapipe/solutions/vision/object_detector)
- [CameraX Documentation](https://developer.android.com/training/camerax)
- Old implementation: `/Users/simonparkhouse/StudioProjects/ballstars/mobile/composeApp/src/androidMain/kotlin/com/ballstars/mobile/fusion/viewmodels/`

---

**Status:** Phase 4 implementation complete. Full camera preview UI with real-time target detection ready. Awaiting model export to copy to assets, then ready for physical device testing.
