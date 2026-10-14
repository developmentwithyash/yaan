package com.yaan.core.data.crdt

import kotlinx.serialization.Serializable

enum class ClockComparison {
    BEFORE,
    AFTER,
    SAME,
    CONCURRENT
}

/**
 * Lamport Vector Clock for non-destructive multi-device state tracking.
 */
@Serializable
data class VectorClock(
    val clocks: Map<String, Long> = emptyMap()
) {
    fun increment(nodeId: String): VectorClock {
        val current = clocks[nodeId] ?: 0L
        return copy(clocks = clocks + (nodeId to current + 1L))
    }

    fun merge(other: VectorClock): VectorClock {
        val allKeys = clocks.keys + other.clocks.keys
        val merged = allKeys.associateWith { key ->
            maxOf(clocks[key] ?: 0L, other.clocks[key] ?: 0L)
        }
        return VectorClock(merged)
    }

    fun compare(other: VectorClock): ClockComparison {
        if (this == other) return ClockComparison.SAME

        var isGreater = false
        var isLesser = false

        val allKeys = clocks.keys + other.clocks.keys
        for (key in allKeys) {
            val v1 = clocks[key] ?: 0L
            val v2 = other.clocks[key] ?: 0L
            if (v1 > v2) isGreater = true
            if (v1 < v2) isLesser = true
        }

        return when {
            isGreater && !isLesser -> ClockComparison.AFTER
            !isGreater && isLesser -> ClockComparison.BEFORE
            !isGreater && !isLesser -> ClockComparison.SAME
            else -> ClockComparison.CONCURRENT
        }
    }
}
