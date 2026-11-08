package com.yaan.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class YaanThemeTest {

    @Test
    fun testLightColorTokensIntegrity() {
        // Assert canvas base and surface container hierarchy
        assertEquals(Color(0xFFFDF9F3), SurfaceLight)
        assertEquals(Color(0xFFFFFFFF), SurfaceContainerLowestLight)
        assertEquals(Color(0xFF141311), OnSurfaceLight)
        assertEquals(Color(0xFF785600), PrimaryLight)
        assertEquals(Color(0xFFDAD4CA), DelicateBorderLight)

        // Ensure primary is distinct from surface
        assertNotEquals(PrimaryLight, SurfaceLight)
    }

    @Test
    fun testDarkColorTokensIntegrity() {
        assertEquals(Color(0xFF141311), SurfaceDark)
        assertEquals(Color(0xFFF4F0EB), OnSurfaceDark)
        assertEquals(Color(0xFFF7BD48), PrimaryDark)

        // Ensure dark surface is dark
        assertNotEquals(SurfaceLight, SurfaceDark)
    }

    @Test
    fun testTypographyScaleDefinitions() {
        assertEquals(40f, YaanTypography.displayLarge.fontSize.value)
        assertEquals(28f, YaanTypography.headlineLarge.fontSize.value)
        assertEquals(22f, YaanTypography.headlineMedium.fontSize.value)
        assertEquals(18f, YaanTypography.headlineSmall.fontSize.value)
        assertEquals(14f, YaanTypography.bodyMedium.fontSize.value)
        assertEquals(11f, YaanTypography.labelSmall.fontSize.value)
    }

    @Test
    fun testShapeCornerRadii() {
        // Extra small is 4dp, small is 8dp, medium is 12dp, large is 16dp
        assertTrue(YaanShapes.extraSmall.topStart.toString().contains("4.0"))
        assertTrue(YaanShapes.small.topStart.toString().contains("8.0"))
        assertTrue(YaanShapes.medium.topStart.toString().contains("12.0"))
        assertTrue(YaanShapes.large.topStart.toString().contains("16.0"))
    }

    @Test
    fun testSpacingTokensIntegrity() {
        val spacing = YaanSpacing()
        assertEquals(0f, spacing.none.value)
        assertEquals(4f, spacing.xs.value)
        assertEquals(8f, spacing.sm.value)
        assertEquals(12f, spacing.md.value)
        assertEquals(16f, spacing.lg.value)
        assertEquals(24f, spacing.xxl.value)
        assertEquals(32f, spacing.xxxl.value)
    }

    @Test
    fun testElevationTokensIntegrity() {
        val elevation = YaanElevation()
        assertEquals(0f, elevation.level0.elevationDp.value)
        assertEquals(1f, elevation.level1.elevationDp.value)
        assertEquals(2f, elevation.level2.elevationDp.value)
        assertEquals(4f, elevation.level3.elevationDp.value)
        assertEquals(8f, elevation.level4.elevationDp.value)
        assertEquals(12f, elevation.level5.elevationDp.value)
    }

    @Test
    fun testMotionTokensIntegrity() {
        val motion = YaanMotion()
        assertEquals(80, motion.instantMs)
        assertEquals(150, motion.fastMs)
        assertEquals(250, motion.normalMs)
        assertEquals(400, motion.emphasizedMs)
    }

    @Test
    fun testThemeModeResolutionAndCycling() {
        // SYSTEM should defer to system flag
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertTrue(!ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))

        // LIGHT should always be false
        assertTrue(!ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
        assertTrue(!ThemeMode.LIGHT.isDark(systemInDarkTheme = false))

        // DARK should always be true
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = true))
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = false))

        // Cycle through modes: SYSTEM -> LIGHT -> DARK -> SYSTEM
        assertEquals(ThemeMode.LIGHT, ThemeMode.SYSTEM.next())
        assertEquals(ThemeMode.DARK, ThemeMode.LIGHT.next())
        assertEquals(ThemeMode.SYSTEM, ThemeMode.DARK.next())
    }
}
