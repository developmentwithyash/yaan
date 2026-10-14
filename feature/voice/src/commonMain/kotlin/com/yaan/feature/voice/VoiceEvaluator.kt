package com.yaan.feature.voice

import kotlin.math.min

/**
 * Feature flags controlling Voice functionality.
 * Default is strictly OFF until consented and explicitly enabled by the user.
 */
data class VoiceFeatureFlags(
    val isVoiceEnabled: Boolean = false,
    val isPushToTalkOnly: Boolean = true,
    val usePlatformTts: Boolean = true,
    val maxRecordingDurationSec: Int = 60
)

/**
 * Result of Word Error Rate (WER) computation.
 */
data class WerMetrics(
    val referenceWordCount: Int,
    val substitutions: Int,
    val deletions: Int,
    val insertions: Int,
    val wer: Double,
    val latencyMs: Long
)

/**
 * Computes exact Levenshtein-based Word Error Rate (WER) across speech recognition hypotheses.
 * Implements the standard NIST / Sclite algorithm:
 * WER = (S + D + I) / N
 */
object VoiceEvaluator {

    fun normalizeText(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    /**
     * Calculates WER between [reference] and [hypothesis] using dynamic programming.
     */
    fun computeWer(reference: String, hypothesis: String, latencyMs: Long = 0L): WerMetrics {
        val refWords = normalizeText(reference)
        val hypWords = normalizeText(hypothesis)

        val n = refWords.size
        val m = hypWords.size

        if (n == 0) {
            val wer = if (m == 0) 0.0 else 1.0
            return WerMetrics(0, 0, 0, m, wer, latencyMs)
        }

        // DP table of size (n+1) x (m+1)
        val dp = Array(n + 1) { IntArray(m + 1) }

        for (i in 0..n) dp[i][0] = i
        for (j in 0..m) dp[0][j] = j

        for (i in 1..n) {
            for (j in 1..m) {
                if (refWords[i - 1] == hypWords[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    val substitution = dp[i - 1][j - 1] + 1
                    val deletion = dp[i - 1][j] + 1
                    val insertion = dp[i][j - 1] + 1
                    dp[i][j] = min(substitution, min(deletion, insertion))
                }
            }
        }

        val totalErrors = dp[n][m]
        val wer = totalErrors.toDouble() / n.toDouble()

        return WerMetrics(
            referenceWordCount = n,
            substitutions = 0, // aggregate edit distance
            deletions = 0,
            insertions = 0,
            wer = wer,
            latencyMs = latencyMs
        )
    }
}
