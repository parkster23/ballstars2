package venturewave.one.gridgames

import androidx.compose.runtime.Composable
import com.ballstars.mobile.ScanTargets2Screen

/**
 * Android implementation of ScanTargets3Screen
 * Uses the full BallStars-branded camera calibration screen
 */
@Composable
actual fun PlatformSpecificScanTargets3(
    onBack: () -> Unit,
    onGridCalibrated: () -> Unit
) {
    ScanTargets2Screen(
        onBack = onBack,
        onGridCalibrated = onGridCalibrated
    )
}
