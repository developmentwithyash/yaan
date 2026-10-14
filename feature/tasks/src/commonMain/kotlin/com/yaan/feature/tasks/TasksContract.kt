package com.yaan.feature.tasks

import com.yaan.core.domain.model.Task

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface TasksIntent {
    data class ToggleTaskCompleted(val id: String) : TasksIntent
    data class DeleteTask(val id: String) : TasksIntent
}
