package com.yaan.app.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopTrayCoordinatorTest {

    @Test
    fun testInitialState() {
        val coordinator = DesktopTrayCoordinator(minimizeToTrayOnClose = true)

        assertTrue(coordinator.isWindowVisible)
        assertFalse(coordinator.isNotificationsMuted)
        assertFalse(coordinator.shouldExitApplication)
        assertEquals("YAAN — Local AI Assistant", coordinator.trayTooltip)
    }

    @Test
    fun testToggleMuteNotifications() {
        val coordinator = DesktopTrayCoordinator()

        assertFalse(coordinator.isNotificationsMuted)
        coordinator.handleAction(DesktopTrayAction.TOGGLE_MUTE_NOTIFICATIONS)
        assertTrue(coordinator.isNotificationsMuted)
        coordinator.handleAction(DesktopTrayAction.TOGGLE_MUTE_NOTIFICATIONS)
        assertFalse(coordinator.isNotificationsMuted)
    }

    @Test
    fun testOpenAppActionRestoresWindow() {
        val coordinator = DesktopTrayCoordinator()
        coordinator.isWindowVisible = false

        coordinator.handleAction(DesktopTrayAction.OPEN_APP)
        assertTrue(coordinator.isWindowVisible)
    }

    @Test
    fun testWindowCloseMinimizesToTrayWhenEnabled() {
        val coordinator = DesktopTrayCoordinator(minimizeToTrayOnClose = true)

        coordinator.onWindowCloseRequested()
        assertFalse(coordinator.isWindowVisible)
        assertFalse(coordinator.shouldExitApplication)
    }

    @Test
    fun testWindowCloseExitsWhenMinimizeToTrayDisabled() {
        val coordinator = DesktopTrayCoordinator(minimizeToTrayOnClose = false)

        coordinator.onWindowCloseRequested()
        assertTrue(coordinator.shouldExitApplication)
    }

    @Test
    fun testQuitActionTriggersExit() {
        val coordinator = DesktopTrayCoordinator()

        coordinator.handleAction(DesktopTrayAction.QUIT)
        assertTrue(coordinator.shouldExitApplication)
    }

    @Test
    fun testQuickCaptureActionDispatchesCallback() {
        var quickCaptureInvoked = false
        val coordinator = DesktopTrayCoordinator(
            onQuickCaptureRequested = { quickCaptureInvoked = true }
        )

        coordinator.handleAction(DesktopTrayAction.QUICK_CAPTURE)
        assertTrue(quickCaptureInvoked)
        assertTrue(coordinator.isWindowVisible)
    }
}
