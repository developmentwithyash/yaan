package com.yaan.core.data.export

import com.yaan.core.common.result.YaanResult
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
 * High-performance JSON and encrypted archive serializer for exporting user notes and tasks into
 * portable, self-contained archives (.json and .yaanvault formats).
 */
object VaultExporter {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val bundleManager = VaultBundleManager()

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

    /**
     * Exports user notes and tasks into an encrypted offline archive (.yaanvault JSON format).
     * Secures plaintext with PBKDF2-HMAC-SHA256 key derivation and AES-GCM-256 authenticated encryption.
     */
    suspend fun exportToEncryptedArchive(
        notes: List<Note>,
        tasks: List<Task>,
        password: String,
        timestampMs: Long,
        iterations: Int = EncryptedVaultBundle.DEFAULT_PBKDF2_ITERATIONS
    ): YaanResult<String> {
        val bundleResult = bundleManager.createEncryptedBundle(
            notes = notes,
            tasks = tasks,
            password = password,
            timestampMs = timestampMs,
            iterations = iterations
        )
        return when (bundleResult) {
            is YaanResult.Success -> YaanResult.Success(bundleManager.serializeBundle(bundleResult.data))
            is YaanResult.Failure -> YaanResult.Failure(bundleResult.error)
        }
    }

    /**
     * Decrypts and parses a password-protected .yaanvault bundle string back into a VaultExportArchive.
     */
    suspend fun parseEncryptedArchive(
        encryptedJson: String,
        password: String
    ): YaanResult<VaultExportArchive> {
        val bundleResult = bundleManager.deserializeBundle(encryptedJson)
        val bundle = when (bundleResult) {
            is YaanResult.Success -> bundleResult.data
            is YaanResult.Failure -> return YaanResult.Failure(bundleResult.error)
        }
        return bundleManager.restoreBundle(bundle, password)
    }
}
