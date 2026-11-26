package com.yaan.feature.briefing.schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BriefingDeliveryTimePreferenceTest {

    @Test
    fun testDefaultPreferenceIsEightAm() {
        val pref = BriefingDeliveryTimePreference()
        assertEquals(8, pref.hour)
        assertEquals(0, pref.minute)
        assertEquals("08:00 AM", pref.formatDisplayTime())
    }

    @Test
    fun testValidCustomDeliveryTime() {
        val pref = BriefingDeliveryTimePreference(hour = 7, minute = 30)
        assertEquals(7, pref.hour)
        assertEquals(30, pref.minute)
        assertEquals("07:30 AM", pref.formatDisplayTime())
    }

    @Test
    fun testAfternoonDeliveryTimeFormat() {
        val pref = BriefingDeliveryTimePreference(hour = 17, minute = 45)
        assertEquals("05:45 PM", pref.formatDisplayTime())
    }

    @Test
    fun testInvalidHourThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            BriefingDeliveryTimePreference(hour = 24, minute = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            BriefingDeliveryTimePreference(hour = -1, minute = 0)
        }
    }

    @Test
    fun testInvalidMinuteThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            BriefingDeliveryTimePreference(hour = 8, minute = 60)
        }
        assertFailsWith<IllegalArgumentException> {
            BriefingDeliveryTimePreference(hour = 8, minute = -5)
        }
    }

    @Test
    fun testCalculateNextTriggerEpochMillisAdvancesCorrectly() {
        val pref = BriefingDeliveryTimePreference(hour = 9, minute = 0)
        // Baseline epoch time (12:00 PM today)
        val currentEpochMillis = 1700050000000L
        val nextTrigger = pref.calculateNextTriggerEpochMillis(currentEpochMillis)

        assertTrue(nextTrigger > currentEpochMillis)
        // Must be in the future, less than or equal to 24 hours away
        val diff = nextTrigger - currentEpochMillis
        assertTrue(diff in 1..(24 * 3600 * 1000L))
    }
}
