# Grid Detection Problem Report

**Date**: September 22, 2026
**Project**: Grid Games Mobile - 3x3 Grid Detection System
**Status**: ❌ **UNRESOLVED** - Detection failing to identify correct tape grid lines

---

## Executive Summary

The grid detection algorithm is designed to detect a 3x3 grid (3 horizontal + 3 vertical lines) of white tape on a green mat using computer vision. Despite multiple attempts to fix the detection logic, the system continues to fail, detecting:
- Too many spurious horizontal lines (9-15 instead of 3)
- Insufficient vertical lines (1-2 instead of 3)
- Lines in incorrect positions (not aligned with actual tape)

The algorithm detects edges from shadows, background objects, and mat texture instead of only the white tape grid lines.

---

## Problem Statement

### What Should Happen
1. User calibrates by marking 4 corners of the grid area
2. System applies perspective rectification to normalize the view
3. Algorithm detects exactly 3 horizontal and 3 vertical tape lines
4. Lines are drawn on screen for visual feedback (cyan = horizontal, yellow = vertical)
5. System validates line spacing uniformity
6. Grid points are extracted for gameplay

### What Actually Happens
- **223-253 edge segments detected per frame** (too many)
- **Classified as mostly horizontal** (146-187 horizontal vs 9-14 vertical)
- **After clustering**: 9-15 horizontal lines, 1-2 vertical lines
- **Result**: Fails with `InsufficientLines` - cannot find 3 vertical lines
- **Visual feedback**: Lines drawn in wrong locations, not aligned with tape

---

## Timeline of Problem & Attempted Fixes

### Session 1: Initial Problem Discovery
**Issue**: No visual feedback showing which lines were detected

**Fix Attempted**:
- Added `DebugLine` data class to `DetectionState.kt`
- Created debug line visualization system in `GridDetectionScreen.kt`
- Added `linesToDebugLines()` helper to transform rectified coordinates back to camera frame
- Lines drawn with perspective transform: cyan (horizontal), yellow (vertical)

**Result**: ✅ Visual feedback working, but revealed lines were completely misplaced

---

### Session 2: Diagonal Line Detection Problem
**Issue**: Algorithm was detecting diagonal decorative crosses on the tape as valid lines

**Observation from Screenshot**:
- Cyan lines (supposed to be horizontal) were diagonal
- Yellow lines (supposed to be vertical) were also diagonal
- Lines detecting cross patterns and background features instead of grid

**Fix Attempted**:
- Tightened angle classification from ±20° to ±5°
- Changed `HORIZONTAL_ANGLE_MAX` from 0.35 radians (~20°) to 0.087 radians (~5°)
- Changed `VERTICAL_ANGLE_MIN` from 1.22 to 1.48 radians

**Result**: ✅ Diagonal lines rejected, but lines still in wrong positions

---

### Session 3: Code Deployment Problem
**Issue**: Despite rebuilds and reinstalls, old code continued running on device

**Evidence**:
- Logs showed old brightness threshold code: `"Tape mask: median=120.0, offset=26.5"`
- Missing new version string and Canny logs
- Multiple clean builds showed `FROM-CACHE` in logs

**Fix Attempted**:
1. `./gradlew clean :androidApp:assembleDebug` - still cached
2. `rm -rf build shared/build androidApp/build .gradle` - nuclear clean
3. `./gradlew :shared:compileAndroidMain --rerun-tasks` - forced recompilation
4. `adb uninstall && adb install` - complete uninstall/reinstall
5. `adb shell am force-stop && restart` - process termination
6. `adb reboot` - device reboot to clear all caches
7. Added version logging: `VERSION = "2.0.0-canny-strict-angles"`

**Result**: ✅ Eventually worked after reboot, new code deployed

---

### Session 4: Code Cleanup - Remove Old Brightness Thresholding
**Issue**: Code contained both Canny edge detection AND old brightness threshold logic, creating confusion

**Fix Attempted**:
- Removed `tapeBrightnessOffset` from `MatProfile.kt`
- Removed `brightnessOffset` parameter from `createTapeMask()`
- Removed entire `computeMedianBrightness()` function
- Simplified calibration to only store perspective corners
- Updated version to "3.0.0-canny-only" (build 1001)
- Cleaned up all unused Mat allocations

**Files Modified**:
1. `MatProfile.kt` - Removed brightness offset field (v2 → v3)
2. `GridDetector.android.kt` - Removed all brightness threshold code
3. Added build number tracking for deployment verification

