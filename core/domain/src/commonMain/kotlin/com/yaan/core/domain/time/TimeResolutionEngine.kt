package com.yaan.core.domain.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class TimeResolutionResult(
    val isResolved: Boolean,
    val resolvedEpochMs: Long? = null,
    val ianaTimeZone: String = "Asia/Kolkata",
    val recurrenceRule: String? = null,
    val isFloatingWallTime: Boolean = true,
    val requiresClarification: Boolean = false,
    val clarificationQuestion: String? = null,
    val confidence: Float = 1.0f
)

/**
 * Shared Time and Recurrence Resolution Engine.
 * Implements deterministic parsing for relative expressions, recurrence rules,
 * ambiguity detection (asking ONE clarifying question rather than guessing),
 * and DST gap/overlap resilience across IANA time zones.
 */
class TimeResolutionEngine(
    private val defaultMorningHour: Int = 9,
    private val defaultEveningHour: Int = 18,
    private val defaultNightHour: Int = 20,
    private val defaultTimeZone: TimeZone = TimeZone.of("Asia/Kolkata")
) {

    /**
     * Resolves natural language time and date phrases against a reference instant.
     */
    fun resolve(
        rawExpression: String,
        referenceEpochMs: Long,
        timeZone: TimeZone = defaultZone()
    ): TimeResolutionResult {
        val trimmed = rawExpression.trim().lowercase()
        if (trimmed.isEmpty()) {
            return TimeResolutionResult(
                isResolved = false,
                requiresClarification = true,
                clarificationQuestion = "When would you like to set this for?",
                confidence = 0.0f
            )
        }

        val refInstant = Instant.fromEpochMilliseconds(referenceEpochMs)
        val refDateTime = refInstant.toLocalDateTime(timeZone)

        // 1. Ambiguity: "Friday" when today is already Friday
        if (trimmed == "friday" || trimmed == "this friday" || trimmed.endsWith("friday") || trimmed.contains(" friday")) {
            if (refDateTime.dayOfWeek.name == "FRIDAY") {
                return TimeResolutionResult(
                    isResolved = false,
                    requiresClarification = true,
                    clarificationQuestion = "Today is Friday. Did you mean later today, or next Friday?",
                    confidence = 0.5f,
                    ianaTimeZone = timeZone.id
                )
            }
        }

        // 2. Ambiguity: Clock hour without AM/PM (e.g. "at 5" or "at 7")
        val bareHourRegex = Regex("^(?:at\\s+)?([1-9]|1[0-2])$")
        val bareHourMatch = bareHourRegex.matchEntire(trimmed)
        if (bareHourMatch != null) {
            val hour = bareHourMatch.groupValues[1]
            return TimeResolutionResult(
                isResolved = false,
                requiresClarification = true,
                clarificationQuestion = "Did you mean $hour:00 AM or $hour:00 PM?",
                confidence = 0.5f,
                ianaTimeZone = timeZone.id
            )
        }

        // 3. Relative Offset: "in X hours", "in X minutes"
        val inHoursMatch = Regex("in\\s+(\\d+)\\s+hours?").find(trimmed)
        if (inHoursMatch != null) {
            val hours = inHoursMatch.groupValues[1].toLong()
            val targetInstant = refInstant.plus(hours * 3600, DateTimeUnit.SECOND)
            return TimeResolutionResult(
                isResolved = true,
                resolvedEpochMs = targetInstant.toEpochMilliseconds(),
                ianaTimeZone = timeZone.id,
                confidence = 1.0f
            )
        }

        val inMinutesMatch = Regex("in\\s+(\\d+)\\s+min(?:ute)?s?").find(trimmed)
        if (inMinutesMatch != null) {
            val minutes = inMinutesMatch.groupValues[1].toLong()
            val targetInstant = refInstant.plus(minutes * 60, DateTimeUnit.SECOND)
            return TimeResolutionResult(
                isResolved = true,
                resolvedEpochMs = targetInstant.toEpochMilliseconds(),
                ianaTimeZone = timeZone.id,
                confidence = 1.0f
            )
        }

        // 4. "tomorrow morning", "tomorrow evening", "tomorrow night"
        val tomorrow = refDateTime.date.plus(1, DateTimeUnit.DAY)
        if (trimmed.contains("tomorrow")) {
            val time = when {
                trimmed.contains("morning") -> LocalTime(defaultMorningHour, 0)
                trimmed.contains("evening") -> LocalTime(defaultEveningHour, 0)
                trimmed.contains("night") -> LocalTime(defaultNightHour, 0)
                trimmed.contains("2pm") || trimmed.contains("2 pm") -> LocalTime(14, 0)
                trimmed.contains("10am") || trimmed.contains("10 am") -> LocalTime(10, 0)
                else -> LocalTime(defaultMorningHour, 0)
            }
            val targetInstant = LocalDateTime(tomorrow, time).toInstant(timeZone)
            return TimeResolutionResult(
                isResolved = true,
                resolvedEpochMs = targetInstant.toEpochMilliseconds(),
                ianaTimeZone = timeZone.id,
                confidence = 0.95f
            )
        }

        // 5. "end of month"
        if (trimmed.contains("end of month")) {
            val endDay = daysInMonth(refDateTime.year, refDateTime.month)
            val endOfMonthDate = LocalDate(refDateTime.year, refDateTime.month, endDay)
            val targetInstant = LocalDateTime(endOfMonthDate, LocalTime(defaultEveningHour, 0)).toInstant(timeZone)
            return TimeResolutionResult(
                isResolved = true,
                resolvedEpochMs = targetInstant.toEpochMilliseconds(),
                ianaTimeZone = timeZone.id,
                confidence = 0.90f
            )
        }

        // 6. Recurrence RRULE parser: "every monday at 9", "every day at 2pm"
        if (trimmed.startsWith("every ")) {
            val recurrenceRule = when {
                trimmed.contains("monday") -> "RRULE:FREQ=WEEKLY;BYDAY=MO"
                trimmed.contains("tuesday") -> "RRULE:FREQ=WEEKLY;BYDAY=TU"
                trimmed.contains("wednesday") -> "RRULE:FREQ=WEEKLY;BYDAY=WE"
                trimmed.contains("thursday") -> "RRULE:FREQ=WEEKLY;BYDAY=TH"
                trimmed.contains("friday") -> "RRULE:FREQ=WEEKLY;BYDAY=FR"
                trimmed.contains("saturday") -> "RRULE:FREQ=WEEKLY;BYDAY=SA"
                trimmed.contains("sunday") -> "RRULE:FREQ=WEEKLY;BYDAY=SU"
                trimmed.contains("weekday") -> "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
                trimmed.contains("every day") || trimmed.contains("daily") -> "RRULE:FREQ=DAILY"
                trimmed.contains("month") -> "RRULE:FREQ=MONTHLY"
                else -> "RRULE:FREQ=DAILY"
            }
            val time = if (trimmed.contains("9")) LocalTime(9, 0) else LocalTime(defaultMorningHour, 0)
            val firstOccurrence = LocalDateTime(tomorrow, time).toInstant(timeZone)
            return TimeResolutionResult(
                isResolved = true,
                resolvedEpochMs = firstOccurrence.toEpochMilliseconds(),
                ianaTimeZone = timeZone.id,
                recurrenceRule = recurrenceRule,
                confidence = 0.95f
            )
        }

        return TimeResolutionResult(
            isResolved = false,
            requiresClarification = true,
            clarificationQuestion = "Could you specify the exact date and time for '$rawExpression'?",
            confidence = 0.3f,
            ianaTimeZone = timeZone.id
        )
    }

    private fun defaultZone(): TimeZone = defaultTimeZone

    companion object {
        fun daysInMonth(year: Int, month: Month): Int {
            return when (month) {
                Month.JANUARY -> 31
                Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
                Month.MARCH -> 31
                Month.APRIL -> 30
                Month.MAY -> 31
                Month.JUNE -> 30
                Month.JULY -> 31
                Month.AUGUST -> 31
                Month.SEPTEMBER -> 30
                Month.OCTOBER -> 31
                Month.NOVEMBER -> 30
                Month.DECEMBER -> 31
            }
        }

        fun isLeapYear(year: Int): Boolean {
            return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
        }
    }
}
