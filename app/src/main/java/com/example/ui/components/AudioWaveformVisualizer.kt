package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RoyalJadeTealAura
import com.example.ui.theme.SolarChampagne
import com.example.ui.theme.SolarChampagneGlow
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Real-time audio waveform visualizer that renders dynamic vertical equalizer frequency bars
 * on a Jetpack Compose Canvas, reacting instantly to microphone input levels to assure
 * shopkeepers that their Bangla voice commands are being actively captured.
 */
@Composable
fun AudioWaveformVisualizer(
    audioLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 23,
    height: Dp = 54.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    
    // Wave phase animation for natural undulating flow
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )

    // Ambient breathing pulse when speech is quiet
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("audio_waveform_visualizer")
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barSpacing = totalWidth / (barCount * 1.6f)
        val barWidth = barSpacing * 0.6f
        val centerIndex = barCount / 2f

        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                SolarChampagneGlow,
                SolarChampagne,
                RoyalJadeTealAura
            ),
            startY = 0f,
            endY = canvasHeight
        )

        val idleBrush = Brush.verticalGradient(
            colors = listOf(
                RoyalJadeTealAura.copy(alpha = 0.5f),
                SolarChampagne.copy(alpha = 0.3f)
            ),
            startY = 0f,
            endY = canvasHeight
        )

        for (i in 0 until barCount) {
            // Distance from center (0 at center, 1 at ends)
            val distFromCenter = abs(i - centerIndex) / centerIndex
            // Symmetrical bell curve weight (higher in the middle)
            val bellWeight = (1f - (distFromCenter * 0.65f)).coerceIn(0.2f, 1f)

            // Sine wave phase offset per bar
            val waveOffset = sin(phase + (i * 0.45f))

            val currentHeight: Float = if (isListening) {
                val effectiveLevel = if (audioLevel > 0.05f) audioLevel else ambientPulse
                // Bar height combines normalized mic dB, wave oscillation, and bell curve
                val dynamicFactor = (effectiveLevel * 0.75f) + (abs(waveOffset) * 0.25f * effectiveLevel)
                val calculated = canvasHeight * dynamicFactor * bellWeight
                calculated.coerceIn(8.dp.toPx(), canvasHeight)
            } else {
                // Dormant subtle resting line
                (canvasHeight * 0.12f * bellWeight).coerceAtLeast(4.dp.toPx())
            }

            val x = (i * (barWidth + barSpacing)) + (barSpacing / 2f)
            val y = (canvasHeight - currentHeight) / 2f

            drawRoundRect(
                brush = if (isListening && (audioLevel > 0.05f || ambientPulse > 0.2f)) gradientBrush else idleBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, currentHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
