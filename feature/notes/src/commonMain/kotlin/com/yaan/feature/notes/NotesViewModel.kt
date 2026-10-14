package com.yaan.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotesViewModel(
    private val noteRepository: NoteRepository,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(NotesState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch(dispatchers.main) {
            noteRepository.observeNotes().collect { list ->
                _state.update { it.copy(notes = list, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: NotesIntent) {
        when (intent) {
            is NotesIntent.Search -> {
                _state.update { it.copy(searchQuery = intent.query) }
                viewModelScope.launch(dispatchers.main) {
                    val result = noteRepository.searchNotes(intent.query)
                    result.onSuccess { filtered ->
                        _state.update { it.copy(notes = filtered) }
                    }
                }
            }
            is NotesIntent.DeleteNote -> {
                viewModelScope.launch(dispatchers.main) {
                    noteRepository.deleteNote(intent.id)
                }
            }
        }
    }
}