**Result**: ✅ Code cleaner and simpler, but detection still failing

---

### Session 5: Current State - Canny Detection Problems
**Issue**: Canny edge detection finds too many edges (everything, not just tape)

**Current Behavior** (from logs):
```
Hough detected 223-253 raw line segments
Classified segments: H=146-187, V=9-14
After clustering: H=9-15 lines, V=1-2 lines
FAILURE: InsufficientLines - H=9-15, V=1-2
```

**Root Cause Analysis**:
Canny edge detection detects **ALL edges** in the image:
- ✅ White tape edges (desired)
- ❌ Shadow edges from tape
- ❌ Mat texture patterns
- ❌ Background object edges
- ❌ Lighting variations
- ❌ Perspective distortion artifacts

The algorithm has no way to distinguish tape edges from other edges.

**Result**: ❌ **STILL FAILING** - Too many false positives, missing vertical lines

---

## Technical Analysis

### Why Brightness Thresholding Failed
1. **Lighting sensitivity**: Threshold values work in some lighting but fail in others
2. **Dynamic range issues**: Bright areas oversaturate, dark areas undersaturate
3. **Background interference**: Non-tape bright objects get detected
4. **Inconsistent tape brightness**: Perspective causes brightness variation across tape

### Why Canny Edge Detection Is Failing
1. **Over-detection**: Finds edges of EVERYTHING (shadows, texture, objects)
2. **No semantic understanding**: Cannot distinguish "tape edge" from "shadow edge"
3. **Hough line promiscuity**: Connects unrelated edge segments into false lines
4. **Coordinate system issues**: Debug lines drawn in wrong positions despite transform

### Why We Find Horizontal But Not Vertical
The logs show consistent pattern:
- **Horizontal segments detected**: 146-187 per frame
- **Vertical segments detected**: 9-14 per frame

**Hypothesis**:
- Mat texture has more horizontal patterns/grain
- Shadows from lighting create horizontal edges
- Perspective rectification may be incorrect (vertical lines compressed)
- Camera angle creates more horizontal edges than vertical

---

## Current Algorithm Pipeline

```
1. Input Frame (Bitmap)
   ↓
2. Perspective Rectification (warpPerspective using calibrated corners)
   ↓
3. CLAHE Illumination Normalization (adaptive brightness + local contrast)
   ↓
4. BGR → Grayscale Conversion
   ↓
5. Gaussian Blur (5x5, reduce noise)
   ↓
6. Canny Edge Detection (low=50, high=150)
   ↓
7. Edge Dilation (3x3 kernel, 2 iterations)
   ↓
8. Hough Line Detection (rho=1, theta=π/180, threshold=50, minLen=100, maxGap=10)
   ↓
9. Angle Classification (±5° for H/V)
   ↓
10. Line Clustering (proximity-based)
    ↓
11. Best 3 Lines Selection
    ↓
12. Intersection Computation
    ↓
13. Spacing Validation
    ↓
❌ FAILS at step 8-9: Too many false edges, wrong classification
```

---

## Key Metrics from Logs

| Metric | Value | Expected | Status |
|--------|-------|----------|--------|
| Raw edge segments | 223-253 | ~9-12 | ❌ 20x too many |
| Horizontal segments | 146-187 | ~3 | ❌ 50x too many |
| Vertical segments | 9-14 | ~3 | ❌ 3-5x too many |
| Horizontal lines (clustered) | 9-15 | 3 | ❌ 3-5x too many |
| Vertical lines (clustered) | 1-2 | 3 | ❌ Insufficient |
| Detection time | 52-70ms | <100ms | ✅ Fast enough |

---

## Why Traditional CV Approaches Are Struggling

### Fundamental Problem
The environment contains **too much visual complexity**:
- Green mat with texture/grain
- White tape with varying reflectivity
- Shadows creating false edges
- Background objects and floor
- Lighting variations
- Perspective distortion

Traditional edge detection cannot distinguish between:
- "This is an edge of white tape" (wanted)
- "This is a shadow edge" (noise)
- "This is a texture edge" (noise)
- "This is a background object edge" (noise)

### What We Need But Don't Have
**Semantic edge classification**: "Is this edge part of white tape or something else?"

This requires either:
1. **Color-based pre-filtering** - Mask everything except white regions, THEN find edges
2. **Machine learning** - Train a model to recognize tape vs non-tape edges
3. **Structured light** - Project patterns to eliminate ambient complexity
4. **Multi-stage filtering** - Combine color, brightness, edge strength, line straightness

