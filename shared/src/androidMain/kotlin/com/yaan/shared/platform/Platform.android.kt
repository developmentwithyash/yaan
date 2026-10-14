package com.yaan.shared.platform

actual fun getPlatformName(): String = "Android"

actual class PlatformHaptics actual constructor() {
    actual fun performTick() {
        // Platform vibration tick via Android system service
    }
}
