package com.yaan.backend.inference

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

enum class CircuitState {
    CLOSED,    // Healthy: requests go to primary provider
    OPEN,      // Tripped: requests route immediately to secondary fallback
    HALF_OPEN  // Testing recovery
}

/**
 * High-performance, thread-safe Circuit Breaker
 *
 * Prevents cascading latency violations across the AI inference mesh.
 * Trips if consecutive failures or timeouts exceed the threshold, bypassing slow providers
 * to maintain the sub-150ms interaction ceiling.
 */
class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val resetTimeoutMs: Long = 10_000L
) {
    private val state = AtomicReference(CircuitState.CLOSED)
    private val failureCount = AtomicInteger(0)
    private val lastStateChange = AtomicLong(System.currentTimeMillis())

    fun getState(): CircuitState {
        val current = state.get()
        if (current == CircuitState.OPEN) {
            val elapsed = System.currentTimeMillis() - lastStateChange.get()
            if (elapsed > resetTimeoutMs) {
                if (state.compareAndSet(CircuitState.OPEN, CircuitState.HALF_OPEN)) {
                    lastStateChange.set(System.currentTimeMillis())
                    return CircuitState.HALF_OPEN
                }
            }
        }
        return state.get()
    }

    fun recordSuccess() {
        failureCount.set(0)
        if (state.get() != CircuitState.CLOSED) {
            state.set(CircuitState.CLOSED)
            lastStateChange.set(System.currentTimeMillis())
        }
    }

    fun recordFailure() {
        val failures = failureCount.incrementAndGet()
        if (failures >= failureThreshold || state.get() == CircuitState.HALF_OPEN) {
            state.set(CircuitState.OPEN)
            lastStateChange.set(System.currentTimeMillis())
        }
    }

    fun reset() {
        failureCount.set(0)
        state.set(CircuitState.CLOSED)
        lastStateChange.set(System.currentTimeMillis())
    }
}
