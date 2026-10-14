package com.yaan.core.data.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryNoteRepository : NoteRepository {
    private val mutex = Mutex()
    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())

    override fun observeNotes(): Flow<List<Note>> = notesFlow.asStateFlow()

    override suspend fun getNotes(): YaanResult<List<Note>> = mutex.withLock {
        YaanResult.Success(notesFlow.value)
    }

    override suspend fun getNoteById(id: String): YaanResult<Note?> = mutex.withLock {
        YaanResult.Success(notesFlow.value.find { it.id == id })
    }

    override suspend fun upsertNote(note: Note): YaanResult<Unit> = mutex.withLock {
        val current = notesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            current[index] = note
        } else {
            current.add(0, note)
        }
        notesFlow.value = current
        YaanResult.Success(Unit)
    }

    override suspend fun deleteNote(id: String): YaanResult<Unit> = mutex.withLock {
        notesFlow.value = notesFlow.value.filterNot { it.id == id }
        YaanResult.Success(Unit)
    }

    override suspend fun searchNotes(query: String): YaanResult<List<Note>> = mutex.withLock {
        val filtered = notesFlow.value.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.contentCiphertext.contains(query, ignoreCase = true)
        }
        YaanResult.Success(filtered)
    }
}
