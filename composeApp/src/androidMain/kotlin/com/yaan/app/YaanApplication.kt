package com.yaan.app

import android.app.Application
import com.yaan.app.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class YaanApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Napier logging in Debug
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }

        // Initialize Koin DI graph
        initKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)
            androidContext(this@YaanApplication)
        }
    }
}
