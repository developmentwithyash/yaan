package com.yaan.feature.reminders

import com.yaan.core.domain.model.Reminder

data class RemindersState(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface RemindersIntent {
    data class AcknowledgeReminder(val id: String) : RemindersIntent
    data class SnoozeReminder(val id: String) : RemindersIntent
    data class DeleteReminder(val id: String) : RemindersIntent
}
