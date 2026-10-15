package com.yaan.app.ui.state

/**
 * Pure Presentation State for the Plan & Reminders screen.
 */
data class PlanUiState(
    val selectedTab: String = "Today",
    val availableTabs: List<String> = listOf("Today", "Upcoming", "Routines", "Completed"),
    val selectedDay: Int = 24,
    val fastActionText: String = "",
    val isDueNextDone: Boolean = false,
    val task1Done: Boolean = false,
    val task2Done: Boolean = false,
    val task3Done: Boolean = false,
    val stream1Done: Boolean = true,
    val stream2Done: Boolean = false,
    val stream3Done: Boolean = false,
    val isRoutineRunning: Boolean = false,
    val isPermissionWarningDismissed: Boolean = false
)

/**
 * User Actions and Intents emitted by PlanScreen.
 */
sealed interface PlanUiAction {
    data class SelectTab(val tab: String) : PlanUiAction
    data class SelectDay(val day: Int) : PlanUiAction
    data class UpdateFastAction(val text: String) : PlanUiAction
    data class ToggleTask(val taskKey: String) : PlanUiAction
    data object StartRoutine : PlanUiAction
    data object DismissPermissionWarning : PlanUiAction
}