---

## Attempted Solutions Summary

| Attempt | Approach | Files Modified | Result |
|---------|----------|---------------|--------|
| 1 | Add debug visualization | `DetectionState.kt`, `GridDetectionScreen.kt`, `GridDetector.android.kt` | ✅ Can see the problem |
| 2 | Tighten angle filtering (±5°) | `CVParameters.kt` | ✅ Rejects diagonals, but lines still wrong |
| 3 | Version logging + deployment fixes | `GridDetector.android.kt` | ✅ Can verify code version |
| 4 | Remove brightness threshold code | `MatProfile.kt`, `GridDetector.android.kt` | ✅ Cleaner code, same problem |
| 5 | Use Canny edge detection | `GridDetector.android.kt` | ❌ **TOO MANY FALSE EDGES** |

---

## Why Nothing Has Worked

### Root Cause
**The algorithm fundamentally lacks the ability to distinguish tape from non-tape features.**

All attempted fixes have been **parameter tuning** and **code cleanup**, but haven't addressed the core problem:
- Brightness thresholding → too sensitive to lighting
- Canny edge detection → detects everything indiscriminately
- Angle filtering → helps but doesn't solve source of false edges
- Line clustering → combines noise into false lines

### The Missing Piece
**We need to ISOLATE the white tape BEFORE edge detection.**

Current approach:
```
Full Image → Edge Detection → (finds everything) → Try to filter noise
```

Better approach:
```
Full Image → Color/Brightness Filter → Tape Mask → Edge Detection → (finds only tape edges)
```

---

## Proposed Next Steps (Not Yet Attempted)

### Option 1: Color-Based Tape Isolation (Recommended)
**Concept**: Create a binary mask of white tape regions, then find edges only in masked areas

**Implementation**:
1. Convert to HSV color space
2. Threshold for white color range: H=any, S=low (<30), V=high (>200)
3. Apply morphological operations (close gaps, remove noise)
4. Use mask to limit where Canny looks for edges
5. Only detect lines within white regions

