package com.yaan.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yaan.app.desktop.DesktopKeyboardShortcutHandler
import com.yaan.app.desktop.KeyboardShortcutAction

fun main() = application {
    var shortcutHandler: ((KeyboardShortcutAction) -> Unit)? = null

    Window(
        onCloseRequest = ::exitApplication,
        title = "YAAN — Your Autonomous Action Navigator (Desktop)",
        state = rememberWindowState(width = 440.dp, height = 880.dp),
        onKeyEvent = { keyEvent ->
            shortcutHandler?.let { handler ->
                DesktopKeyboardShortcutHandler.handleKeyEvent(keyEvent, handler)
            } ?: false
        }
    ) {
        YaanApp(
            onRegisterShortcutHandler = { handler ->
                shortcutHandler = handler
            }
        )
    }
}
