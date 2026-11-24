package com.yaan.app.ui.voice

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/**
 * Interactive, hardware-accelerated audio waveform visualizer canvas.
 * Draws dynamic amplitude bars modulated with sine animation and center-weighting.
 */
@Composable
fun AudioWaveformCanvas(
    modifier: Modifier = Modifier,
    amplitude: Float = 0.5f,
    barCount: Int = 24,
    barWidth: Dp = 4.dp,
    barSpacing: Dp = 3.dp,
    canvasHeight: Dp = 60.dp,
    primaryColor: Color = Color(0xFFD4AF37), // AmberGold
    secondaryColor: Color = Color(0xFFCD7F32) // BronzeAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_angle"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeight)
    ) {
        val heights = WaveformCalculator.computeBarHeights(
            barCount = barCount,
            inputAmplitude = amplitude,
            phaseRad = phase,
            minHeight = 4.dp.toPx(),
            maxHeight = size.height * 0.85f
        )

        val totalBarWidthPx = barWidth.toPx()
        val spacingPx = barSpacing.toPx()
        val totalSpanPx = barCount * totalBarWidthPx + (barCount - 1) * spacingPx
        val startXPx = (size.width - totalSpanPx) / 2f
        val centerYPx = size.height / 2f

        for (i in 0 until barCount) {
            val barH = heights.getOrElse(i) { 4.dp.toPx() }
            val x = startXPx + i * (totalBarWidthPx + spacingPx)
            val y = centerYPx - (barH / 2f)

            val color = if (i % 2 == 0) primaryColor else secondaryColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(totalBarWidthPx, barH),
                cornerRadius = CornerRadius(totalBarWidthPx / 2f, totalBarWidthPx / 2f)
            )
        }
    }
}
