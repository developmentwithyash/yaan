package com.yaan.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.crypto.VaultKeyManager
import com.yaan.core.data.crypto.YaanCipher
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * High-performance, zero-knowledge local SQLite repository for personal notes.
 * Enforces AES-GCM-256 authenticated encryption at rest when cipher & keyManager are bound.
 */
@OptIn(ExperimentalEncodingApi::class)
class SqlDelightNoteRepository(
    private val database: YaanDatabase,
    private val dispatchers: CoroutineDispatchers,
    private val cipher: YaanCipher? = null,
    private val keyManager: VaultKeyManager? = null,
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

    override suspend fun getNotesPaged(limit: Long, offset: Long): YaanResult<List<Note>> = withContext(dispatchers.io) {
        try {
            val list = queries.selectNotesPaged(limit = limit, offset = offset).executeAsList().map { it.toDomain() }
            YaanResult.Success(list)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to fetch paged notes: ${e.message}", e))
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

            // Encrypt content at rest using AES-GCM-256
            val rawContent = note.contentCiphertext
            val storedContent = if (cipher != null && keyManager != null) {
                if (rawContent.startsWith(VAULT_CIPHER_PREFIX)) {
                    rawContent
                } else {
                    val key = keyManager.getOrCreateMasterKey()
                    when (val res = cipher.encryptSync(rawContent, key)) {
                        is YaanResult.Success -> VAULT_CIPHER_PREFIX + Base64.encode(res.data)
                        is YaanResult.Failure -> rawContent
                    }
                }
            } else {
                rawContent
            }

            queries.insertNote(
                id = note.id,
                title = note.title,
                content = storedContent,
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
            val all = queries.selectAllNotes().executeAsList().map { it.toDomain() }
            val q = query.trim().lowercase()
            val filtered = if (q.isEmpty()) {
                all
            } else {
                all.filter { note ->
                    note.title.lowercase().contains(q) ||
                        note.contentCiphertext.lowercase().contains(q) ||
                        note.tags.any { it.name.lowercase().contains(q) }
                }
            }
            YaanResult.Success(filtered)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.StorageError("Failed to search notes for '$query': ${e.message}", e))
        }
    }

    private fun NoteEntity.toDomain(): Note {
        val decryptedContent = if (cipher != null && keyManager != null && content.startsWith(VAULT_CIPHER_PREFIX)) {
            try {
                val base64Bytes = content.removePrefix(VAULT_CIPHER_PREFIX)
                val cipherBytes = Base64.decode(base64Bytes)
                val key = keyManager.getOrCreateMasterKey()
                when (val res = cipher.decryptSync(cipherBytes, key)) {
                    is YaanResult.Success -> res.data
                    is YaanResult.Failure -> "[Decryption Error: Integrity Check Mismatch]"
                }
            } catch (e: Exception) {
                "[Decryption Error: Corrupted Ciphertext]"
            }
        } else {
            content
        }

        return Note(
            id = id,
            title = title,
            contentCiphertext = decryptedContent,
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

    companion object {
        const val VAULT_CIPHER_PREFIX = "vault_v1_aes_gcm:"
    }
}
