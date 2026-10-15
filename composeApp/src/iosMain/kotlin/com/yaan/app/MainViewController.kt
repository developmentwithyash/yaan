package com.yaan.app

import androidx.compose.ui.window.ComposeUIViewController
import com.yaan.app.di.initKoin
import platform.UIKit.UIViewController

private var koinInitialized = false

fun MainViewController(): UIViewController {
    if (!koinInitialized) {
        initKoin()
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
