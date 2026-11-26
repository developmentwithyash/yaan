package com.yaan.feature.briefing.schedule

/**
 * Encapsulates the user's preferred daily Morning Briefing delivery time.
 * Validates boundaries (00:00 to 23:59), formats 12-hour AM/PM representations,
 * and calculates the exact next trigger epoch timestamp.
 */
data class BriefingDeliveryTimePreference(
    val hour: Int = 8,
    val minute: Int = 0
) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23. Received: $hour" }
        require(minute in 0..59) { "Minute must be between 0 and 59. Received: $minute" }
    }

    companion object {
        const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    /**
     * Formats the time into canonical "hh:mm AM/PM" string.
     */
    fun formatDisplayTime(): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        val paddedHour = displayHour.toString().padStart(2, '0')
        val paddedMinute = minute.toString().padStart(2, '0')
        return "$paddedHour:$paddedMinute $period"
    }

    /**
     * Calculates the next epoch timestamp (in milliseconds) at which this briefing should trigger.
     *
     * @param currentEpochMillis Current wall-clock epoch timestamp.
     * @return Next trigger timestamp in epoch millis.
     */
    fun calculateNextTriggerEpochMillis(currentEpochMillis: Long): Long {
        val millisInDay = currentEpochMillis % ONE_DAY_MILLIS
        val currentDayStart = currentEpochMillis - millisInDay
        val targetOffsetToday = (hour * 3600 * 1000L) + (minute * 60 * 1000L)
        val targetToday = currentDayStart + targetOffsetToday

        return if (targetToday > currentEpochMillis) {
            targetToday
        } else {
            targetToday + ONE_DAY_MILLIS
        }
    }
}
