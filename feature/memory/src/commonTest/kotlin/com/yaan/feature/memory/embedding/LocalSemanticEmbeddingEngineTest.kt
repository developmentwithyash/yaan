package com.yaan.feature.memory.embedding

import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalSemanticEmbeddingEngineTest {

    private val engine = LocalSemanticEmbeddingEngine()

    @Test
    fun testEmbeddingDimensionIs384() {
        assertEquals(384, engine.dimension)
    }

    @Test
    fun testGeneratedEmbeddingIsNormalized() = runTest {
        val text = "Executive quarterly revenue growth and partnership roadmap"
        val embedding = engine.generateEmbedding(text)

        assertEquals(384, embedding.size)

        // Compute L2 norm: sqrt(sum(x_i^2))
        var sumSquares = 0.0
        for (f in embedding) {
            sumSquares += (f * f)
        }
        val norm = kotlin.math.sqrt(sumSquares)
        assertTrue(abs(norm - 1.0) < 1e-4, "L2 norm of embedding must be 1.0 (unit vector)")
    }

    @Test
    fun testCosineSimilarityIdenticalText() = runTest {
        val text = "Confidential client contracts for Stripe integration"
        val v1 = engine.generateEmbedding(text)
        val v2 = engine.generateEmbedding(text)

        val similarity = LocalSemanticEmbeddingEngine.cosineSimilarity(v1, v2)
        assertTrue(abs(similarity - 1.0f) < 1e-4, "Identical vectors must yield cosine similarity ~1.0")
    }

    @Test
    fun testCosineSimilarityDistinctTexts() = runTest {
        val v1 = engine.generateEmbedding("Deep learning vector embeddings and neural networks")
        val v2 = engine.generateEmbedding("Recipe for chocolate chip cookies and baking flour")

        val similarity = LocalSemanticEmbeddingEngine.cosineSimilarity(v1, v2)
        assertTrue(similarity < 0.85f, "Distinct topics should have significantly lower cosine similarity")
    }

    @Test
    fun testBlobSerializationRoundTrip() = runTest {
        val text = "Offline encrypted memory indexing"
        val original = engine.generateEmbedding(text)

        val blob = LocalSemanticEmbeddingEngine.serializeEmbeddingToBlob(original)
        assertEquals(384 * 4, blob.size, "384 floats must serialize to 1536 bytes")

        val restored = LocalSemanticEmbeddingEngine.deserializeBlobToEmbedding(blob)
        assertEquals(384, restored.size)

        for (i in 0 until 384) {
            assertEquals(original[i], restored[i], "Float at index $i must match after roundtrip")
        }
    }
}
