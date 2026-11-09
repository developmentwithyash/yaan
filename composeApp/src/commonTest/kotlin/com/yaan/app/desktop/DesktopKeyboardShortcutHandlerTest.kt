package com.yaan.app.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopKeyboardShortcutHandlerTest {

    @Test
    fun testUnrecognizedKeyReturnsFalse() {
        var triggeredAction: KeyboardShortcutAction? = null
        // Key down without modifier
        val handled = DesktopKeyboardShortcutHandler.processKey(
            isCtrlOrMeta = false,
            key = Key.A,
            eventType = KeyEventType.KeyDown
        ) { triggeredAction = it }

        assertFalse(handled)
        assertEquals(null, triggeredAction)
    }

    @Test
    fun testKeyUpIsIgnored() {
        var triggeredAction: KeyboardShortcutAction? = null
        val handled = DesktopKeyboardShortcutHandler.processKey(
            isCtrlOrMeta = true,
            key = Key.K,
            eventType = KeyEventType.KeyUp
        ) { triggeredAction = it }

        assertFalse(handled)
        assertEquals(null, triggeredAction)
    }

    @Test
    fun testCmdCtrlKTriggersQuickCapture() {
        var triggeredAction: KeyboardShortcutAction? = null
        val handled = DesktopKeyboardShortcutHandler.processKey(
            isCtrlOrMeta = true,
            key = Key.K,
            eventType = KeyEventType.KeyDown
        ) { triggeredAction = it }

        assertTrue(handled)
        assertEquals(KeyboardShortcutAction.OPEN_QUICK_CAPTURE, triggeredAction)
    }

    @Test
    fun testCmdCtrlNTriggersNotes() {
        var triggeredAction: KeyboardShortcutAction? = null
        val handled = DesktopKeyboardShortcutHandler.processKey(
            isCtrlOrMeta = true,
            key = Key.N,
            eventType = KeyEventType.KeyDown
        ) { triggeredAction = it }

        assertTrue(handled)
        assertEquals(KeyboardShortcutAction.NAVIGATE_NOTES, triggeredAction)
    }

    @Test
    fun testEscapeTriggersDismissModal() {
        var triggeredAction: KeyboardShortcutAction? = null
        val handled = DesktopKeyboardShortcutHandler.processKey(
            isCtrlOrMeta = false,
            key = Key.Escape,
            eventType = KeyEventType.KeyDown
        ) { triggeredAction = it }

        assertTrue(handled)
        assertEquals(KeyboardShortcutAction.DISMISS_MODAL, triggeredAction)
    }

    @Test
    fun testNavigationNumbersSwitchTabs() {
        val cases = listOf(
            Key.One to KeyboardShortcutAction.NAVIGATE_HOME,
            Key.Two to KeyboardShortcutAction.NAVIGATE_PLAN,
            Key.Three to KeyboardShortcutAction.NAVIGATE_NOTES,
            Key.Four to KeyboardShortcutAction.NAVIGATE_YOU
        )

        for ((key, expectedAction) in cases) {
            var action: KeyboardShortcutAction? = null
            val handled = DesktopKeyboardShortcutHandler.processKey(
                isCtrlOrMeta = true,
                key = key,
                eventType = KeyEventType.KeyDown
            ) { action = it }

            assertTrue(handled, "Expected Key $key to be handled")
            assertEquals(expectedAction, action)
        }
    }
}
