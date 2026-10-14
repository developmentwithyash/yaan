package com.yaan.core.domain.ai

import kotlin.math.max
import kotlin.math.pow

/**
 * Epistemic Memory Decay Calculator.
 *
 * Implements Layer 4 of ADR-0006:
 * Mathematical half-life decay function for subjective memory confidence:
 * c(t) = c0 * 2^(-Δt / halfLifeDays)
 *
 * Automatically triggers TTL pruning when confidence drops below 0.20 or expires.
 */
object EpistemicDecayCalculator {

    private const val MS_PER_DAY = 86_400_000.0
    const val PRUNE_CONFIDENCE_THRESHOLD = 0.20

    /**
     * Calculates the decayed confidence score for a given fact at [currentTimeMs].
     */
    fun calculateDecayedConfidence(fact: EpistemicFact, currentTimeMs: Long): Double {
        // Explicit expiration check
        if (fact.expiresAtMs != null && currentTimeMs > fact.expiresAtMs) {
            return 0.0
        }

        val elapsedMs = max(0L, currentTimeMs - fact.createdAtMs)
        val elapsedDays = elapsedMs / MS_PER_DAY
        val decayFactor = 2.0.pow(-elapsedDays / fact.halfLifeDays)

        return (fact.initialConfidence * decayFactor).coerceIn(0.0, 1.0)
    }

    /**
     * Determines whether a fact has decayed beyond recovery and should be pruned.
     */
    fun shouldPrune(fact: EpistemicFact, currentTimeMs: Long): Boolean {
        val decayed = calculateDecayedConfidence(fact, currentTimeMs)
        return decayed < PRUNE_CONFIDENCE_THRESHOLD
    }
}
