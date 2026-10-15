package com.yaan.app

import androidx.compose.ui.window.ComposeUIViewController
import com.yaan.app.di.initKoin
import com.yaan.app.notification.IosAlarmScheduler
import com.yaan.core.domain.alarm.AlarmScheduler
import org.koin.dsl.module
import platform.UIKit.UIViewController

private var koinInitialized = false

fun MainViewController(): UIViewController {
    if (!koinInitialized) {
        initKoin {
            modules(
                module {
                    single<AlarmScheduler> { IosAlarmScheduler() }
                }
            )
        }
        koinInitialized = true
    }
    return ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        }
    ) {
        YaanApp()
    }
}
