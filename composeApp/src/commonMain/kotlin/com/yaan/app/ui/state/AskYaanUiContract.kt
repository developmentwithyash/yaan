package com.yaan.app.ui.state

/**
 * Proposed action item awaiting affirmative confirmation in Ask YAAN.
 */
data class ProposedActionUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val actionType: String = "CALENDAR_RESCHEDULE",
    val isConfirmed: Boolean = false,
    val isDismissed: Boolean = false,
    val isUndone: Boolean = false
)

/**
 * Single turn in the Ask YAAN conversation thread.
 */
data class ChatMessageUi(
    val id: String,
    val sender: String, // "USER" or "YAAN"
    val text: String,
    val timestamp: String = "Just now",
    val citations: List<String> = emptyList(),
    val proposedAction: ProposedActionUi? = null
)

/**
 * Pure Presentation State for the Ask YAAN screen.
 */
data class AskYaanUiState(
    val activeMode: String = "Chat",
    val thoughtExpanded: Boolean = false,
    val inputText: String = "",
    val proposedAction: ProposedActionUi? = ProposedActionUi(
        id = "action_01",
        title = "Reschedule Dentist Appointment to 2:15 PM",
        subtitle = "Avoids 25-minute conflict with Q3 Cloud Spend Review",
        actionType = "CALENDAR_RESCHEDULE",
        isConfirmed = false
    ),
    val isStreaming: Boolean = false,
    val responseMarkdown: String = "Based on your encrypted vault notes and schedule, I've mapped out today's priority commitments. You have a 25-minute conflict between your Dentist visit and the AWS spend review.",
    val messages: List<ChatMessageUi> = listOf(
        ChatMessageUi(
            id = "msg_init_welcome",
            sender = "YAAN",
            text = "Welcome to YAAN, your personal autonomous navigator. How can I assist you today? I can help you manage tasks, capture notes, search your vault, or brief your schedule.",
            timestamp = "Just now"
        )
    )
)

/**
 * User Actions and Intents emitted by AskYaanScreen.
 */
sealed interface AskYaanUiAction {
    data class UpdateInput(val text: String) : AskYaanUiAction
    data class SwitchMode(val mode: String) : AskYaanUiAction
    data object ToggleThoughtProcess : AskYaanUiAction
    data object ConfirmActionProposal : AskYaanUiAction
    data object DismissActionProposal : AskYaanUiAction
    data object UndoActionProposal : AskYaanUiAction
    data object SubmitQuery : AskYaanUiAction
    data class ExecutePrompt(val prompt: String) : AskYaanUiAction
    data object LaunchVoice : AskYaanUiAction
}
