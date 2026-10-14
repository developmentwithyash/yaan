package com.yaan.core.domain.usecase

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow

class QueryPersonalVaultUseCase(
    private val noteRepository: NoteRepository,
) {
    fun observeNotes(): Flow<List<Note>> = noteRepository.observeNotes()

    suspend fun search(query: String): YaanResult<List<Note>> =
        noteRepository.searchNotes(query)
}
