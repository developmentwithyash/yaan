package com.yaan.feature.capture

import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.repository.NoteRepository
import com.yaan.core.domain.repository.TaskRepository
import com.yaan.core.domain.usecase.CaptureIntentUseCase
import com.yaan.core.common.result.YaanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private class TestTaskRepo : TaskRepository {
    val tasks = mutableListOf<Task>()
    override fun observeTasks(): Flow<List<Task>> = flowOf(tasks)
    override suspend fun getTasks(): YaanResult<List<Task>> = YaanResult.Success(tasks)
    override suspend fun getTaskById(id: String): YaanResult<Task?> = YaanResult.Success(null)
    override suspend fun upsertTask(task: Task): YaanResult<Unit> {
        tasks.add(task)
        return YaanResult.Success(Unit)
    }
    override suspend fun toggleTaskCompleted(id: String): YaanResult<Task> = error("Not needed")
    override suspend fun deleteTask(id: String): YaanResult<Unit> = YaanResult.Success(Unit)
}

private class TestNoteRepo : NoteRepository {
    val notes = mutableListOf<Note>()
    override fun observeNotes(): Flow<List<Note>> = flowOf(notes)
    override suspend fun getNotes(): YaanResult<List<Note>> = YaanResult.Success(notes)
    override suspend fun getNoteById(id: String): YaanResult<Note?> = YaanResult.Success(null)
    override suspend fun upsertNote(note: Note): YaanResult<Unit> {
        notes.add(note)
        return YaanResult.Success(Unit)
    }
    override suspend fun deleteNote(id: String): YaanResult<Unit> = YaanResult.Success(Unit)
    override suspend fun searchNotes(query: String): YaanResult<List<Note>> = YaanResult.Success(emptyList())
}

class CaptureViewModelTest {

    @Test
    fun testUpdateInputAndSubmit() = runTest {
        val taskRepo = TestTaskRepo()
        val noteRepo = TestNoteRepo()
        val useCase = CaptureIntentUseCase(taskRepo, noteRepo)
        val viewModel = CaptureViewModel(useCase, com.yaan.core.common.dispatcher.TestCoroutineDispatchers())

        viewModel.onIntent(CaptureIntent.UpdateInput("TODO: Prepare Keynote"))
        assertEquals("TODO: Prepare Keynote", viewModel.state.value.inputText)

        viewModel.onIntent(CaptureIntent.Submit)
        assertEquals("", viewModel.state.value.inputText)
        assertNotNull(viewModel.state.value.feedbackMessage)
        assertEquals(1, taskRepo.tasks.size)
    }
}
