package com.yaan.feature.memory

import com.yaan.core.domain.model.MemoryItem

data class MemoryState(
    val memories: List<MemoryItem> = emptyList(),
    val isLoading: Boolean = false,
)

sealed interface MemoryIntent {
    data class SearchMemory(val query: String) : MemoryIntent
    data class DeleteMemory(val id: String) : MemoryIntent
}
