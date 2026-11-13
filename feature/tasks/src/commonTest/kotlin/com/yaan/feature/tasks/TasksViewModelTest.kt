package com.yaan.feature.tasks

import com.yaan.core.common.datetime.nowInstant
import com.yaan.core.common.dispatcher.TestCoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskStatus
import com.yaan.core.domain.repository.TaskRepository
import com.yaan.core.domain.usecase.ResolveTaskChecklistUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeTaskRepository : TaskRepository {
    val tasks = mutableListOf<Task>()
    val deletedIds = mutableListOf<String>()

    override fun observeTasks(): Flow<List<Task>> = flowOf(tasks)
    override suspend fun getTasks(): YaanResult<List<Task>> = YaanResult.Success(tasks)
    override suspend fun getTaskById(id: String): YaanResult<Task?> = YaanResult.Success(tasks.find { it.id == id })
    override suspend fun upsertTask(task: Task): YaanResult<Unit> {
        tasks.add(task)
        return YaanResult.Success(Unit)
    }
    override suspend fun toggleTaskCompleted(id: String): YaanResult<Task> {
        val index = tasks.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = if (tasks[index].status == TaskStatus.COMPLETED) {
                tasks[index].copy(status = TaskStatus.PENDING, completedAt = null)
            } else {
                tasks[index].copy(status = TaskStatus.COMPLETED, completedAt = nowInstant())
            }
            tasks[index] = updated
            return YaanResult.Success(updated)
        }
        val defaultTask = Task(id = id, title = "Task", status = TaskStatus.COMPLETED)
        return YaanResult.Success(defaultTask)
    }
    override suspend fun deleteTask(id: String): YaanResult<Unit> {
        deletedIds.add(id)
        tasks.removeAll { it.id == id }
        return YaanResult.Success(Unit)
    }
}

class TasksViewModelTest {

    @Test
    fun testDeleteTaskDelegatesToUseCase() = runTest {
        val repository = FakeTaskRepository()
        val task = Task(id = "task-delete-123", title = "Clean up temp logs")
        repository.upsertTask(task)

        val useCase = ResolveTaskChecklistUseCase(repository)
        val viewModel = TasksViewModel(useCase, TestCoroutineDispatchers())

        viewModel.onIntent(TasksIntent.DeleteTask("task-delete-123"))

        assertTrue(repository.deletedIds.contains("task-delete-123"))
        assertEquals(0, repository.tasks.size)
    }

    @Test
    fun testToggleCompletedDelegatesToUseCase() = runTest {
        val repository = FakeTaskRepository()
        val task = Task(id = "task-toggle-456", title = "Write ADR")
        repository.upsertTask(task)

        val useCase = ResolveTaskChecklistUseCase(repository)
        val viewModel = TasksViewModel(useCase, TestCoroutineDispatchers())

        viewModel.onIntent(TasksIntent.ToggleTaskCompleted("task-toggle-456"))

        val updated = repository.tasks.find { it.id == "task-toggle-456" }
        assertEquals(TaskStatus.COMPLETED, updated?.status)
    }
}
