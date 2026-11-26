package com.yaan.feature.reminders.snooze

/**
 * Calculates exact target trigger timestamps for reminder snooze presets.
 * Handles deterministic interval addition and next-morning calendar roll-forwards.
 */
class SnoozePresetCalculator {

    companion object {
        const val FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1000L
        const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
        const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    /**
     * Computes the epoch timestamp in milliseconds for a selected [preset].
     *
     * @param preset The chosen quick snooze preset.
     * @param currentEpochMillis The baseline epoch timestamp (current time).
     * @param morningHour Target hour for NEXT_MORNING (0-23, default 9).
     * @param morningMinute Target minute for NEXT_MORNING (0-59, default 0).
     * @return Target trigger epoch millis.
     */
    fun calculateSnoozeTimestamp(
        preset: SnoozePreset,
        currentEpochMillis: Long,
        morningHour: Int = 9,
        morningMinute: Int = 0
    ): Long {
        return when (preset) {
            SnoozePreset.FIFTEEN_MINUTES -> currentEpochMillis + FIFTEEN_MINUTES_MILLIS
            SnoozePreset.ONE_HOUR -> currentEpochMillis + ONE_HOUR_MILLIS
            SnoozePreset.NEXT_MORNING -> calculateNextMorning(currentEpochMillis, morningHour, morningMinute)
        }
    }

    private fun calculateNextMorning(
        currentEpochMillis: Long,
        morningHour: Int,
        morningMinute: Int
    ): Long {
        val millisInDay = currentEpochMillis % ONE_DAY_MILLIS
        val currentDayStart = currentEpochMillis - millisInDay
        val targetOffsetToday = (morningHour * 3600 * 1000L) + (morningMinute * 60 * 1000L)
        val targetToday = currentDayStart + targetOffsetToday

        return if (targetToday > currentEpochMillis + (30 * 60 * 1000L)) {
            // Target is later today and at least 30 minutes away
            targetToday
        } else {
            // Target is already past or too close; roll to tomorrow
            targetToday + ONE_DAY_MILLIS
        }
    }
}
