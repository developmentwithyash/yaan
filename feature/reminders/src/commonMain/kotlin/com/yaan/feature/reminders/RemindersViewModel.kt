package com.yaan.feature.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.datetime.nowInstant
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.domain.model.AlarmAckState
import com.yaan.core.domain.repository.ReminderRepository
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RemindersViewModel(
    private val reminderRepository: ReminderRepository,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(RemindersState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        observeReminders()
    }

    private fun observeReminders() {
        viewModelScope.launch(dispatchers.main) {
            reminderRepository.observeReminders().collect { list ->
                _state.update { it.copy(reminders = list, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: RemindersIntent) {
        when (intent) {
            is RemindersIntent.AcknowledgeReminder -> {
                viewModelScope.launch(dispatchers.main) {
                    reminderRepository.updateAckState(intent.id, AlarmAckState.ACKNOWLEDGED)
                }
            }
            is RemindersIntent.SnoozeReminder -> {
                viewModelScope.launch(dispatchers.main) {
                    val existingResult = reminderRepository.getReminderById(intent.id)
                    existingResult.onSuccess { existing ->
                        if (existing != null) {
                            val updated = existing.copy(
                                triggerTime = nowInstant() + 10.minutes,
                                ackState = AlarmAckState.SNOOZED,
                            )
                            reminderRepository.scheduleReminder(updated)
                        } else {
                            reminderRepository.updateAckState(intent.id, AlarmAckState.SNOOZED)
                        }
                    }
                }
            }
            is RemindersIntent.DeleteReminder -> {
                viewModelScope.launch(dispatchers.main) {
                    reminderRepository.deleteReminder(intent.id)
                }
            }
        }
    }
}
