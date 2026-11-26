package com.yaan.feature.memory.retrieval

import com.yaan.core.domain.model.MemoryItem
import com.yaan.feature.memory.embedding.EmbeddingModelEngine
import com.yaan.feature.memory.embedding.LocalSemanticEmbeddingEngine

data class ScoredMemory(
    val memory: MemoryItem,
    val score: Float
)

/**
 * Retrieves semantic memories relevant to a query using on-device vector similarity.
 * Applies a strict cosine similarity threshold filter to prevent low-relevance
 * context dilution in downstream LLM prompts.
 */
class SemanticMemoryRetriever(
    private val embeddingEngine: EmbeddingModelEngine
) {

    /**
     * Scores candidate memories against [query] and returns matches satisfying [similarityThreshold].
     *
     * @param query The natural language query string.
     * @param candidates The list of candidate memories to filter.
     * @param similarityThreshold Minimum cosine similarity threshold (default 0.70f). Matches below this are filtered out.
     * @param limit Maximum number of memories to return (default 10).
     * @return List of [ScoredMemory] matching or exceeding threshold, ordered descending by score.
     */
    suspend fun retrieveRelevantMemories(
        query: String,
        candidates: List<MemoryItem>,
        similarityThreshold: Float = 0.70f,
        limit: Int = 10
    ): List<ScoredMemory> {
        if (candidates.isEmpty() || query.isBlank()) {
            return emptyList()
        }

        val queryEmbedding = embeddingEngine.generateEmbedding(query)

        val scored = candidates.mapNotNull { candidate ->
            val candidateText = "${candidate.subject} ${candidate.predicate}".trim()
            if (candidateText.isBlank()) return@mapNotNull null

            val candidateEmbedding = embeddingEngine.generateEmbedding(candidateText)
            val score = LocalSemanticEmbeddingEngine.cosineSimilarity(queryEmbedding, candidateEmbedding)

            if (score >= similarityThreshold) {
                ScoredMemory(memory = candidate, score = score)
            } else {
                null
            }
        }

        return scored
            .sortedByDescending { it.score }
            .take(limit)
    }
}
