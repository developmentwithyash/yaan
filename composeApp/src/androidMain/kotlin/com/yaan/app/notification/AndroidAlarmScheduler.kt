package com.yaan.app.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yaan.core.domain.alarm.AlarmScheduler
import com.yaan.core.domain.model.Reminder

/**
 * Android native AlarmScheduler using AlarmManager with exact while-idle delivery.
 * Enforces Zero-Knowledge Invariant: Operates 100% locally on device without network.
 */
class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    @SuppressLint("MissingPermission")
    override fun schedule(reminder: Reminder) {
        if (alarmManager == null) return

        val intent = createAlarmIntent(reminder)
        val pendingIntent = createPendingIntent(reminder.id, intent)
        val triggerAtMillis = reminder.triggerTime.toEpochMilliseconds()

        // Check if exact alarms are permitted (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    override fun cancel(reminderId: String) {
        if (alarmManager == null) return

        val intent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_REMINDER
        }
        val requestCode = reminderId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun createAlarmIntent(reminder: Reminder): Intent {
        return Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = AlarmBroadcastReceiver.ACTION_FIRE_REMINDER
            putExtra(AlarmBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmBroadcastReceiver.EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(AlarmBroadcastReceiver.EXTRA_IS_ALARM_SOUND, reminder.isAlarmSound)
        }
    }

    private fun createPendingIntent(reminderId: String, intent: Intent): PendingIntent {
        val requestCode = reminderId.hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
