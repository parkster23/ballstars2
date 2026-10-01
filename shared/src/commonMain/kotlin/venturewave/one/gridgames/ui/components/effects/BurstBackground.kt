package venturewave.one.gridgames.ui.components.effects

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import venturewave.one.gridgames.ui.theme.BallStarsColor
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * BurstBackground - Animated radial burst effect
 *
 * Gold/cyan radial streaks from center, used for:
 * - Splash screen background
 * - Move success screen
 * - Special move activation
 *
 * Design reference: Splash screen, Move success screen
 */
@Composable
fun BurstBackground(
    modifier: Modifier = Modifier,
    rayColor: Color = BallStarsColor.Gold,
    backgroundColor: Color = BallStarsColor.BgDeep,
    rayCount: Int = 8,
    animate: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "burst_rotation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animate) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "burst_rotation_angle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = maxOf(size.width, size.height)

        // Background gradient
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    BallStarsColor.Surface,
                    BallStarsColor.BgDeep,
                    backgroundColor
                ),
                center = Offset(centerX, centerY),
                radius = maxRadius * 0.8f
            )
        )

        // Draw burst rays
        rotate(rotation, pivot = Offset(centerX, centerY)) {
            val angleStep = 360f / rayCount

            for (i in 0 until rayCount) {
                // Math.toRadians is JVM-only; degrees * PI / 180 is the plain conversion
                val angle = (i * angleStep).toDouble() * PI / 180.0
                val rayLength = maxRadius * 1.5f

                // Calculate ray endpoints
                val startX = centerX
                val startY = centerY
                val endX = centerX + (cos(angle) * rayLength).toFloat()
                val endY = centerY + (sin(angle) * rayLength).toFloat()

                // Draw ray with gradient (bright center to transparent edge)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            rayColor.copy(alpha = 0.6f),
                            rayColor.copy(alpha = 0.3f),
                            rayColor.copy(alpha = 0.0f)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 40f
                )
            }
        }
    }
}
