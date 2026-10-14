package com.yaan.core.common.datetime

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Editorial and system DateTime utility extensions.
 */
fun nowInstant(): Instant = Clock.System.now()

fun Instant.toLocalDateTimeSystem(): LocalDateTime =
    toLocalDateTime(TimeZone.currentSystemDefault())

fun Instant.formatTimeOnly(): String {
    val dt = toLocalDateTimeSystem()
    val hour = dt.hour
    val minute = dt.minute.toString().padStart(2, '0')
    val amPm = if (hour >= 12) "PM" else "AM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:$minute $amPm"
}

fun LocalDate.formatEditorialDate(): String {
    val monthName = when (monthNumber) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
    val dayOfWeekName = when (dayOfWeek.name) {
        "MONDAY" -> "Monday"
        "TUESDAY" -> "Tuesday"
        "WEDNESDAY" -> "Wednesday"
        "THURSDAY" -> "Thursday"
        "FRIDAY" -> "Friday"
        "SATURDAY" -> "Saturday"
        "SUNDAY" -> "Sunday"
        else -> dayOfWeek.name
    }
    return "$dayOfWeekName, $monthName $dayOfMonth"
}
