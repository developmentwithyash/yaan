package com.yaan.backend.sync

import kotlinx.serialization.Serializable

@Serializable
enum class EntityType {
    TASK, NOTE, REMINDER, MEMORY, SETTING
}

/**
 * Blind Encrypted Synchronization Envelope.
 *
 * Implements Zero-Knowledge Invariant #2:
 * The backend stores and relays this envelope without ever decrypting or having access
 * to private user keys.
 */
@Serializable
data class EncryptedSyncEnvelope(
    val id: String,
    val userId: String,
    val deviceId: String,
    val entityType: EntityType,
    val ciphertext: String,
    val nonce: String,
    val lamportClock: Long,
    val epoch: Long = 1L,
    val timestampMs: Long
)

@Serializable
data class SyncPushRequest(
    val userId: String,
    val deviceId: String,
    val envelopes: List<EncryptedSyncEnvelope>
)

@Serializable
data class SyncPushResponse(
    val success: Boolean,
    val acceptedCount: Int,
    val highestLamport: Long,
    val serverTimestampMs: Long
)

@Serializable
data class SyncPullResponse(
    val userId: String,
    val envelopes: List<EncryptedSyncEnvelope>,
    val highestLamport: Long,
    val hasMore: Boolean
)
