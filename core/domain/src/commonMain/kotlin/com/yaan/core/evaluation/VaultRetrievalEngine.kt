package com.yaan.core.evaluation

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Retrieval modes available for Ask YAAN ablation testing.
 */
enum class RetrievalMode {
    FTS5_ONLY,
    FTS5_WITH_RECENCY,
    EMBEDDINGS_ONLY,
    HYBRID_RANK_FUSION
}

/**
 * High-performance vault retrieval engine supporting Lexical BM25 (FTS5),
 * Dense Semantic Embeddings (Cosine Sim), Recency Scoring, and Reciprocal Rank Fusion (RRF).
 */
class VaultRetrievalEngine(
    private val vault: SyntheticVault,
    private val k1: Double = 1.2,
    private val b: Double = 0.75,
    private val rrfK: Double = 60.0
) {

    private data class IndexedDoc(
        val id: String,
        val text: String,
        val tokens: List<String>,
        val termFrequencies: Map<String, Int>,
        val docLength: Int,
        val timestampEpochSec: Long,
        val pseudoEmbedding: FloatArray
    )

    private val documents = ArrayList<IndexedDoc>(vault.totalItems)
    private val docFreqs = HashMap<String, Int>()
    private val avgDocLength: Double
    private val maxEpochSec: Long

    init {
        var highestEpoch = 1L

        // Index Notes
        for (note in vault.notes) {
            val fullText = "${note.title} ${note.contentCiphertext} ${note.tags.joinToString(" ") { it.name }}"
            val tokens = LexicalBaselineSearch.tokenize(fullText)
            val tf = HashMap<String, Int>()
            for (t in tokens) {
                tf[t] = (tf[t] ?: 0) + 1
            }
            for (t in tf.keys) {
                docFreqs[t] = (docFreqs[t] ?: 0) + 1
            }
            val epoch = note.updatedAt.epochSeconds
            if (epoch > highestEpoch) highestEpoch = epoch

            documents.add(
                IndexedDoc(
                    id = note.id,
                    text = fullText,
                    tokens = tokens,
                    termFrequencies = tf,
                    docLength = tokens.size,
                    timestampEpochSec = epoch,
                    pseudoEmbedding = computeSemanticVector(fullText)
                )
            )
        }

        // Index Tasks
        for (task in vault.tasks) {
            val fullText = "${task.title} ${task.notes}"
            val tokens = LexicalBaselineSearch.tokenize(fullText)
            val tf = HashMap<String, Int>()
            for (t in tokens) {
                tf[t] = (tf[t] ?: 0) + 1
            }
            for (t in tf.keys) {
                docFreqs[t] = (docFreqs[t] ?: 0) + 1
            }
            val epoch = task.createdAt.epochSeconds
            if (epoch > highestEpoch) highestEpoch = epoch

            documents.add(
                IndexedDoc(
                    id = task.id,
                    text = fullText,
                    tokens = tokens,
                    termFrequencies = tf,
                    docLength = tokens.size,
                    timestampEpochSec = epoch,
                    pseudoEmbedding = computeSemanticVector(fullText)
                )
            )
        }

        maxEpochSec = highestEpoch
        val totalLength = documents.sumOf { it.docLength.toLong() }
        avgDocLength = if (documents.isNotEmpty()) totalLength.toDouble() / documents.size else 1.0
    }

    /**
     * Executes retrieval using the specified [mode] and returns top [limit] results.
     */
    fun search(query: String, mode: RetrievalMode, limit: Int = 5): RetrievalResult {
        val startTime = currentTimeNanos()
        val queryTokens = LexicalBaselineSearch.tokenize(query)

        val results = when (mode) {
            RetrievalMode.FTS5_ONLY -> scoreBm25(queryTokens, recencyBoost = 0.0)
            RetrievalMode.FTS5_WITH_RECENCY -> scoreBm25(queryTokens, recencyBoost = 0.15)
            RetrievalMode.EMBEDDINGS_ONLY -> scoreEmbeddings(query)
            RetrievalMode.HYBRID_RANK_FUSION -> scoreHybridRrf(queryTokens, query)
        }

        val topHits = results.take(limit)
        val elapsedMs = (currentTimeNanos() - startTime) / 1_000_000

        return RetrievalResult(
            queryId = "",
            retrievedIds = topHits.map { it.first },
            scores = topHits.map { it.second },
            latencyMs = elapsedMs
        )
    }

    private fun scoreBm25(queryTokens: List<String>, recencyBoost: Double): List<Pair<String, Double>> {
        if (queryTokens.isEmpty()) return emptyList()

        val n = documents.size.toDouble()
        val scored = ArrayList<Pair<String, Double>>()

        for (doc in documents) {
            var score = 0.0
            for (token in queryTokens) {
                val tf = doc.termFrequencies[token] ?: 0
                if (tf > 0) {
                    val df = docFreqs[token] ?: 1
                    val idf = ln(1.0 + (n - df + 0.5) / (df + 0.5))
                    val numerator = tf * (k1 + 1.0)
                    val denominator = tf + k1 * (1.0 - b + b * (doc.docLength / avgDocLength))
                    score += idf * (numerator / max(0.001, denominator))
                }
            }

            if (score > 0.0) {
                if (recencyBoost > 0.0 && maxEpochSec > 0) {
                    val recencyFraction = doc.timestampEpochSec.toDouble() / maxEpochSec.toDouble()
                    score *= (1.0 + recencyBoost * recencyFraction)
                }
                scored.add(Pair(doc.id, score))
            }
        }

        scored.sortByDescending { it.second }
        return scored
    }

    private fun scoreEmbeddings(query: String): List<Pair<String, Double>> {
        val queryVec = computeSemanticVector(query)
        val scored = ArrayList<Pair<String, Double>>(documents.size)

        for (doc in documents) {
            val sim = cosineSimilarity(queryVec, doc.pseudoEmbedding)
            if (sim > 0.10) {
                scored.add(Pair(doc.id, sim))
            }
        }

        scored.sortByDescending { it.second }
        return scored
    }

    private fun scoreHybridRrf(queryTokens: List<String>, rawQuery: String): List<Pair<String, Double>> {
        val bm25Hits = scoreBm25(queryTokens, recencyBoost = 0.0)
        val embeddingHits = scoreEmbeddings(rawQuery)

        val rrfScores = HashMap<String, Double>()

        // BM25 RRF contribution: rank is 1-indexed
        for ((rank, hit) in bm25Hits.withIndex()) {
            val score = 1.0 / (rrfK + (rank + 1))
            rrfScores[hit.first] = (rrfScores[hit.first] ?: 0.0) + score
        }

        // Embedding RRF contribution
        for ((rank, hit) in embeddingHits.withIndex()) {
            val score = 1.0 / (rrfK + (rank + 1))
            rrfScores[hit.first] = (rrfScores[hit.first] ?: 0.0) + score
        }

        // Recency slight boost (0.05)
        val scored = rrfScores.map { (id, score) ->
            val doc = documents.find { it.id == id }
            val recencyFraction = if (doc != null && maxEpochSec > 0) doc.timestampEpochSec.toDouble() / maxEpochSec.toDouble() else 0.0
            Pair(id, score * (1.0 + 0.05 * recencyFraction))
        }.sortedByDescending { it.second }

        return scored
    }

    companion object {
        private const val VECTOR_DIM = 64

        /**
         * Computes a deterministic normalized semantic representation for ablation testing.
         * Simulates dense semantic embeddings (e.g. BGE-micro / MiniLM-L6).
         */
        fun computeSemanticVector(text: String): FloatArray {
            val tokens = LexicalBaselineSearch.tokenize(text)
            val vec = FloatArray(VECTOR_DIM)

            for (token in tokens) {
                val hash = token.hashCode()
                for (dim in 0 until VECTOR_DIM) {
                    val weight = ((hash shr (dim % 16)) and 0x7F) - 64
                    vec[dim] += weight.toFloat() / 100.0f
                }
            }

            // L2 Normalize
            var normSq = 0.0f
            for (v in vec) normSq += v * v
            val norm = sqrt(normSq)
            if (norm > 0.0001f) {
                for (i in vec.indices) vec[i] /= norm
            }
            return vec
        }

        fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Double {
            var dot = 0.0
            val len = minOf(v1.size, v2.size)
            for (i in 0 until len) {
                dot += v1[i] * v2[i]
            }
            return max(0.0, dot)
        }

        private fun currentTimeNanos(): Long {
            return kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeNanoseconds
        }
    }
}
