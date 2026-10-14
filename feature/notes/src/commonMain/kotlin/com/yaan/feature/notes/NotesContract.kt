package com.yaan.feature.notes

import com.yaan.core.domain.model.Note

data class NotesState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
)

sealed interface NotesIntent {
    data class Search(val query: String) : NotesIntent
    data class DeleteNote(val id: String) : NotesIntent
}
