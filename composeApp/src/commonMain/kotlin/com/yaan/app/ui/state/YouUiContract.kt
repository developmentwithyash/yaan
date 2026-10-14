package com.yaan.app.ui.state

/**
 * Pure Presentation State for the You / Settings screen.
 */
data class YouUiState(
    val isDarkTheme: Boolean = false,
    val notificationHealth: String = "100% SLA Active",
    val batteryHealthScore: String = "Optimal (0 wakelock leaks)",
    val vaultItemsCount: Int = 42,
    val vaultSizeBytes: String = "1.4 MB (Encrypted)",
    val snapshotArmed: Boolean = false,
    val isPurgeConfirmed: Boolean = false
)

/**
 * User Actions and Intents emitted by YouScreen.
 */
sealed interface YouUiAction {
    data object ToggleDarkTheme : YouUiAction
    data object ToggleSnapshotArmed : YouUiAction
    data object ExportVault : YouUiAction
    data object PurgeVault : YouUiAction
}
