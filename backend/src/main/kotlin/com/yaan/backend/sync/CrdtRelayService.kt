package com.yaan.backend.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Blind CRDT State Relay Service.
 *
 * Implements Zero-Knowledge state convergence across devices without decryption.
 * Uses Lamport vector clocks for causal ordering and SharedFlow for real-time fanout.
 */
class CrdtRelayService {

    // In-memory multi-tenant envelope repository: userId -> list of envelopes
    private val store = ConcurrentHashMap<String, MutableList<EncryptedSyncEnvelope>>()
    private val highestLamportPerUser = ConcurrentHashMap<String, AtomicLong>()

    // Global bus for real-time delta fanout (SSE / WebSockets)
    private val deltaFlow = MutableSharedFlow<EncryptedSyncEnvelope>(extraBufferCapacity = 512)

    fun push(request: SyncPushRequest): SyncPushResponse {
        val userList = store.computeIfAbsent(request.userId) { mutableListOf() }
        val userLamport = highestLamportPerUser.computeIfAbsent(request.userId) { AtomicLong(0) }

        var accepted = 0
        synchronized(userList) {
            for (env in request.envelopes) {
                // Ensure Lamport clock is strictly monotonically increasing
                val currentMax = userLamport.get()
                if (env.lamportClock > currentMax) {
                    userLamport.set(env.lamportClock)
                }

                // Deduplicate by envelope ID
                if (userList.none { it.id == env.id }) {
                    userList.add(env)
                    accepted++
                    deltaFlow.tryEmit(env)
                }
            }
        }

        return SyncPushResponse(
            success = true,
            acceptedCount = accepted,
            highestLamport = userLamport.get(),
            serverTimestampMs = System.currentTimeMillis()
        )
    }

    fun pull(userId: String, sinceLamport: Long, limit: Int = 100): SyncPullResponse {
        val userList = store[userId] ?: emptyList()
        val userLamport = highestLamportPerUser[userId]?.get() ?: 0L

        val filtered = synchronized(userList) {
            userList
                .filter { it.lamportClock > sinceLamport }
                .sortedBy { it.lamportClock }
                .take(limit)
        }

        return SyncPullResponse(
            userId = userId,
            envelopes = filtered,
            highestLamport = userLamport,
            hasMore = (userList.size - filtered.size) > 0 && filtered.size == limit
        )
    }

    fun streamDeltas(userId: String, excludeDeviceId: String? = null): Flow<EncryptedSyncEnvelope> {
        return deltaFlow.asSharedFlow().filter { env ->
            env.userId == userId && (excludeDeviceId == null || env.deviceId != excludeDeviceId)
        }
    }

    fun clear(userId: String) {
        store.remove(userId)
        highestLamportPerUser.remove(userId)
    }
}
