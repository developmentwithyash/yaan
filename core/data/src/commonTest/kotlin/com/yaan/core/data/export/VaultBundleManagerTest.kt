package com.yaan.core.data.export

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.model.TaskStatus
import com.yaan.core.domain.model.VaultTag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultBundleManagerTest {

    private val sampleNotes = listOf(
        Note(
            id = "note_01",
            title = "Personal Mission & Principles",
            contentCiphertext = "Zero-knowledge encryption for personal intelligence and workflows.",
            tags = listOf(VaultTag("Vision"), VaultTag("Security"))
        ),
        Note(
            id = "note_02",
            title = "Quarterly Strategic Milestones",
            contentCiphertext = "Deliver offline encrypted backup and cross-platform native sync.",
            tags = listOf(VaultTag("Roadmap"))
        )
    )

    private val sampleTasks = listOf(
        Task(
            id = "task_01",
            title = "Audit cryptographic zeroization in VaultKeyManager",
            priority = TaskPriority.HIGH,
            status = TaskStatus.PENDING
        ),
        Task(
            id = "task_02",
            title = "Finalize multiplatform packaging for Linux and macOS",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.COMPLETED
        )
    )

    private val manager = VaultBundleManager()

    @Test
    fun testCreateAndRestoreEncryptedBundleRoundTrip() = runTest {
        val password = "SuperSecretMasterVaultKey!2026"
        val timestamp = 1776500000000L

        // 1. Create encrypted bundle
        val exportResult = manager.createEncryptedBundle(
            notes = sampleNotes,
            tasks = sampleTasks,
            password = password,
            timestampMs = timestamp
        )

        assertTrue(exportResult is YaanResult.Success<*>)
        val bundle = (exportResult as YaanResult.Success<EncryptedVaultBundle>).data

        // 2. Assert bundle metadata contract
        assertEquals(EncryptedVaultBundle.BUNDLE_FORMAT_VERSION, bundle.formatVersion)
        assertEquals("PBKDF2-HMAC-SHA256:AES-256-GCM", bundle.algorithm)
        assertEquals(2, bundle.notesCount)
        assertEquals(2, bundle.tasksCount)
        assertEquals(timestamp, bundle.exportedAtEpochMs)
        assertTrue(bundle.saltHex.isNotEmpty())
        assertTrue(bundle.ivHex.isNotEmpty())
        assertTrue(bundle.ciphertextHex.isNotEmpty())
        assertTrue(bundle.checksumSha256.isNotEmpty())

        // 3. Restore bundle with the exact same password
        val restoreResult = manager.restoreBundle(
            bundle = bundle,
            password = password
        )

        assertTrue(restoreResult is YaanResult.Success<*>)
        val restoredArchive = (restoreResult as YaanResult.Success<VaultExportArchive>).data

        assertEquals("2.0", restoredArchive.exportVersion)
        assertEquals(timestamp, restoredArchive.exportedAtEpochMs)
        assertEquals(2, restoredArchive.notesCount)
        assertEquals(2, restoredArchive.tasksCount)
        assertEquals(sampleNotes.size, restoredArchive.notes.size)
        assertEquals(sampleTasks.size, restoredArchive.tasks.size)
        assertEquals("Personal Mission & Principles", restoredArchive.notes[0].title)
        assertEquals("Audit cryptographic zeroization in VaultKeyManager", restoredArchive.tasks[0].title)
    }

    @Test
    fun testRestoreFailsWithIncorrectPassword() = runTest {
        val password = "CorrectMasterPassword2026"
        val wrongPassword = "WrongPasswordAttackerGuess"

        val exportResult = manager.createEncryptedBundle(
            notes = sampleNotes,
            tasks = sampleTasks,
            password = password,
            timestampMs = 1776500000000L
        )
        assertTrue(exportResult is YaanResult.Success<*>)
        val bundle = (exportResult as YaanResult.Success<EncryptedVaultBundle>).data

        val restoreResult = manager.restoreBundle(
            bundle = bundle,
            password = wrongPassword
        )

        assertTrue(restoreResult is YaanResult.Failure)
        assertTrue(
            restoreResult.error is YaanError.CryptoError,
            "Expected CryptoError on bad password, got: ${restoreResult.error}"
        )
    }

    @Test
    fun testRestoreFailsWithTamperedCiphertextChecksumMismatch() = runTest {
        val password = "StrongTamperProofPassword99!"

        val exportResult = manager.createEncryptedBundle(
            notes = sampleNotes,
            tasks = sampleTasks,
            password = password,
            timestampMs = 1776500000000L
        )
        assertTrue(exportResult is YaanResult.Success<*>)
        val originalBundle = (exportResult as YaanResult.Success<EncryptedVaultBundle>).data

        // Tamper with ciphertext by flipping the last character
        val tamperedCipherHex = if (originalBundle.ciphertextHex.endsWith("0")) {
            "${originalBundle.ciphertextHex.dropLast(1)}1"
        } else {
            "${originalBundle.ciphertextHex.dropLast(1)}0"
        }
        val tamperedBundle = originalBundle.copy(ciphertextHex = tamperedCipherHex)

        val restoreResult = manager.restoreBundle(
            bundle = tamperedBundle,
            password = password
        )

        assertTrue(restoreResult is YaanResult.Failure)
        assertTrue(
            restoreResult.error is YaanError.ValidationError,
            "Expected ValidationError due to checksum mismatch before decryption, got: ${restoreResult.error}"
        )
    }

    @Test
    fun testBundleSerializationAndDeserialization() {
        val bundle = EncryptedVaultBundle(
            formatVersion = EncryptedVaultBundle.BUNDLE_FORMAT_VERSION,
            algorithm = "PBKDF2-HMAC-SHA256:AES-256-GCM",
            saltHex = "abcdef0123456789",
            iterations = 10000,
            ivHex = "0102030405060708090a0b0c",
            ciphertextHex = "cafebabedeadbeef",
            checksumSha256 = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            exportedAtEpochMs = 1776500000000L,
            notesCount = 1,
            tasksCount = 1
        )

        val jsonString = manager.serializeBundle(bundle)
        assertTrue(jsonString.contains("\"formatVersion\""))
        assertTrue(jsonString.contains("\"saltHex\""))
        assertTrue(jsonString.contains("abcdef0123456789"))

        val deserialized = manager.deserializeBundle(jsonString)
        assertTrue(deserialized is YaanResult.Success<*>)
        assertEquals(bundle, (deserialized as YaanResult.Success<EncryptedVaultBundle>).data)
    }

    @Test
    fun testCreateBundleRejectsBlankPassword() = runTest {
        val result = manager.createEncryptedBundle(
            notes = sampleNotes,
            tasks = sampleTasks,
            password = "   ",
            timestampMs = 1776500000000L
        )

        assertTrue(result is YaanResult.Failure)
        assertTrue(result.error is YaanError.ValidationError)
    }
}
