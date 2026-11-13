package com.yaan.feature.notes

import com.yaan.core.common.dispatcher.TestCoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeNoteRepository : NoteRepository {
    val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    val deletedIds = mutableListOf<String>()

    override fun observeNotes(): Flow<List<Note>> = notesFlow.asStateFlow()

    override suspend fun getNotes(): YaanResult<List<Note>> = YaanResult.Success(notesFlow.value)

    override suspend fun getNoteById(id: String): YaanResult<Note?> =
        YaanResult.Success(notesFlow.value.find { it.id == id })

    override suspend fun upsertNote(note: Note): YaanResult<Unit> {
        val current = notesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            current[index] = note
        } else {
            current.add(note)
        }
        notesFlow.value = current
        return YaanResult.Success(Unit)
    }

    override suspend fun deleteNote(id: String): YaanResult<Unit> {
        deletedIds.add(id)
        notesFlow.value = notesFlow.value.filterNot { it.id == id }
        return YaanResult.Success(Unit)
    }

    override suspend fun searchNotes(query: String): YaanResult<List<Note>> {
        val filtered = notesFlow.value.filter {
            it.title.contains(query, ignoreCase = true) || it.contentCiphertext.contains(query, ignoreCase = true)
        }
        return YaanResult.Success(filtered)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSearchDebounceAndClearingQueryRestoresFullList() = runTest(testDispatcher) {
        val repository = FakeNoteRepository()
        val note1 = Note(id = "n1", title = "Weekly Executive Sync", contentCiphertext = "encrypted_1")
        val note2 = Note(id = "n2", title = "Personal Budget Planning", contentCiphertext = "encrypted_2")
        repository.upsertNote(note1)
        repository.upsertNote(note2)

        val viewModel = NotesViewModel(repository, TestCoroutineDispatchers(testDispatcher))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.state.value.notes.size)

        // Dispatch search
        viewModel.onIntent(NotesIntent.Search("Executive"))
        // Before 300ms debounce
        advanceTimeBy(200)
        assertEquals(2, viewModel.state.value.notes.size)

        // After debounce
        advanceTimeBy(150)
        assertEquals(1, viewModel.state.value.notes.size)
        assertEquals("n1", viewModel.state.value.notes.first().id)

        // Clear search query
        viewModel.onIntent(NotesIntent.Search(""))
        assertEquals(2, viewModel.state.value.notes.size)
    }

    @Test
    fun testDeleteNoteDelegatesToRepository() = runTest(testDispatcher) {
        val repository = FakeNoteRepository()
        val note = Note(id = "n-delete", title = "Obsolete Draft", contentCiphertext = "cipher")
        repository.upsertNote(note)

        val viewModel = NotesViewModel(repository, TestCoroutineDispatchers(testDispatcher))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onIntent(NotesIntent.DeleteNote("n-delete"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.deletedIds.contains("n-delete"))
    }
}
