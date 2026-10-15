package com.yaan.app.ui.state

/**
 * UI State Model for a Task Item in the presentation layer.
 * Decoupled from domain persistence entities.
 */
data class TaskItemUi(
    val id: String,
    val title: String,
    val dueTime: String,
    val tag: String,
    val isHighPriority: Boolean = false,
    val completed: Boolean = false
)

/**
 * Pure Presentation State for the Home Command Center screen.
 */
data class HomeUiState(
    val tasks: List<TaskItemUi> = defaultHomeTasks,
    val briefingHeadline: String = "Good morning, Elena",
    val briefingSummary: String = "You have 3 primary commitments today. Your next action is preparing the Stripe partnership term sheet, and 1 conflict requires rescheduling.",
    val showBriefing: Boolean = true,
    val showConflict: Boolean = true,
    val conflictAdjusted: Boolean = false,
    val conflictTitle: String = "Dentist Appointment vs Q3 Cloud Spend Review",
    val conflictTimeSlot: String = "3:00 PM – 4:00 PM",
    val conflictResolution: String = "Shift Dentist prep alert to 2:15 PM (25 min transit buffer)",
    val nextCommitmentTitle: String = "Product Architecture Sync",
    val nextCommitmentTime: String = "10:30 AM – 11:15 AM (Starts in 42m)",
    val nextCommitmentLocation: String = "Google Meet · 4 Attendees",
    val feedbackMessage: String? = null,
    val isCapturing: Boolean = false
)

val defaultHomeTasks = listOf(
    TaskItemUi(
        id = "1",
        title = "Send revised terms to Raj (Stripe partner)",
        dueTime = "Due 12:00 PM",
        tag = "High Priority",
        isHighPriority = true,
        completed = true
    ),
    TaskItemUi(
        id = "2",
        title = "Review Q3 Cloud Infra spend anomaly note",
        dueTime = "Due 3:30 PM",
        tag = "AWS Billing Alert",
        isHighPriority = false,
        completed = false
    ),
    TaskItemUi(
        id = "3",
        title = "Draft YAAN MVP release notes v0.9",
        dueTime = "Flexible",
        tag = "Product Scope",
        isHighPriority = false,
        completed = false
    )
)

/**
 * User Actions and Intents emitted by the HomeScreen presentation layer.
 */
sealed interface HomeUiAction {
    data class ToggleTask(val id: String) : HomeUiAction
    data object DismissBriefing : HomeUiAction
    data object DismissConflict : HomeUiAction
    data object AdjustConflict : HomeUiAction
    data object UndoConflictAdjustment : HomeUiAction
    data class SubmitQuickCapture(val input: String) : HomeUiAction
    data object ClearFeedback : HomeUiAction
    data object NavigateToAskYaan : HomeUiAction
    data object LaunchVoice : HomeUiAction
}
