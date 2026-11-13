package com.yaan.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var cachedAllNotes: List<Note> = emptyList()
    private var searchJob: Job? = null

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch(dispatchers.main) {
            noteRepository.observeNotes().collect { list ->
                cachedAllNotes = list
                if (_state.value.searchQuery.isBlank()) {
                    _state.update { it.copy(notes = list, isLoading = false) }
                }
            }
        }
    }

    fun onIntent(intent: NotesIntent) {
        when (intent) {
            is NotesIntent.Search -> {
                val query = intent.query
                _state.update { it.copy(searchQuery = query) }
                searchJob?.cancel()
                if (query.isBlank()) {
                    _state.update { it.copy(notes = cachedAllNotes) }
                } else {
                    searchJob = viewModelScope.launch(dispatchers.main) {
                        delay(300L)
                        val result = noteRepository.searchNotes(query)
                        result.onSuccess { filtered ->
                            _state.update { it.copy(notes = filtered) }
                        }
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
