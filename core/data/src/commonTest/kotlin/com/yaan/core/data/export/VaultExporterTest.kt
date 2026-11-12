package com.yaan.core.data.export

import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import com.yaan.core.domain.model.TaskPriority
import com.yaan.core.domain.model.VaultTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultExporterTest {

    @Test
    fun testExportAndParseArchive() {
        val notes = listOf(
            Note(
                id = "note_01",
                title = "Founding Memo",
                contentCiphertext = "Building an autonomous personal assistant for executive workflows.",
                tags = listOf(VaultTag("Founders"), VaultTag("Vision"))
            ),
            Note(
                id = "note_02",
                title = "Architecture Invariants",
                contentCiphertext = "Local encryption via AES-GCM-256.",
                tags = listOf(VaultTag("Engineering"), VaultTag("Security"))
            )
        )

        val tasks = listOf(
            Task(
                id = "task_01",
                title = "Prepare pitch deck for seed investor",
                priority = TaskPriority.HIGH,
                status = com.yaan.core.domain.model.TaskStatus.PENDING
            ),
            Task(
                id = "task_02",
                title = "Schedule dentist appointment",
                priority = TaskPriority.LOW,
                status = com.yaan.core.domain.model.TaskStatus.COMPLETED
            )
        )

        val timestamp = 1776200000000L
        val jsonOutput = VaultExporter.exportToJson(notes, tasks, timestamp)

        assertTrue(jsonOutput.contains("\"exportVersion\": \"1.0\""))
        assertTrue(jsonOutput.contains("\"notesCount\": 2"))
        assertTrue(jsonOutput.contains("\"tasksCount\": 2"))
        assertTrue(jsonOutput.contains("Founding Memo"))
        assertTrue(jsonOutput.contains("Prepare pitch deck for seed investor"))

        // Round-trip verification
        val parsed = VaultExporter.parseArchive(jsonOutput)
        assertEquals("1.0", parsed.exportVersion)
        assertEquals(timestamp, parsed.exportedAtEpochMs)
        assertEquals(2, parsed.notesCount)
        assertEquals(2, parsed.tasksCount)
        assertEquals(2, parsed.notes.size)
        assertEquals(2, parsed.tasks.size)
        assertEquals("Founding Memo", parsed.notes[0].title)
        assertEquals("Prepare pitch deck for seed investor", parsed.tasks[0].title)
    }

    @Test
    fun testExportAndRestoreEncryptedVaultArchiveRoundTrip() = kotlinx.coroutines.test.runTest {
        val notes = listOf(
            Note(
                id = "enc_note_01",
                title = "Secret Patent Application",
                contentCiphertext = "Self-contained neural cognitive mesh architecture.",
                tags = listOf(VaultTag("Patent"))
            )
        )
        val tasks = listOf(
            Task(
                id = "enc_task_01",
                title = "File provisional patent with USPTO",
                priority = TaskPriority.HIGH,
                status = com.yaan.core.domain.model.TaskStatus.PENDING
            )
        )
        val timestamp = 1776500000000L
        val password = "SuperSecretMasterKey!2026"

        val exportResult = VaultExporter.exportToEncryptedArchive(
            notes = notes,
            tasks = tasks,
            password = password,
            timestampMs = timestamp,
            iterations = 1000 // Fast iterations for unit test execution
        )

        assertTrue(exportResult is com.yaan.core.common.result.YaanResult.Success)
        val encryptedJson = (exportResult as com.yaan.core.common.result.YaanResult.Success).data

        // Forensic assertions: verify encrypted attributes and absence of plaintext
        assertTrue(encryptedJson.contains("PBKDF2-HMAC-SHA256:AES-256-GCM"))
        assertTrue(encryptedJson.contains("saltHex"))
        assertTrue(encryptedJson.contains("ivHex"))
        assertTrue(encryptedJson.contains("ciphertextHex"))
        assertTrue(encryptedJson.contains("checksumSha256"))
        kotlin.test.assertFalse(encryptedJson.contains("Secret Patent Application"), "Plaintext title leaked in export!")
        kotlin.test.assertFalse(encryptedJson.contains("neural cognitive mesh"), "Plaintext content leaked in export!")

        // Decryption round-trip with correct password
        val parseResult = VaultExporter.parseEncryptedArchive(encryptedJson, password)
        assertTrue(parseResult is com.yaan.core.common.result.YaanResult.Success)
        val archive = (parseResult as com.yaan.core.common.result.YaanResult.Success).data
        assertEquals("2.0", archive.exportVersion)
        assertEquals(1, archive.notesCount)
        assertEquals(1, archive.tasksCount)
        assertEquals("Secret Patent Application", archive.notes[0].title)
        assertEquals("File provisional patent with USPTO", archive.tasks[0].title)
    }

    @Test
    fun testEncryptedArchiveFailsWithWrongPassword() = kotlinx.coroutines.test.runTest {
        val notes = listOf(Note(id = "n1", title = "T1", contentCiphertext = "C1"))
        val tasks = emptyList<Task>()
        val exportResult = VaultExporter.exportToEncryptedArchive(
            notes = notes,
            tasks = tasks,
            password = "CorrectPassword123!",
            timestampMs = 1000L,
            iterations = 1000
        )
        assertTrue(exportResult is com.yaan.core.common.result.YaanResult.Success)
        val encryptedJson = (exportResult as com.yaan.core.common.result.YaanResult.Success).data

        val parseResult = VaultExporter.parseEncryptedArchive(encryptedJson, "WrongPassword999!")
        assertTrue(parseResult is com.yaan.core.common.result.YaanResult.Failure)
    }

    @Test
    fun testEncryptedArchiveRejectsBlankPassword() = kotlinx.coroutines.test.runTest {
        val exportResult = VaultExporter.exportToEncryptedArchive(
            notes = emptyList(),
            tasks = emptyList(),
            password = "   ",
            timestampMs = 1000L
        )
        assertTrue(exportResult is com.yaan.core.common.result.YaanResult.Failure)
    }
}
