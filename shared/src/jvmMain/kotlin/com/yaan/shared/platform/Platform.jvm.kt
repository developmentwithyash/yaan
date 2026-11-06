package com.yaan.shared.platform

actual fun getPlatformName(): String = "Desktop JVM (${System.getProperty("os.name")})"

actual class PlatformHaptics actual constructor() {
    actual fun performTick() {}
}
