package com.yaan.feature.memory.retrieval

import com.yaan.core.domain.model.MemoryItem
import com.yaan.feature.memory.embedding.EmbeddingModelEngine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticMemoryRetrieverTest {

    private class MockEmbeddingEngine : EmbeddingModelEngine {
        override val dimension: Int = 4

        override suspend fun generateEmbedding(text: String): FloatArray {
            return when (text.lowercase()) {
                "quantum computing" -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f)
                "quantum physics" -> floatArrayOf(0.9f, 0.43588989f, 0.0f, 0.0f) // Cosine similarity ~ 0.90
                "computer science" -> floatArrayOf(0.75f, 0.0f, 0.6614378f, 0.0f) // Cosine similarity ~ 0.75
                "baking chocolate cake" -> floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f) // Cosine similarity ~ 0.00
                else -> floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
            }
        }
    }

    private val engine = MockEmbeddingEngine()
    private val retriever = SemanticMemoryRetriever(engine)

    private val candidates = listOf(
        MemoryItem(id = "1", subject = "Quantum", predicate = "Physics", confidenceScore = 0.95),
        MemoryItem(id = "2", subject = "Computer", predicate = "Science", confidenceScore = 0.90),
        MemoryItem(id = "3", subject = "Baking", predicate = "chocolate cake", confidenceScore = 0.85)
    )

    @Test
    fun testDefaultThresholdExcludesLowSimilarityMemories() = runBlocking {
        // Query: "quantum computing"
        // Candidates scores: Quantum Physics (~0.90), Computer Science (~0.75), Cake (~0.00)
        // With default similarityThreshold = 0.70f: Cake (0.00) should be excluded, Physics & CS included
        val results = retriever.retrieveRelevantMemories(
            query = "quantum computing",
            candidates = candidates
        )

        assertEquals(2, results.size)
        assertEquals("1", results[0].memory.id)
        assertTrue(results[0].score >= 0.70f)
        assertEquals("2", results[1].memory.id)
        assertTrue(results[1].score >= 0.70f)
        assertTrue(results[0].score >= results[1].score) // Descending order
    }

    @Test
    fun testStrictThresholdExcludesMediumScores() = runBlocking {
        // With similarityThreshold = 0.85f: only Quantum Physics (~0.90) should pass
        val results = retriever.retrieveRelevantMemories(
            query = "quantum computing",
            candidates = candidates,
            similarityThreshold = 0.85f
        )

        assertEquals(1, results.size)
        assertEquals("1", results[0].memory.id)
        assertTrue(results[0].score >= 0.85f)
    }

    @Test
    fun testLimitRestrictsResultCount() = runBlocking {
        val results = retriever.retrieveRelevantMemories(
            query = "quantum computing",
            candidates = candidates,
            similarityThreshold = 0.50f,
            limit = 1
        )

        assertEquals(1, results.size)
        assertEquals("1", results[0].memory.id)
    }
}
