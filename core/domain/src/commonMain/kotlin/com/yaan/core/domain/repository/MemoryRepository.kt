package com.yaan.core.domain.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.MemoryItem
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeMemories(): Flow<List<MemoryItem>>
    suspend fun getMemories(): YaanResult<List<MemoryItem>>
    suspend fun queryMemories(query: String): YaanResult<List<MemoryItem>>
    suspend fun saveMemory(item: MemoryItem): YaanResult<Unit>
    suspend fun deleteMemory(id: String): YaanResult<Unit>
}
