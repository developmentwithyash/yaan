package com.yaan.core.domain.alarm

import com.yaan.core.domain.model.Reminder

/**
 * Platform-agnostic contract for scheduling and cancelling exact system alarms and reminders.
 * Implemented on Android via AlarmManager and on iOS via UNUserNotificationCenter.
 */
interface AlarmScheduler {
    /**
     * Schedules an exact system alarm / notification for the given reminder.
     */
    fun schedule(reminder: Reminder)

    /**
     * Cancels any pending system alarm / notification for the given reminder identifier.
     */
    fun cancel(reminderId: String)
}

/**
 * No-op alarm scheduler used in platforms or tests without native alarm daemons.
 */
class NoOpAlarmScheduler : AlarmScheduler {
    override fun schedule(reminder: Reminder) = Unit
    override fun cancel(reminderId: String) = Unit
}
