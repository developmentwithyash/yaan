package com.yaan.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.db.NoteEntity
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.model.AudioMemoMetadata
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.VaultTag
import com.yaan.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class SqlDelightNoteRepository(
    private val database: YaanDatabase,
    private val dispatchers: CoroutineDispatchers,
) : NoteRepository {
    private val queries = database.yaanDatabaseQueries

    override fun observeNotes(): Flow<List<Note>> {
        return queries.selectAllNotes()
            .asFlow()
            .mapToList(dispatchers.io)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getNotes(): YaanResult<List<Note>> = withContext(dispatchers.io) {
        try {
            val list = queries.selectAllNotes().executeAsList().map { it.toDomain() }
            YaanResult.Success(list)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch notes: ${e.message}", e))
        }
    }

    override suspend fun getNoteById(id: String): YaanResult<Note?> = withContext(dispatchers.io) {
        try {
            val entity = queries.selectNoteById(id).executeAsOneOrNull()
            YaanResult.Success(entity?.toDomain())
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch note $id: ${e.message}", e))
        }
    }

    override suspend fun upsertNote(note: Note): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            val now = com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()
            val primaryTag = note.tags.firstOrNull()?.name ?: "Personal"
            queries.insertNote(
                id = note.id,
                title = note.title,
                content = note.contentCiphertext,
                tag = primaryTag,
                is_pinned = note.isPinned,
                is_encrypted = true,
                audio_duration = note.audioMemo?.durationMs?.let { "${it / 1000}s" },
                created_at = note.createdAt.toEpochMilliseconds(),
                updated_at = now
            )
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to upsert note ${note.id}: ${e.message}", e))
        }
    }

    override suspend fun deleteNote(id: String): YaanResult<Unit> = withContext(dispatchers.io) {
        try {
            queries.deleteNote(id)
            YaanResult.Success(Unit)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to delete note $id: ${e.message}", e))
        }
    }

    override suspend fun searchNotes(query: String): YaanResult<List<Note>> = withContext(dispatchers.io) {
        try {
            val list = queries.searchNotes(query).executeAsList().map { it.toDomain() }
            YaanResult.Success(list)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to search notes for '$query': ${e.message}", e))
        }
    }

    private fun NoteEntity.toDomain(): Note {
        return Note(
            id = id,
            title = title,
            contentCiphertext = content,
            tags = listOf(VaultTag(name = tag, isEncrypted = is_encrypted)),
            audioMemo = audio_duration?.let {
                AudioMemoMetadata(
                    audioUri = "vault://audio/$id",
                    durationMs = 120_000L
                )
            },
            isPinned = is_pinned,
            createdAt = Instant.fromEpochMilliseconds(created_at),
            updatedAt = Instant.fromEpochMilliseconds(updated_at)
        )
    }
}
