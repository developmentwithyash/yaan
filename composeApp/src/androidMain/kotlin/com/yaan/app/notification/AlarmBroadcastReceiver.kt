package com.yaan.app.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.yaan.app.MainActivity
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.model.Reminder
import com.yaan.core.domain.model.ReminderTier
import com.yaan.core.domain.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.context.GlobalContext
import kotlin.time.Duration.Companion.minutes

/**
 * BroadcastReceiver responsible for receiving scheduled AlarmManager intents,
 * posting system notifications, and handling user actions (Acknowledge / Snooze).
 */
class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        when (action) {
            ACTION_FIRE_REMINDER -> {
                val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "YAAN Reminder"
                val isAlarmSound = intent.getBooleanExtra(EXTRA_IS_ALARM_SOUND, false)
                postNotification(context, notificationManager, reminderId, title, isAlarmSound)
            }

            ACTION_ACKNOWLEDGE -> {
                val notificationId = reminderId.hashCode()
                notificationManager.cancel(notificationId)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = GlobalContext.get().getOrNull<ReminderRepository>()
                        repo?.updateAckState(reminderId, AlarmAckState.DISMISSED)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE -> {
                val notificationId = reminderId.hashCode()
                notificationManager.cancel(notificationId)

                val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "YAAN Reminder"
                val isAlarmSound = intent.getBooleanExtra(EXTRA_IS_ALARM_SOUND, false)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = GlobalContext.get().getOrNull<ReminderRepository>()
                        val newTriggerTime = Clock.System.now() + 10.minutes
                        val snoozedReminder = Reminder(
                            id = reminderId,
                            title = title,
                            triggerTime = newTriggerTime,
                            tier = if (isAlarmSound) ReminderTier.PERSISTENT_ALARM else ReminderTier.NORMAL,
                            isAlarmSound = isAlarmSound,
                            ackState = AlarmAckState.SNOOZED,
                            createdAt = Clock.System.now()
                        )
                        repo?.scheduleReminder(snoozedReminder)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun postNotification(
        context: Context,
        notificationManager: NotificationManager,
        reminderId: String,
        title: String,
        isAlarmSound: Boolean
    ) {
        val notificationId = reminderId.hashCode()

        // Content Intent (opens app)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acknowledge Action
        val ackIntent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = ACTION_ACKNOWLEDGE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            ackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action
        val snoozeIntent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_IS_ALARM_SOUND, isAlarmSound)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isAlarmSound) CHANNEL_ALARMS_ID else CHANNEL_REMINDERS_ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("YAAN Reminder")
            .setContentText(title)
            .setPriority(if (isAlarmSound) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isAlarmSound) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Acknowledge", ackPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snoozePendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val CHANNEL_REMINDERS_ID = "yaan_reminders"
        const val CHANNEL_ALARMS_ID = "yaan_alarms"

        const val ACTION_FIRE_REMINDER = "com.yaan.app.ACTION_FIRE_REMINDER"
        const val ACTION_ACKNOWLEDGE = "com.yaan.app.ACTION_ACKNOWLEDGE"
        const val ACTION_SNOOZE = "com.yaan.app.ACTION_SNOOZE"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_IS_ALARM_SOUND = "extra_is_alarm_sound"
    }
}
