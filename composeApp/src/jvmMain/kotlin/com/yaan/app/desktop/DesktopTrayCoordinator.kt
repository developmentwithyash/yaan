package com.yaan.app.desktop

/**
 * Tray context menu actions supported on Compose Desktop targets.
 */
enum class DesktopTrayAction {
    OPEN_APP,
    QUICK_CAPTURE,
    TOGGLE_MUTE_NOTIFICATIONS,
    QUIT
}

/**
 * Enterprise coordinator governing System Tray (macOS MenuBar, Windows/Linux notification area)
 * integration and background reminder notification states on desktop JVM.
 */
class DesktopTrayCoordinator(
    var minimizeToTrayOnClose: Boolean = true,
    private val onQuickCaptureRequested: (() -> Unit)? = null
) {
    var isWindowVisible: Boolean = true
    var isNotificationsMuted: Boolean = false
        private set
    var shouldExitApplication: Boolean = false
        private set

    val trayTooltip: String = DEFAULT_TRAY_TOOLTIP

    /**
     * Dispatches tray context menu actions.
     */
    fun handleAction(action: DesktopTrayAction) {
        when (action) {
            DesktopTrayAction.OPEN_APP -> {
                isWindowVisible = true
            }
            DesktopTrayAction.QUICK_CAPTURE -> {
                isWindowVisible = true
                onQuickCaptureRequested?.invoke()
            }
            DesktopTrayAction.TOGGLE_MUTE_NOTIFICATIONS -> {
                isNotificationsMuted = !isNotificationsMuted
            }
            DesktopTrayAction.QUIT -> {
                shouldExitApplication = true
            }
        }
    }

    /**
     * Intercepts the window close request.
     * If minimize to tray is enabled, the window is hidden while keeping the process running.
     */
    fun onWindowCloseRequested() {
        if (minimizeToTrayOnClose) {
            isWindowVisible = false
        } else {
            shouldExitApplication = true
        }
    }

    companion object {
        const val DEFAULT_TRAY_TOOLTIP = "YAAN — Local AI Assistant"
    }
}
