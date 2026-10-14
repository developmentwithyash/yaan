package com.yaan.backend

import com.yaan.backend.sync.CrdtRelayService
import com.yaan.backend.sync.EncryptedSyncEnvelope
import com.yaan.backend.sync.EntityType
import com.yaan.backend.sync.SyncPushRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncRelayTest {

    @Test
    fun `test push and pull of encrypted deltas preserves lamport order`() {
        val service = CrdtRelayService()
        val userId = "user-789"
        val deviceId = "pixel-6a"

        val env1 = EncryptedSyncEnvelope(
            id = "env-1",
            userId = userId,
            deviceId = deviceId,
            entityType = EntityType.TASK,
            ciphertext = "cipher_payload_1",
            nonce = "nonce_1",
            lamportClock = 10L,
            epoch = 1L,
            timestampMs = 1000L
        )

        val env2 = EncryptedSyncEnvelope(
            id = "env-2",
            userId = userId,
            deviceId = deviceId,
            entityType = EntityType.NOTE,
            ciphertext = "cipher_payload_2",
            nonce = "nonce_2",
            lamportClock = 15L,
            epoch = 1L,
            timestampMs = 2000L
        )

        val pushResponse = service.push(
            SyncPushRequest(
                userId = userId,
                deviceId = deviceId,
                envelopes = listOf(env1, env2)
            )
        )

        assertTrue(pushResponse.success)
        assertEquals(2, pushResponse.acceptedCount)
        assertEquals(15L, pushResponse.highestLamport)

        // Pull with sinceLamport = 10 -> should only return env2
        val pullResponse = service.pull(userId = userId, sinceLamport = 10L)
        assertEquals(1, pullResponse.envelopes.size)
        assertEquals("env-2", pullResponse.envelopes.first().id)
        assertEquals(15L, pullResponse.highestLamport)
    }

    @Test
    fun `test envelope deduplication`() {
        val service = CrdtRelayService()
        val userId = "user-dedup"
        val env = EncryptedSyncEnvelope(
            id = "duplicate-id",
            userId = userId,
            deviceId = "dev-1",
            entityType = EntityType.MEMORY,
            ciphertext = "encrypted_bits",
            nonce = "n",
            lamportClock = 5L,
            timestampMs = 500L
        )

        val res1 = service.push(SyncPushRequest(userId, "dev-1", listOf(env)))
        assertEquals(1, res1.acceptedCount)

        // Push identical envelope again
        val res2 = service.push(SyncPushRequest(userId, "dev-1", listOf(env)))
        assertEquals(0, res2.acceptedCount) // Deduplicated
    }
}
