package com.yaan.core.data.export

import kotlinx.serialization.Serializable

/**
 * Data format specification for offline .yaanvault encrypted backup archives.
 * Encapsulates password-derived key metadata, authenticated AES-GCM ciphertext,
 * and pre-decryption SHA-256 integrity checksums.
 */
@Serializable
data class EncryptedVaultBundle(
    val formatVersion: String = BUNDLE_FORMAT_VERSION,
    val algorithm: String = "PBKDF2-HMAC-SHA256:AES-256-GCM",
    val saltHex: String,
    val iterations: Int = DEFAULT_PBKDF2_ITERATIONS,
    val ivHex: String,
    val ciphertextHex: String,
    val checksumSha256: String,
    val exportedAtEpochMs: Long,
    val notesCount: Int,
    val tasksCount: Int,
    val schemaVersion: String = "2.0"
) {
    companion object {
        const val BUNDLE_FORMAT_VERSION = "2.0"
        const val DEFAULT_PBKDF2_ITERATIONS = 10_000
        const val FILE_EXTENSION = ".yaanvault"
    }
}
