package com.yaan.core.domain.usecase

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class FakeTaskRepository : TaskRepository {
    val tasks = mutableListOf<Task>()
    override fun observeTasks(): Flow<List<Task>> = flowOf(tasks)
    override suspend fun getTasks(): YaanResult<List<Task>> = YaanResult.Success(tasks)
    override suspend fun getTaskById(id: String): YaanResult<Task?> = YaanResult.Success(tasks.find { it.id == id })
    override suspend fun upsertTask(task: Task): YaanResult<Unit> {
        tasks.add(task)
        return YaanResult.Success(Unit)
    }
    override suspend fun toggleTaskCompleted(id: String): YaanResult<Task> {
        val task = tasks.find { it.id == id }!!
        return YaanResult.Success(task)
    }
    override suspend fun deleteTask(id: String): YaanResult<Unit> {
        tasks.removeAll { it.id == id }
        return YaanResult.Success(Unit)
    }
}

class FakeNoteRepository : NoteRepository {
    val notes = mutableListOf<Note>()
    override fun observeNotes(): Flow<List<Note>> = flowOf(notes)
    override suspend fun getNotes(): YaanResult<List<Note>> = YaanResult.Success(notes)
    override suspend fun getNoteById(id: String): YaanResult<Note?> = YaanResult.Success(notes.find { it.id == id })
    override suspend fun upsertNote(note: Note): YaanResult<Unit> {
        notes.add(note)
        return YaanResult.Success(Unit)
    }
    override suspend fun deleteNote(id: String): YaanResult<Unit> {
        notes.removeAll { it.id == id }
        return YaanResult.Success(Unit)
    }
    override suspend fun searchNotes(query: String): YaanResult<List<Note>> {
        return YaanResult.Success(notes.filter { it.title.contains(query, ignoreCase = true) })
    }
}

class CaptureIntentUseCaseTest {

    @Test
    fun testTaskCaptureRouting() = runTest {
        val taskRepo = FakeTaskRepository()
        val noteRepo = FakeNoteRepository()
        val useCase = CaptureIntentUseCase(taskRepo, noteRepo)

        val result = useCase("TODO: Finalize Q4 Pitch Deck")
        assertTrue(result.isSuccess)
        assertTrue(taskRepo.tasks.any { it.title == "Finalize Q4 Pitch Deck" })
    }

    @Test
    fun testNoteCaptureRouting() = runTest {
        val taskRepo = FakeTaskRepository()
        val noteRepo = FakeNoteRepository()
        val useCase = CaptureIntentUseCase(taskRepo, noteRepo)

        val result = useCase("Thoughts on executive kinetic minimalism design system")
        assertTrue(result.isSuccess)
        assertTrue(noteRepo.notes.isNotEmpty())
    }
}
