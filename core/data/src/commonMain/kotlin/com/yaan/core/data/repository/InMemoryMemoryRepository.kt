package com.yaan.core.data.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.MemoryItem
import com.yaan.core.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryMemoryRepository : MemoryRepository {
    private val mutex = Mutex()
    private val memoryFlow = MutableStateFlow<List<MemoryItem>>(emptyList())

    override fun observeMemories(): Flow<List<MemoryItem>> = memoryFlow.asStateFlow()

    override suspend fun getMemories(): YaanResult<List<MemoryItem>> = mutex.withLock {
        YaanResult.Success(memoryFlow.value)
    }

    override suspend fun queryMemories(query: String): YaanResult<List<MemoryItem>> = mutex.withLock {
        val filtered = memoryFlow.value.filter {
            it.subject.contains(query, ignoreCase = true) ||
            it.predicate.contains(query, ignoreCase = true)
        }
        YaanResult.Success(filtered)
    }

    override suspend fun saveMemory(item: MemoryItem): YaanResult<Unit> = mutex.withLock {
        val current = memoryFlow.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        memoryFlow.value = current
        YaanResult.Success(Unit)
    }

    override suspend fun deleteMemory(id: String): YaanResult<Unit> = mutex.withLock {
        memoryFlow.value = memoryFlow.value.filterNot { it.id == id }
        YaanResult.Success(Unit)
    }
}
