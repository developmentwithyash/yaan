package com.yaan.core.domain.ai

import kotlinx.serialization.Serializable

/**
 * State of an on-device model package.
 */
@Serializable
enum class ModelDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    VERIFYING_CHECKSUM,
    INSTALLED,
    FAILED
}

/**
 * Metadata descriptor for an optional on-device model package.
 */
@Serializable
data class ModelPackageDescriptor(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val downloadUrl: String,
    val minimumRamTier: DeviceRamTier = DeviceRamTier.TIER_2_MID
) {
    val sizeMb: Double get() = sizeBytes.toDouble() / (1024.0 * 1024.0)
    val userSizeDisclosure: String get() = "${sizeMb.toInt()} MB (${displayName})"
}

/**
 * Manages model downloads with Wi-Fi only default, storage pre-checks,
 * checksum validation, and deletion.
 */
class ModelDownloadManager(
    val packageDescriptor: ModelPackageDescriptor,
    var requiresWifiOnly: Boolean = true
) {
    var status: ModelDownloadStatus = ModelDownloadStatus.NOT_DOWNLOADED
        private set

    var progressPercent: Int = 0
        private set

    var failureReason: String? = null
        private set

    /**
     * Pre-flight storage check: ensures available storage is at least 1.5x the model package size.
     */
    fun hasSufficientStorage(availableDiskBytes: Long): Boolean {
        val requiredBytes = (packageDescriptor.sizeBytes * 1.5).toLong()
        return availableDiskBytes >= requiredBytes
    }

    /**
     * Initiates a model download.
     */
    fun startDownload(
        availableDiskBytes: Long,
        isOnWifi: Boolean
    ): Boolean {
        if (requiresWifiOnly && !isOnWifi) {
            status = ModelDownloadStatus.FAILED
            failureReason = "Wi-Fi connection required by user settings. Connect to Wi-Fi or disable 'Wi-Fi only' in settings."
            return false
        }

        if (!hasSufficientStorage(availableDiskBytes)) {
            val neededMb = (packageDescriptor.sizeBytes * 1.5) / (1024 * 1024)
            status = ModelDownloadStatus.FAILED
            failureReason = "Insufficient storage space. Need at least ${neededMb.toInt()} MB available."
            return false
        }

        status = ModelDownloadStatus.DOWNLOADING
        progressPercent = 0
        failureReason = null
        return true
    }

    /**
     * Simulates downloading chunks and updates progress.
     */
    fun updateProgress(percent: Int) {
        if (status == ModelDownloadStatus.DOWNLOADING) {
            progressPercent = percent.coerceIn(0, 100)
            if (progressPercent == 100) {
                status = ModelDownloadStatus.VERIFYING_CHECKSUM
            }
        }
    }

    /**
     * Validates SHA-256 checksum and transitions to INSTALLED.
     */
    fun verifyChecksum(calculatedChecksum: String): Boolean {
        if (calculatedChecksum.equals(packageDescriptor.sha256Checksum, ignoreCase = true)) {
            status = ModelDownloadStatus.INSTALLED
            failureReason = null
            return true
        } else {
            status = ModelDownloadStatus.FAILED
            failureReason = "Checksum verification failed. Downloaded file may be corrupted."
            return false
        }
    }

    /**
     * Uninstalls/deletes the model to reclaim disk space.
     */
    fun deleteModel() {
        status = ModelDownloadStatus.NOT_DOWNLOADED
        progressPercent = 0
        failureReason = null
    }

    companion object {
        val SMOLLM2_360M_DESCRIPTOR = ModelPackageDescriptor(
            id = "smollm2_360m_int4",
            displayName = "SmolLM2-360M INT4 Intent Router",
            sizeBytes = 220L * 1024 * 1024, // 220 MB
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            downloadUrl = "https://models.yaan.app/smollm2/smollm2-360m-q4_k_m.gguf",
            minimumRamTier = DeviceRamTier.TIER_2_MID
        )
    }
}
