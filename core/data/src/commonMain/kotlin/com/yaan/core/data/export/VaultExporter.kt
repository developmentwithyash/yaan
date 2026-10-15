package com.yaan.core.data.export

import com.yaan.core.domain.model.Note
import com.yaan.core.domain.model.Task
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data model for a portable, user-controlled Vault Export Archive.
 * Satisfies Critical User Journey 8 (Account / Data Export) and Finding F-005.
 */
@Serializable
data class VaultExportArchive(
    val exportVersion: String = "1.0",
    val exportedAtEpochMs: Long,
    val notesCount: Int,
    val tasksCount: Int,
    val notes: List<Note>,
    val tasks: List<Task>
)

/**
 * High-performance JSON serializer for exporting user notes and tasks into
 * a portable, self-contained archive.
 */
object VaultExporter {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportToJson(notes: List<Note>, tasks: List<Task>, timestampMs: Long): String {
        val archive = VaultExportArchive(
            exportVersion = "1.0",
            exportedAtEpochMs = timestampMs,
            notesCount = notes.size,
            tasksCount = tasks.size,
            notes = notes,
            tasks = tasks
        )
        return json.encodeToString(archive)
    }

    fun parseArchive(jsonString: String): VaultExportArchive {
        return json.decodeFromString(jsonString)
    }
}
