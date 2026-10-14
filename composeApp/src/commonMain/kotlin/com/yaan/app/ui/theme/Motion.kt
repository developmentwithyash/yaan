package com.yaan.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Immutable

/**
 * Motion and Choreography Tokens for Executive Kinetic Minimalism
 * Fluid, physics-based micro-interactions without gratuitous latency.
 */
@Immutable
data class YaanMotion(
    // Durations in milliseconds
    val instantMs: Int = 80,
    val fastMs: Int = 150,
    val normalMs: Int = 250,
    val emphasizedMs: Int = 400,
    val orbitalCycleMs: Int = 2400,

    // Easings
    val standardEasing: Easing = FastOutSlowInEasing,
    val exitEasing: Easing = LinearOutSlowInEasing,
    val kineticSpring: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val gentleDecel: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f),
)
