package com.yaan.backend.ratelimit

import java.util.concurrent.ConcurrentHashMap

/**
 * Token Bucket Rate Limiter for Ktor backend endpoints.
 * Controls burst capacity and steady-state request rates per identifier (userId or IP).
 */
class TokenBucketRateLimiter(
    val capacity: Int = 30,
    val refillTokensPerSecond: Double = 5.0
) {
    private data class Bucket(
        val tokens: Double,
        val lastRefillTimestamp: Long
    )

    private val buckets = ConcurrentHashMap<String, Bucket>()

    /**
     * Attempts to acquire [tokensToConsume] for [key].
     * Returns true if allowed, or false if rate limited.
     */
    fun tryAcquire(
        key: String,
        tokensToConsume: Int = 1,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        var acquired = false
        buckets.compute(key) { _, existing ->
            val current = existing ?: Bucket(capacity.toDouble(), nowMs)
            val elapsedSec = (nowMs - current.lastRefillTimestamp).coerceAtLeast(0) / 1000.0
            val refilled = (current.tokens + elapsedSec * refillTokensPerSecond).coerceAtMost(capacity.toDouble())

            if (refilled >= tokensToConsume) {
                acquired = true
                Bucket(refilled - tokensToConsume, nowMs)
            } else {
                acquired = false
                Bucket(refilled, nowMs)
            }
        }
        return acquired
    }

    fun reset() {
        buckets.clear()
    }
}
