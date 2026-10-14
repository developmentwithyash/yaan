package com.yaan.core.evaluation

import kotlin.math.ln
import kotlin.math.max

/**
 * Deterministic lexical search engine simulating SQLite FTS5 with Okapi BM25 ranking.
 * Provides the baseline retrieval mechanism against which all advanced retrievers are compared.
 */
class LexicalBaselineSearch(
    private val vault: SyntheticVault,
    private val k1: Double = 1.2,
    private val b: Double = 0.75,
) {

    private data class IndexedDoc(
        val id: String,
        val text: String,
        val tokens: List<String>,
        val termFrequencies: Map<String, Int>,
        val docLength: Int,
        val timestampEpochSec: Long
    )

    private val documents = ArrayList<IndexedDoc>(vault.totalItems)
    private val docFreqs = HashMap<String, Int>()
    private val avgDocLength: Double

    init {
        // Index Notes
        for (note in vault.notes) {
            val fullText = "${note.title} ${note.contentCiphertext} ${note.tags.joinToString(" ") { it.name }}"
            val tokens = tokenize(fullText)
            val tf = HashMap<String, Int>()
            for (t in tokens) {
                tf[t] = (tf[t] ?: 0) + 1
            }
            for (t in tf.keys) {
                docFreqs[t] = (docFreqs[t] ?: 0) + 1
            }
            documents.add(
                IndexedDoc(
                    id = note.id,
                    text = fullText,
                    tokens = tokens,
                    termFrequencies = tf,
                    docLength = tokens.size,
                    timestampEpochSec = note.updatedAt.epochSeconds
                )
            )
        }

        // Index Tasks
        for (task in vault.tasks) {
            val fullText = "${task.title} ${task.notes}"
            val tokens = tokenize(fullText)
            val tf = HashMap<String, Int>()
            for (t in tokens) {
                tf[t] = (tf[t] ?: 0) + 1
            }
            for (t in tf.keys) {
                docFreqs[t] = (docFreqs[t] ?: 0) + 1
            }
            documents.add(
                IndexedDoc(
                    id = task.id,
                    text = fullText,
                    tokens = tokens,
                    termFrequencies = tf,
                    docLength = tokens.size,
                    timestampEpochSec = task.createdAt.epochSeconds
                )
            )
        }

        val totalLength = documents.sumOf { it.docLength.toLong() }
        avgDocLength = if (documents.isNotEmpty()) totalLength.toDouble() / documents.size else 1.0
    }

    /**
     * Executes BM25 search for [query] and returns top [limit] hits with scores and elapsed latency.
     * Optionally applies [recencyWeight] (0.0 = pure BM25, 0.1 = slight boost for recent documents).
     */
    fun search(query: String, limit: Int = 5, recencyWeight: Double = 0.0): RetrievalResult {
        val startTime = currentTimeNanos()
        val queryTokens = tokenize(query)

        if (queryTokens.isEmpty()) {
            return RetrievalResult(queryId = "", retrievedIds = emptyList(), scores = emptyList(), latencyMs = 0)
        }

        val n = documents.size.toDouble()
        val scoredList = ArrayList<Pair<String, Double>>()

        // Reference max epoch for recency calculation
        val maxEpoch = documents.maxOfOrNull { it.timestampEpochSec } ?: 1L

        for (doc in documents) {
            var score = 0.0
            for (token in queryTokens) {
                val tf = doc.termFrequencies[token] ?: 0
                if (tf > 0) {
                    val df = docFreqs[token] ?: 1
                    // Standard Lucene/BM25 IDF: ln(1 + (N - df + 0.5) / (df + 0.5))
                    val idf = ln(1.0 + (n - df + 0.5) / (df + 0.5))
                    val numerator = tf * (k1 + 1.0)
                    val denominator = tf + k1 * (1.0 - b + b * (doc.docLength / avgDocLength))
                    score += idf * (numerator / max(0.001, denominator))
                }
            }

            if (score > 0.0) {
                if (recencyWeight > 0.0) {
                    // Recency score normalized between 0.0 and 1.0 based on epoch
                    val recencyFraction = doc.timestampEpochSec.toDouble() / maxEpoch.toDouble()
                    score *= (1.0 + recencyWeight * recencyFraction)
                }
                scoredList.add(Pair(doc.id, score))
            }
        }

        scoredList.sortByDescending { it.second }
        val topResults = scoredList.take(limit)

        val elapsedMs = (currentTimeNanos() - startTime) / 1_000_000

        return RetrievalResult(
            queryId = "",
            retrievedIds = topResults.map { it.first },
            scores = topResults.map { it.second },
            latencyMs = elapsedMs
        )
    }

    companion object {
        private val STOPWORDS = setOf(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from",
            "has", "he", "in", "is", "it", "its", "of", "on", "that", "the",
            "to", "was", "were", "will", "with", "my", "me", "i", "can", "you",
            "what", "when", "where", "which", "who", "whom", "how", "why", "do"
        )

        fun tokenize(text: String): List<String> {
            return text.lowercase()
                .split(Regex("[^a-zA-Z0-9]+"))
                .filter { it.isNotBlank() && it.length > 1 && it !in STOPWORDS }
        }

        private fun currentTimeNanos(): Long {
            // Safe cross-platform time approximation for benchmarks
            return kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeNanoseconds
        }
    }
}
