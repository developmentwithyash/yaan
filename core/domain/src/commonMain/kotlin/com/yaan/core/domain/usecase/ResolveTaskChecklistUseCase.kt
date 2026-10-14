package com.yaan.core.domain.usecase

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class ResolveTaskChecklistUseCase(
    private val taskRepository: TaskRepository,
) {
    fun observeTasks(): Flow<List<Task>> = taskRepository.observeTasks()

    suspend fun toggleCompleted(taskId: String): YaanResult<Task> =
        taskRepository.toggleTaskCompleted(taskId)
}
