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
}
