package com.yaan.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.repository.ReminderRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.context.GlobalContext

/**
 * BroadcastReceiver responsible for rescheduling pending reminders when the device reboots,
 * when the app is updated, or when the system timezone or clock changes.
 *
 * Guarantees Critical User Journey 6: Reminders survive process death and reboot.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED
        ) {
            return
        }

        Napier.d("BootCompletedReceiver: System event received [$action], rescheduling pending reminders")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = GlobalContext.get().getOrNull<ReminderRepository>() ?: return@launch
                val scheduler = AndroidAlarmScheduler(context)

                val now = Clock.System.now()
                val remindersResult = repo.getReminders()

                if (remindersResult is com.yaan.core.common.result.YaanResult.Success) {
                    val pendingReminders = remindersResult.data.filter {
                        it.ackState == AlarmAckState.PENDING || it.ackState == AlarmAckState.SNOOZED
                    }

                    var rescheduledCount = 0
                    for (reminder in pendingReminders) {
                        if (reminder.triggerTime > now) {
                            scheduler.schedule(reminder)
                            rescheduledCount++
                        } else {
                            // Missed while device was powered off: fire missed alert immediately
                            val fireIntent = Intent(context, AlarmBroadcastReceiver::class.java).apply {
                                this.action = AlarmBroadcastReceiver.ACTION_FIRE_REMINDER
                                putExtra(AlarmBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
                                putExtra(AlarmBroadcastReceiver.EXTRA_REMINDER_TITLE, "${reminder.title} (Missed)")
                                putExtra(AlarmBroadcastReceiver.EXTRA_IS_ALARM_SOUND, false)
                            }
                            context.sendBroadcast(fireIntent)
                        }
                    }
                    Napier.i("BootCompletedReceiver: Rescheduled $rescheduledCount reminders successfully")
                }
            } catch (e: Exception) {
                Napier.e("BootCompletedReceiver: Failed to reschedule reminders: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
