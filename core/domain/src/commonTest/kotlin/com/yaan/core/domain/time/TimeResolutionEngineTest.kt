package com.yaan.core.domain.time

import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TimeResolutionEngineTest {

    private val engine = TimeResolutionEngine()

    // 2026-10-15T10:00:00Z (Thursday)
    private val refThursdayEpochMs = 1792058400000L

    // 2026-10-16T10:00:00Z (Friday)
    private val refFridayEpochMs = 1792144800000L

    @Test
    fun testRelativeExpressionsTomorrow() {
        val result = engine.resolve("tomorrow morning", refThursdayEpochMs)
        assertTrue(result.isResolved)
        assertFalse(result.requiresClarification)
        assertNotNull(result.resolvedEpochMs)

        val local = Instant.fromEpochMilliseconds(result.resolvedEpochMs!!)
            .toLocalDateTime(TimeZone.of("Asia/Kolkata"))
        assertEquals(16, local.dayOfMonth)
        assertEquals(9, local.hour)
    }

    @Test
    fun testRelativeOffsetHoursAndMinutes() {
        val resHours = engine.resolve("in 2 hours", refThursdayEpochMs)
        assertTrue(resHours.isResolved)
        assertEquals(refThursdayEpochMs + (2 * 3600 * 1000), resHours.resolvedEpochMs)

        val resMinutes = engine.resolve("in 30 mins", refThursdayEpochMs)
        assertTrue(resMinutes.isResolved)
        assertEquals(refThursdayEpochMs + (30 * 60 * 1000), resMinutes.resolvedEpochMs)
    }

    @Test
    fun testAmbiguityDetectionFridayWhenTodayIsFriday() {
        val result = engine.resolve("Friday", refFridayEpochMs)
        assertFalse(result.isResolved)
        assertTrue(result.requiresClarification)
        assertNotNull(result.clarificationQuestion)
        assertTrue(result.clarificationQuestion!!.contains("Today is Friday"))
    }

    @Test
    fun testAmbiguityDetectionBareHourWithoutAmPm() {
        val result = engine.resolve("at 5", refThursdayEpochMs)
        assertFalse(result.isResolved)
        assertTrue(result.requiresClarification)
        assertNotNull(result.clarificationQuestion)
        assertTrue(result.clarificationQuestion!!.contains("5:00 AM or 5:00 PM"))
    }

    @Test
    fun testEndOfMonthLeapYearAndNonLeapYear() {
        assertEquals(28, TimeResolutionEngine.daysInMonth(2026, Month.FEBRUARY))
        assertEquals(29, TimeResolutionEngine.daysInMonth(2024, Month.FEBRUARY))
        assertEquals(31, TimeResolutionEngine.daysInMonth(2026, Month.OCTOBER))
        assertEquals(30, TimeResolutionEngine.daysInMonth(2026, Month.NOVEMBER))

        val endOfMonth = engine.resolve("end of month", refThursdayEpochMs)
        assertTrue(endOfMonth.isResolved)
        val local = Instant.fromEpochMilliseconds(endOfMonth.resolvedEpochMs!!)
            .toLocalDateTime(TimeZone.of("Asia/Kolkata"))
        assertEquals(31, local.dayOfMonth)
    }

    @Test
    fun testRecurrenceRruleParsing() {
        val daily = engine.resolve("every day", refThursdayEpochMs)
        assertTrue(daily.isResolved)
        assertEquals("RRULE:FREQ=DAILY", daily.recurrenceRule)

        val monday = engine.resolve("every monday at 9", refThursdayEpochMs)
        assertTrue(monday.isResolved)
        assertEquals("RRULE:FREQ=WEEKLY;BYDAY=MO", monday.recurrenceRule)
    }

    @Test
    fun testDstResilienceAcrossDstTransition() {
        // America/New_York spring-forward transition happens on 2026-03-08
        val nyZone = TimeZone.of("America/New_York")
        // Reference: 2026-03-07T12:00:00Z
        val preDstEpochMs = 1772884800000L

        val tomorrowMorning = engine.resolve("tomorrow morning", preDstEpochMs, timeZone = nyZone)
        assertTrue(tomorrowMorning.isResolved)
        assertNotNull(tomorrowMorning.resolvedEpochMs)

        // Instant resolves safely without throwing IllegalStateException on gap
        val resolved = Instant.fromEpochMilliseconds(tomorrowMorning.resolvedEpochMs!!)
            .toLocalDateTime(nyZone)
        assertEquals(8, resolved.dayOfMonth)
        assertEquals(9, resolved.hour)
    }
}
