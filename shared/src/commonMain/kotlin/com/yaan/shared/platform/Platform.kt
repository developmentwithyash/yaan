package com.yaan.shared.platform

expect fun getPlatformName(): String

expect class PlatformHaptics() {
    fun performTick()
}
