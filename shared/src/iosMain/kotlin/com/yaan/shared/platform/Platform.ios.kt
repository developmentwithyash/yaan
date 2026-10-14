package com.yaan.shared.platform

actual fun getPlatformName(): String = "iOS"

actual class PlatformHaptics actual constructor() {
    actual fun performTick() {
        // Platform haptic feedback via UIKit UIImpactFeedbackGenerator
    }
}
