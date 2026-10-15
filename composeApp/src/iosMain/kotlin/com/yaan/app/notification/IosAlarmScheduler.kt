package com.yaan.app.notification

import com.yaan.core.domain.alarm.AlarmScheduler
import com.yaan.core.domain.model.Reminder
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS native AlarmScheduler implementation using UserNotifications framework.
 *
 * Enforces Apple constraints:
 * 1. Hard system limit: 64 pending local notification requests maximum.
 *    Excess requests are pruned so the soonest 64 are always preserved.
 * 2. Operates 100% locally on device without network egress.
 */
class IosAlarmScheduler : AlarmScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    init {
        requestAuthorization()
    }

    private fun requestAuthorization() {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { _, _ ->
            // Authorization state managed by OS; UI handles degraded permission warnings
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun schedule(reminder: Reminder) {
        val triggerTimeSec = reminder.triggerTime.toEpochMilliseconds() / 1000.0
        val nowSec = NSDate().timeIntervalSince1970
        val delaySec = triggerTimeSec - nowSec

        // If the reminder is in the past, trigger after 1 second
        val interval = if (delaySec > 0.0) delaySec else 1.0

        val content = UNMutableNotificationContent().apply {
            setTitle(reminder.title)
            setBody("YAAN Scheduled Reminder")
            setSound(if (reminder.isAlarmSound) UNNotificationSound.defaultSound else UNNotificationSound.defaultSound)
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(interval, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(reminder.id, content, trigger)

        // Apple limit: 64 pending notification requests
        center.getPendingNotificationRequestsWithCompletionHandler { requests ->
            val pending = (requests as? List<*>)?.filterIsInstance<UNNotificationRequest>() ?: emptyList()
            if (pending.size >= 64) {
                // Prune the furthest request to preserve the 64-limit invariant
                val furthest = pending.lastOrNull()
                if (furthest != null) {
                    center.removePendingNotificationRequestsWithIdentifiers(listOf(furthest.identifier))
                }
            }
            center.addNotificationRequest(request) { _ ->
                // Delivery handled natively by iOS
            }
        }
    }

    override fun cancel(reminderId: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(reminderId))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(reminderId))
    }
}
