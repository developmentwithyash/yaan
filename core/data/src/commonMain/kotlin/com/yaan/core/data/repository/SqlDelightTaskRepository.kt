package com.yaan.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.db.TaskEntity
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.model.AutoReschedulePolicy
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.model.TaskStatus
import com.yaan.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightTaskRepository(
    private val database: YaanDatabase,
    private val dispatchers: CoroutineDispatchers,
) : TaskRepository {
    private val queries = database.yaanDatabaseQueries

    override fun observeTasks(): Flow<List<Task>> {
        return queries.selectAllTasks()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTasks(): YaanResult<List<Task>> = withContext(dispatchers.io) {
        try {
            val list = queries.selectAllTasks().executeAsList().map { it.toDomain() }
            YaanResult.Success(list)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch tasks: ${e.message}", e))
        }
    }

    override suspend fun getTaskById(id: String): YaanResult<Task?> = withContext(dispatchers.io) {
        try {
            val entity = queries.selectTaskById(id).executeAsOneOrNull()
            YaanResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch task $id: ${e.message}", e))
        }
    }

    override suspend fun upsertTask(task: Task): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            val now = com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()
            queries.insertTask(
                id = task.id,
                title = task.title,
                due_time = task.dueAt?.toString(),
                is_completed = task.status == TaskStatus.COMPLETED,
                tag = task.priority.name,
                is_high_priority = task.priority == TaskPriority.HIGH || task.priority == TaskPriority.URGENT,
                created_at = task.createdAt.toEpochMilliseconds(),
                updated_at = now
            )
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to upsert task ${task.id}: ${e.message}", e))
        }
    }

    override suspend fun toggleTaskCompleted(id: String): YaanResult<Task> = withContext(dispatchers.io) {
        try {
            val existing = queries.selectTaskById(id).executeAsOneOrNull()
                ?: return@withContext YaanResult.Failure(YaanError.StorageError("Task not found: $id"))
            val now = com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()
            queries.toggleTaskCompleted(updated_at = now, id = id)
            val updated = queries.selectTaskById(id).executeAsOne()
            YaanResult.Success(updated.toDomain())
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to toggle task $id: ${e.message}", e))
        }
    }

    override suspend fun deleteTask(id: String): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            queries.deleteTask(id)
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to delete task $id: ${e.message}", e))
        }
    }

    private fun TaskEntity.toDomain(): Task {
        return Task(
            id = id,
            title = title,
            notes = "",
            priority = when (tag.uppercase()) {
                "URGENT" -> TaskPriority.URGENT
                "HIGH" -> TaskPriority.HIGH
                "LOW" -> TaskPriority.LOW
                else -> if (is_high_priority) TaskPriority.HIGH else TaskPriority.MEDIUM
            },
            status = if (is_completed) TaskStatus.COMPLETED else TaskStatus.PENDING,
            dueAt = due_time?.let {
                try { Instant.parse(it) } catch (_: Exception) { null }
            },
            completedAt = if (is_completed) Instant.fromEpochMilliseconds(updated_at) else null,
            autoReschedulePolicy = AutoReschedulePolicy.NEXT_MORNING,
            createdAt = Instant.fromEpochMilliseconds(created_at)
        )
    }
}
