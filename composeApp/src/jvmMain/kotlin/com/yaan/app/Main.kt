package com.yaan.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "YAAN — Your Autonomous Action Navigator (Desktop)",
        state = rememberWindowState(width = 440.dp, height = 880.dp)
    ) {
        YaanApp()
    }
}
