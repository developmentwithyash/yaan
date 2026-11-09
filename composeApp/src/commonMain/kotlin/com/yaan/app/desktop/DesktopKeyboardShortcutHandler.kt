package com.yaan.app.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Standardized global shortcut intents for YAAN Desktop & Web targets.
 */
enum class KeyboardShortcutAction {
    OPEN_QUICK_CAPTURE,
    NAVIGATE_HOME,
    NAVIGATE_PLAN,
    NAVIGATE_NOTES,
    NAVIGATE_YOU,
    DISMISS_MODAL,
    TOGGLE_THEME
}

/**
 * Keyboard event processor for desktop window shortcuts (Closes #21).
 */
object DesktopKeyboardShortcutHandler {

    /**
     * Testable core processor decoupled from native platform KeyEvent internals.
     */
    fun processKey(
        isCtrlOrMeta: Boolean,
        key: Key,
        eventType: KeyEventType,
        onAction: (KeyboardShortcutAction) -> Unit
    ): Boolean {
        if (eventType != KeyEventType.KeyDown) return false

        return when {
            isCtrlOrMeta && (key == Key.K) -> {
                onAction(KeyboardShortcutAction.OPEN_QUICK_CAPTURE)
                true
            }
            isCtrlOrMeta && (key == Key.N) -> {
                onAction(KeyboardShortcutAction.NAVIGATE_NOTES)
                true
            }
            isCtrlOrMeta && (key == Key.One) -> {
                onAction(KeyboardShortcutAction.NAVIGATE_HOME)
                true
            }
            isCtrlOrMeta && (key == Key.Two) -> {
                onAction(KeyboardShortcutAction.NAVIGATE_PLAN)
                true
            }
            isCtrlOrMeta && (key == Key.Three) -> {
                onAction(KeyboardShortcutAction.NAVIGATE_NOTES)
                true
            }
            isCtrlOrMeta && (key == Key.Four) -> {
                onAction(KeyboardShortcutAction.NAVIGATE_YOU)
                true
            }
            isCtrlOrMeta && (key == Key.T) -> {
                onAction(KeyboardShortcutAction.TOGGLE_THEME)
                true
            }
            key == Key.Escape -> {
                onAction(KeyboardShortcutAction.DISMISS_MODAL)
                true
            }
            else -> false
        }
    }

    /**
     * Primary entrypoint for Compose KeyEvent routing.
     */
    fun handleKeyEvent(
        event: KeyEvent,
        onAction: (KeyboardShortcutAction) -> Unit
    ): Boolean {
        val isModifier = event.isCtrlPressed || event.isMetaPressed
        return processKey(
            isCtrlOrMeta = isModifier,
            key = event.key,
            eventType = event.type,
            onAction = onAction
        )
    }
}
