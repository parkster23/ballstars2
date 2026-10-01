package venturewave.one.gridgames

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import venturewave.one.gridgames.ui.theme.BallStarsColor

/**
 * iOS placeholder implementation of ScanTargets3Screen
 * TODO: Implement iOS camera calibration when iOS support is added
 */
@Composable
actual fun PlatformSpecificScanTargets3(
    onBack: () -> Unit,
    onGridCalibrated: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "iOS Grid Calibration Coming Soon",
            color = BallStarsColor.TextPrimary
        )
    }
}
