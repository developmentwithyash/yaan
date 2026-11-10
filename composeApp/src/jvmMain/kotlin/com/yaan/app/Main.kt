package com.yaan.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yaan.app.desktop.DesktopKeyboardShortcutHandler
import com.yaan.app.desktop.DesktopWindowStateManager
import com.yaan.app.desktop.JvmDesktopPreferencesStore
import com.yaan.app.desktop.KeyboardShortcutAction
import java.awt.Dimension

fun main() = application {
    val windowStateManager = DesktopWindowStateManager(JvmDesktopPreferencesStore())
    val savedState = windowStateManager.loadWindowState()

    val initialPosition = if (savedState.positionXDp != null && savedState.positionYDp != null) {
        WindowPosition(savedState.positionXDp.dp, savedState.positionYDp.dp)
    } else {
        WindowPosition.PlatformDefault
    }

    val windowState = rememberWindowState(
        width = savedState.widthDp.dp,
        height = savedState.heightDp.dp,
        position = initialPosition
    )

    var shortcutHandler: ((KeyboardShortcutAction) -> Unit)? = null

    Window(
        onCloseRequest = {
            windowStateManager.saveWindowState(
                widthDp = windowState.size.width.value.toInt(),
                heightDp = windowState.size.height.value.toInt(),
                positionXDp = (windowState.position as? WindowPosition.Absolute)?.x?.value?.toInt(),
                positionYDp = (windowState.position as? WindowPosition.Absolute)?.y?.value?.toInt()
            )
            exitApplication()
        },
        title = "YAAN — Your Autonomous Action Navigator (Desktop)",
        state = windowState,
        onKeyEvent = { keyEvent ->
            shortcutHandler?.let { handler ->
                DesktopKeyboardShortcutHandler.handleKeyEvent(keyEvent, handler)
            } ?: false
        }
    ) {
        // Enforce minimum window dimension constraints
        window.minimumSize = Dimension(
            DesktopWindowStateManager.MIN_WIDTH_DP,
            DesktopWindowStateManager.MIN_HEIGHT_DP
        )

        YaanApp(
            onRegisterShortcutHandler = { handler ->
                shortcutHandler = handler
            }
        )
    }
}
