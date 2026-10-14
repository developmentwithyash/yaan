package com.yaan.core.domain.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun getTasks(): YaanResult<List<Task>>
    suspend fun getTaskById(id: String): YaanResult<Task?>
    suspend fun upsertTask(task: Task): YaanResult<Unit>
    suspend fun toggleTaskCompleted(id: String): YaanResult<Task>
    suspend fun deleteTask(id: String): YaanResult<Unit>
}
