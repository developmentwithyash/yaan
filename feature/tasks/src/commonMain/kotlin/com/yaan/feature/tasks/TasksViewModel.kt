package com.yaan.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.domain.usecase.ResolveTaskChecklistUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TasksViewModel(
    private val resolveTaskChecklistUseCase: ResolveTaskChecklistUseCase,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(TasksState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        observeTasks()
    }

    private fun observeTasks() {
        viewModelScope.launch(dispatchers.main) {
            resolveTaskChecklistUseCase.observeTasks().collect { list ->
                _state.update { it.copy(tasks = list, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: TasksIntent) {
        when (intent) {
            is TasksIntent.ToggleTaskCompleted -> {
                viewModelScope.launch(dispatchers.main) {
                    resolveTaskChecklistUseCase.toggleCompleted(intent.id)
                }
            }
            is TasksIntent.DeleteTask -> {
                viewModelScope.launch(dispatchers.main) {
                    resolveTaskChecklistUseCase.deleteTask(intent.id)
                }
            }
        }
    }
}
