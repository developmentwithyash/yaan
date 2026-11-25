package com.yaan.feature.memory.embedding

/**
 * Common contract for local and cloud semantic vector embedding engines.
 */
interface EmbeddingModelEngine {
    val dimension: Int
    suspend fun generateEmbedding(text: String): FloatArray
}
