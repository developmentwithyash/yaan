package com.yaan.backend.vault

import kotlinx.serialization.Serializable

@Serializable
data class VaultBackupEnvelope(
    val backupId: String,
    val userId: String,
    val ciphertextBlob: String,
    val sha256Checksum: String,
    val sizeBytes: Long,
    val createdAtMs: Long
)

@Serializable
data class BackupUploadResponse(
    val success: Boolean,
    val backupId: String,
    val storedSizeBytes: Long,
    val serverChecksum: String
)

@Serializable
data class ShreddingCertificate(
    val userId: String,
    val erasureTimestampMs: Long,
    val deletedBackupsCount: Int,
    val sha256Proof: String,
    val complianceStandard: String = "GDPR-Article-17-Crypto-Shredding"
)
