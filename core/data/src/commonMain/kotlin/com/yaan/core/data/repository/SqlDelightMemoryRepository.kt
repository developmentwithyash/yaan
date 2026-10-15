package com.yaan.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.db.MemoryEntity
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.model.MemoryItem
import com.yaan.core.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightMemoryRepository(
    private val database: YaanDatabase,
    private val dispatchers: CoroutineDispatchers,
) : MemoryRepository {
    private val queries = database.yaanDatabaseQueries

    override fun observeMemories(): Flow<List<MemoryItem>> {
        return queries.selectAllMemories()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getMemories(): YaanResult<List<MemoryItem>> = withContext(dispatchers.io) {
        try {
            val list = queries.selectAllMemories().executeAsList().map { it.toDomain() }
            YaanResult.Success(list)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch memories: ${e.message}", e))
        }
    }

    override suspend fun queryMemories(query: String): YaanResult<List<MemoryItem>> = withContext(dispatchers.io) {
        try {
            val list = queries.searchMemories(query).executeAsList().map { it.toDomain() }
            YaanResult.Success(list)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to query memories: ${e.message}", e))
        }
    }

    override suspend fun saveMemory(item: MemoryItem): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            queries.insertMemory(
                id = item.id,
                content = "${item.subject} | ${item.predicate}",
                category = "Epistemic",
                confidence = item.confidenceScore,
                source_id = item.citationSourceId,
                created_at = item.createdAt.toEpochMilliseconds()
            )
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to save memory: ${e.message}", e))
        }
    }

    override suspend fun deleteMemory(id: String): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            queries.deleteMemory(id)
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to delete memory: ${e.message}", e))
        }
    }

    private fun MemoryEntity.toDomain(): MemoryItem {
        val parts = content.split(" | ", limit = 2)
        val subj = parts.getOrNull(0) ?: content
        val pred = parts.getOrNull(1) ?: ""
        return MemoryItem(
            id = id,
            subject = subj,
            predicate = pred,
            confidenceScore = confidence,
            citationSourceId = source_id,
            createdAt = Instant.fromEpochMilliseconds(created_at)
        )
    }
}
