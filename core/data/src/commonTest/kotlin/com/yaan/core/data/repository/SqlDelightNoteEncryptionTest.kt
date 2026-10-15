package com.yaan.core.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.crypto.AesGcmYaanCipher
import com.yaan.core.data.crypto.InMemoryVaultKeyManager
import com.yaan.core.data.db.YaanDatabase
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.VaultTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SqlDelightNoteEncryptionTest {

    private val testDispatchers = object : CoroutineDispatchers {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val unconfined = Dispatchers.Unconfined
    }

    private fun createTestDb(): YaanDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        YaanDatabase.Schema.create(driver)
        return YaanDatabase(driver)
    }

    @Test
    fun `notes are genuinely encrypted at rest in SQLite table with AES-GCM-256`() = runTest {
        val db = createTestDb()
        val keyManager = InMemoryVaultKeyManager()
        val cipher = AesGcmYaanCipher()
        val repository = SqlDelightNoteRepository(
            database = db,
            dispatchers = testDispatchers,
            cipher = cipher,
            keyManager = keyManager
        )

        val sensitiveContent = "Top-secret executive partnership terms with Stripe and Apple: $10,000,000 deal."
        val note = Note(
            id = "note_sec_01",
            title = "Executive Confidential Strategy",
            contentCiphertext = sensitiveContent,
            tags = listOf(VaultTag("Executive")),
            isPinned = true,
            createdAt = Instant.fromEpochMilliseconds(1000L),
            updatedAt = Instant.fromEpochMilliseconds(2000L)
        )

        // 1. Save note through repository
        val saveResult = repository.upsertNote(note)
        assertTrue(saveResult is YaanResult.Success)

        // 2. Direct forensic inspection of SQLite row on disk
        val rawEntity = db.yaanDatabaseQueries.selectNoteById("note_sec_01").executeAsOne()
        
        // Assert: Raw row contains vault prefix and is NOT plaintext
        assertTrue(rawEntity.content.startsWith(SqlDelightNoteRepository.VAULT_CIPHER_PREFIX))
        assertFalse(
            rawEntity.content.contains("Top-secret"),
            "Plaintext secret must NEVER be visible in raw SQLite database storage!"
        )
        assertFalse(
            rawEntity.content.contains("Stripe"),
            "Entity names must NOT be leaked in plaintext SQLite storage!"
        )

        // 3. Transparent retrieval via repository produces original plaintext
        val readResult = repository.getNoteById("note_sec_01")
        assertTrue(readResult is YaanResult.Success)
        val readNote = (readResult as YaanResult.Success).data
        assertEquals(sensitiveContent, readNote?.contentCiphertext)

        // 4. In-memory search over decrypted contents works accurately
        val searchResult = repository.searchNotes("Stripe")
        assertTrue(searchResult is YaanResult.Success)
        val matchingNotes = (searchResult as YaanResult.Success).data
        assertEquals(1, matchingNotes.size)
        assertEquals("note_sec_01", matchingNotes.first().id)
    }

    @Test
    fun `tampered ciphertext in SQLite fails gracefully with integrity mismatch flag`() = runTest {
        val db = createTestDb()
        val keyManager = InMemoryVaultKeyManager()
        val cipher = AesGcmYaanCipher()
        val repository = SqlDelightNoteRepository(
            database = db,
            dispatchers = testDispatchers,
            cipher = cipher,
            keyManager = keyManager
        )

        val note = Note(
            id = "note_sec_02",
            title = "Integrity Test",
            contentCiphertext = "Original safe content",
            tags = listOf(VaultTag("Safety")),
            createdAt = Instant.fromEpochMilliseconds(1000L),
            updatedAt = Instant.fromEpochMilliseconds(2000L)
        )
        repository.upsertNote(note)

        // Tamper directly with the SQLite database row (simulate malicious file modification)
        db.yaanDatabaseQueries.insertNote(
            id = "note_sec_02",
            title = "Integrity Test",
            content = SqlDelightNoteRepository.VAULT_CIPHER_PREFIX + "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=", // invalid MAC/ciphertext
            tag = "Safety",
            is_pinned = false,
            is_encrypted = true,
            audio_duration = null,
            created_at = 1000L,
            updated_at = 2000L
        )

        // Reading tampered note must detect integrity mismatch without crashing
        val readResult = repository.getNoteById("note_sec_02")
        assertTrue(readResult is YaanResult.Success)
        val readNote = (readResult as YaanResult.Success).data
        assertTrue(
            readNote?.contentCiphertext?.contains("Decryption Error") == true,
            "Tampered ciphertext must produce explicit integrity error notice"
        )
    }
}
