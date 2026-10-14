package com.yaan.core.data.crdt

import kotlinx.serialization.Serializable

/**
 * Multi-Value Register (MVR) CRDT: Holds multiple values in case of concurrent offline edits.
 */
@Serializable
data class MvrVersion<T>(
    val value: T,
    val clock: VectorClock,
)

@Serializable
data class MvrRegister<T>(
    val versions: List<MvrVersion<T>> = emptyList()
) {
    fun set(value: T, nodeId: String): MvrRegister<T> {
        val mergedClock = versions.map { it.clock }.fold(VectorClock()) { acc, c -> acc.merge(c) }
        val newClock = mergedClock.increment(nodeId)
        return MvrRegister(listOf(MvrVersion(value, newClock)))
    }

    fun merge(other: MvrRegister<T>): MvrRegister<T> {
        val combined = this.versions + other.versions
        val survivors = mutableListOf<MvrVersion<T>>()

        for (v1 in combined) {
            val isOverridden = combined.any { v2 ->
                v1 != v2 && v2.clock.compare(v1.clock) == ClockComparison.AFTER
            }
            if (!isOverridden && !survivors.contains(v1)) {
                survivors.add(v1)
            }
        }

        return MvrRegister(survivors)
    }

    val isConflict: Boolean get() = versions.size > 1
    val values: List<T> get() = versions.map { it.value }
}
