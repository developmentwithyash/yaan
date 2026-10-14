package com.yaan.core.data.repository

import com.yaan.core.common.datetime.nowInstant
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskStatus
import com.yaan.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryTaskRepository : TaskRepository {
    private val mutex = Mutex()
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    override fun observeTasks(): Flow<List<Task>> = tasksFlow.asStateFlow()

    override suspend fun getTasks(): YaanResult<List<Task>> = mutex.withLock {
        YaanResult.Success(tasksFlow.value)
    }

    override suspend fun getTaskById(id: String): YaanResult<Task?> = mutex.withLock {
        YaanResult.Success(tasksFlow.value.find { it.id == id })
    }

    override suspend fun upsertTask(task: Task): YaanResult<Unit> = mutex.withLock {
        val current = tasksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            current[index] = task
        } else {
            current.add(0, task)
        }
        tasksFlow.value = current
        YaanResult.Success(Unit)
    }

    override suspend fun toggleTaskCompleted(id: String): YaanResult<Task> = mutex.withLock {
        val current = tasksFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = current[index]
            val updated = if (existing.status == TaskStatus.COMPLETED) {
                existing.copy(status = TaskStatus.PENDING, completedAt = null)
            } else {
                existing.copy(status = TaskStatus.COMPLETED, completedAt = nowInstant())
            }
            current[index] = updated
            tasksFlow.value = current
            YaanResult.Success(updated)
        } else {
            YaanResult.Failure(YaanError.StorageError("Task not found: $id"))
        }
    }

    override suspend fun deleteTask(id: String): YaanResult<Unit> = mutex.withLock {
        tasksFlow.value = tasksFlow.value.filterNot { it.id == id }
        YaanResult.Success(Unit)
    }
}
