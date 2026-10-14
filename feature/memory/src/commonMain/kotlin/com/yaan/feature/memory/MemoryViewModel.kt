package com.yaan.feature.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryViewModel(
    private val memoryRepository: MemoryRepository,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {

    private val _state = MutableStateFlow(MemoryState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        observeMemories()
    }

    private fun observeMemories() {
        viewModelScope.launch(dispatchers.main) {
            memoryRepository.observeMemories().collect { list ->
                _state.update { it.copy(memories = list, isLoading = false) }
            }
        }
    }

    fun onIntent(intent: MemoryIntent) {
        when (intent) {
            is MemoryIntent.SearchMemory -> {
                viewModelScope.launch(dispatchers.main) {
                    val result = memoryRepository.queryMemories(intent.query)
                    result.onSuccess { filtered ->
                        _state.update { it.copy(memories = filtered) }
                    }
                }
            }
            is MemoryIntent.DeleteMemory -> {
                viewModelScope.launch(dispatchers.main) {
                    memoryRepository.deleteMemory(intent.id)
                }
            }
        }
    }
}
