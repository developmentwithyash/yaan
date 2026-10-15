package com.yaan.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.yaan.app.di.appModules
import com.yaan.app.notification.AlarmBroadcastReceiver
import com.yaan.app.notification.AndroidAlarmScheduler
import com.yaan.core.domain.alarm.AlarmScheduler
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

val androidPlatformModule = module {
    single<AlarmScheduler> { AndroidAlarmScheduler(androidContext()) }
}

class YaanApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Napier logging in Debug
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        // Initialize notification channels
        createNotificationChannels()

        // Initialize Koin DI graph with Android platform bindings
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)
            androidContext(this@YaanApplication)
            modules(appModules + androidPlatformModule)
        }
    }

    private fun createNotificationChannels() {
        val remindersChannel = NotificationChannel(
            AlarmBroadcastReceiver.CHANNEL_REMINDERS_ID,
            "YAAN Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical personal reminders and scheduled tasks"
            enableVibration(true)
        }

        val alarmsChannel = NotificationChannel(
            AlarmBroadcastReceiver.CHANNEL_ALARMS_ID,
            "YAAN Persistent Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority wake alarms with persistent sound"
            enableVibration(true)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(remindersChannel)
        notificationManager?.createNotificationChannel(alarmsChannel)
    }
}
