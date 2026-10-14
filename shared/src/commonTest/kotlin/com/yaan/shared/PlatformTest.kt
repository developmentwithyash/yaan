package com.yaan.shared

import com.yaan.shared.platform.PlatformHaptics
import com.yaan.shared.platform.getPlatformName
import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformTest {

    @Test
    fun testPlatformNameResolution() {
        val platform = getPlatformName()
        assertTrue(platform.isNotEmpty())
    }

    @Test
    fun testHapticsInstantiation() {
        val haptics = PlatformHaptics()
        haptics.performTick()
    }
}
