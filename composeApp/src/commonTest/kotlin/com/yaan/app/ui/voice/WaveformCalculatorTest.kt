package com.yaan.app.ui.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaveformCalculatorTest {

    @Test
    fun testBarCountMatchesRequested() {
        val heights = WaveformCalculator.computeBarHeights(
            barCount = 16,
            inputAmplitude = 0.8f,
            phaseRad = 1.0f
        )
        assertEquals(16, heights.size)
    }

    @Test
    fun testAllHeightsWithinBounds() {
        val min = 4f
        val max = 50f
        val heights = WaveformCalculator.computeBarHeights(
            barCount = 20,
            inputAmplitude = 1.0f,
            phaseRad = 2.5f,
            minHeight = min,
            maxHeight = max
        )

        for (h in heights) {
            assertTrue(h >= min, "Height $h must be >= min $min")
            assertTrue(h <= max, "Height $h must be <= max $max")
        }
    }

    @Test
    fun testZeroAmplitudeReturnsMinHeight() {
        val min = 6f
        val heights = WaveformCalculator.computeBarHeights(
            barCount = 12,
            inputAmplitude = 0.0f,
            phaseRad = 0.5f,
            minHeight = min,
            maxHeight = 60f
        )

        for (h in heights) {
            assertEquals(min, h)
        }
    }

    @Test
    fun testCenterWeighting() {
        val heights = WaveformCalculator.computeBarHeights(
            barCount = 11,
            inputAmplitude = 0.9f,
            phaseRad = 0.0f,
            minHeight = 4f,
            maxHeight = 40f
        )

        val centerIdx = 5
        val edgeIdx = 0

        assertTrue(
            heights[centerIdx] >= heights[edgeIdx],
            "Center bar (${heights[centerIdx]}) should be higher than edge bar (${heights[edgeIdx]})"
        )
    }
}
