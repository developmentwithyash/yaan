package com.yaan.app.ui.voice

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Pure mathematical calculator for real-time dynamic audio waveform visualizer bars.
 * Combines sine wave phase modulation with Gaussian center-weighting.
 */
object WaveformCalculator {

    /**
     * Computes a list of bar heights in dp units based on current input amplitude and time phase.
     */
    fun computeBarHeights(
        barCount: Int,
        inputAmplitude: Float,
        phaseRad: Float,
        minHeight: Float = 4f,
        maxHeight: Float = 48f
    ): List<Float> {
        if (barCount <= 0) return emptyList()
        val clampedAmp = inputAmplitude.coerceIn(0f, 1f)
        if (clampedAmp == 0f) {
            return List(barCount) { minHeight }
        }

        val range = (maxHeight - minHeight).coerceAtLeast(0f)

        return List(barCount) { i ->
            val normIndex = if (barCount > 1) i.toFloat() / (barCount - 1) else 0.5f
            val centerWeight = sin(normIndex * PI).toFloat().coerceIn(0.2f, 1.0f)
            val sineMod = abs(sin(phaseRad + i * 0.45f)).toFloat()
            val dynamicFactor = 0.25f + 0.75f * sineMod
            val calculated = minHeight + range * clampedAmp * centerWeight * dynamicFactor
            calculated.coerceIn(minHeight, maxHeight)
        }
    }
}