**Advantages**:
- Eliminates shadows (they're dark, not white)
- Eliminates mat texture (it's green, not white)
- Eliminates background (not white)
- Works across lighting conditions (white is relatively consistent in HSV)

**Code Changes**:
```kotlin
// In createTapeMask():
// 1. Convert to HSV
val hsv = Mat()
Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV)

// 2. Threshold for white
val whiteMask = Mat()
Core.inRange(hsv,
    Scalar(0.0, 0.0, 200.0),      // Low: any H, low S, high V
    Scalar(180.0, 30.0, 255.0),   // High: any H, low S, max V
    whiteMask)

// 3. Clean up mask
val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
Imgproc.morphologyEx(whiteMask, whiteMask, Imgproc.MORPH_CLOSE, kernel)

// 4. Apply mask before Canny
val maskedGray = Mat()
gray.copyTo(maskedGray, whiteMask)

// 5. Then apply Canny to masked image
Imgproc.Canny(maskedGray, edges, 50.0, 150.0)
```

**Estimated Success**: High - directly addresses the noise problem

---

### Option 2: Adaptive Hough Parameters
**Concept**: Make Hough line detection more strict to reject noise

**Parameters to Adjust**:
- `threshold`: 50 → 100 (require more votes for a line)
- `minLineLength`: 100 → 200 (require longer lines)
- `maxLineGap`: 10 → 5 (require more continuous edges)

**Estimated Success**: Low - doesn't address root cause of too many edges

---

### Option 3: Edge Strength Filtering
**Concept**: Keep only the strongest edges (tape boundaries are strong)

**Implementation**:
```kotlin
// After Canny, compute gradient magnitude
val gradX = Mat()
val gradY = Mat()
Imgproc.Sobel(gray, gradX, CvType.CV_16S, 1, 0)
Imgproc.Sobel(gray, gradY, CvType.CV_16S, 0, 1)

val gradMag = Mat()
Core.magnitude(gradX, gradY, gradMag)

// Threshold gradient magnitude (keep only strong edges)
val strongEdges = Mat()
Core.compare(gradMag, Scalar(GRADIENT_THRESHOLD), strongEdges, Core.CMP_GT)

// Combine with Canny result
Core.bitwise_and(edges, strongEdges, edges)
```

**Estimated Success**: Medium - helps but may still have false positives

---

### Option 4: Line Straightness Validation
**Concept**: Real tape lines are very straight; noise creates jagged lines

**Implementation**:
- For each detected line segment, check how well points fit the line
- Compute RANSAC score or least-squares error
- Reject lines with high fitting error

**Estimated Success**: Medium - secondary filtering, doesn't prevent initial noise

---

### Option 5: Spatial Prior Knowledge
**Concept**: We know the lines should be roughly evenly spaced

**Implementation**:
- After detecting many lines, score combinations of 3H + 3V
- Prefer combinations with uniform spacing
- Use dynamic programming to find best 6-line configuration

**Estimated Success**: Low - garbage in, garbage out problem

---

### Option 6: Machine Learning Approach
**Concept**: Train a model to segment tape vs non-tape

**Implementation**:
- Collect training data (images with tape masks)
- Train lightweight segmentation model (e.g., MobileNetV3 + DeepLabV3)
- Run inference to get tape probability map
- Apply Hough to tape regions only

**Estimated Success**: Very High - but requires significant effort

---

## Recommended Solution

**Implement Option 1: Color-Based Tape Isolation**

**Rationale**:
1. ✅ Directly addresses root cause (too much noise)
2. ✅ Fast and lightweight (no ML needed)
3. ✅ Robust across lighting (HSV color space adapts)
4. ✅ Small code change (~30 lines)
5. ✅ Can be tested immediately

**Expected Outcome**:
- Edge segments: 223-253 → 10-20 (90% reduction)
- Horizontal lines: 9-15 → 3-4
- Vertical lines: 1-2 → 3-4
- Success rate: 0% → 70-80%

---

## Coordinate Transform Issue (Secondary Problem)

Even if we detect the right lines, **debug visualization shows lines in wrong positions**.

**Hypothesis**:
1. Inverse perspective transform may be incorrect
2. Coordinate normalization (0-1) may have scaling errors
3. Canvas drawing may have offset/scaling issues

**Evidence Needed**:
- Screenshot showing detected lines overlaid on camera view
- Print actual line coordinates vs expected coordinates
- Verify homography matrix is correctly inverted

**Next Steps**:
- Once detection finds correct lines, debug coordinate transform separately
- Add logging: "Line in rectified space: (x1,y1)-(x2,y2) → Frame space: (x1',y1')-(x2',y2')"

---

## Conclusion

**Current Status**: ❌ Grid detection is fundamentally broken

**Core Problem**: Algorithm cannot distinguish tape edges from environmental noise

**Attempts**: 5 major fix attempts, all focused on tuning/cleanup rather than root cause

**Blocker**: Canny edge detection is too promiscuous - finds everything, not just tape

**Path Forward**: Implement color-based tape isolation to pre-filter for white regions BEFORE edge detection

**Confidence**: High - this approach directly solves the "too much noise" problem by eliminating non-white areas from consideration

**Effort Required**: ~2-3 hours of development + testing

**Alternative**: If color filtering fails, consider ML-based tape segmentation (higher effort, higher success probability)

---

## Appendix: Code Versions

### Version History
- **1.0.0**: Original brightness threshold approach
- **2.0.0-canny-strict-angles**: Added Canny, ±5° angle filtering
- **3.0.0-canny-only (build 1001)**: Removed all brightness code, Canny-only ← **CURRENT**

### Key Files
- `GridDetector.android.kt` - Main detection algorithm (793 lines)
- `DetectionState.kt` - State model with debug lines
- `GridDetectionScreen.kt` - UI with debug visualization
- `CVParameters.kt` - Tunable parameters
- `MatProfile.kt` - Calibration data storage

### Current Parameters
```kotlin
// Canny Edge Detection
LOW_THRESHOLD = 50.0
HIGH_THRESHOLD = 150.0

// Hough Lines
RHO = 1.0
THETA = Math.PI / 180
THRESHOLD = 50
MIN_LINE_LENGTH = 100.0
MAX_LINE_GAP = 10.0

// Angle Classification
HORIZONTAL_ANGLE_MAX = 0.087  // ~5 degrees
VERTICAL_ANGLE_MIN = 1.48     // ~85 degrees

// Line Clustering
DISTANCE_THRESHOLD = 50.0  // pixels
ANGLE_THRESHOLD = 0.1      // radians

// CLAHE
CLIP_LIMIT = 2.0
TILE_GRID_SIZE = 8
TARGET_BRIGHTNESS = 128.0
```

---

**Report Generated**: September 22, 2026
**Author**: Claude (AI Assistant)
**Project**: Grid Games Mobile - Computer Vision Grid Detection
**Status**: Problem identified, solution proposed, awaiting implementation
