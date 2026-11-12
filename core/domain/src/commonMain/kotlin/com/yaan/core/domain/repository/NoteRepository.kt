package com.yaan.core.domain.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNotes(): YaanResult<List<Note>>
    suspend fun getNotesPaged(limit: Long, offset: Long = 0): YaanResult<List<Note>> = getNotes()
    suspend fun getNoteById(id: String): YaanResult<Note?>
    suspend fun upsertNote(note: Note): YaanResult<Unit>
    suspend fun deleteNote(id: String): YaanResult<Unit>
    suspend fun searchNotes(query: String): YaanResult<List<Note>>
}
