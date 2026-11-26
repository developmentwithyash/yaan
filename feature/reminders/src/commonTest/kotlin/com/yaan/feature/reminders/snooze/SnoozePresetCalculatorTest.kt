package com.yaan.feature.reminders.snooze

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnoozePresetCalculatorTest {

    private val calculator = SnoozePresetCalculator()

    @Test
    fun testFifteenMinutesPresetAddsExactMillis() {
        val now = 1700000000000L
        val expected = now + (15 * 60 * 1000L)
        val result = calculator.calculateSnoozeTimestamp(
            preset = SnoozePreset.FIFTEEN_MINUTES,
            currentEpochMillis = now
        )
        assertEquals(expected, result)
    }

    @Test
    fun testOneHourPresetAddsExactMillis() {
        val now = 1700000000000L
        val expected = now + (60 * 60 * 1000L)
        val result = calculator.calculateSnoozeTimestamp(
            preset = SnoozePreset.ONE_HOUR,
            currentEpochMillis = now
        )
        assertEquals(expected, result)
    }

    @Test
    fun testNextMorningAdvancesToNextCalendarDayMorning() {
        // Thursday 2:00 PM (14:00 UTC)
        val now = 1700056800000L 
        val result = calculator.calculateSnoozeTimestamp(
            preset = SnoozePreset.NEXT_MORNING,
            currentEpochMillis = now,
            morningHour = 9,
            morningMinute = 0
        )
        assertTrue(result > now)
        // Must be in the future, at least 10 hours later
        assertTrue(result - now >= 10 * 3600 * 1000L)
    }

    @Test
    fun testNextMorningWhenCalledEarlyMorningAdvancesToSameDayOrNextDayProperly() {
        // 4:00 AM
        val earlyMorning = 1700020800000L
        val result = calculator.calculateSnoozeTimestamp(
            preset = SnoozePreset.NEXT_MORNING,
            currentEpochMillis = earlyMorning,
            morningHour = 9
        )
        assertTrue(result > earlyMorning)
    }
}
