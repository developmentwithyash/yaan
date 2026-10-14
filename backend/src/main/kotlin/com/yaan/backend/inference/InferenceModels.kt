package com.yaan.backend.inference

import kotlinx.serialization.Serializable

@Serializable
data class PromptRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val modelPreference: String = "fast", // "fast", "deep", "balanced"
    val maxTokens: Int = 512,
    val temperature: Float = 0.2f
)

@Serializable
data class TokenChunk(
    val text: String,
    val isLast: Boolean = false,
    val latencyMs: Long,
    val providerUsed: String
)

@Serializable
data class ProviderMetrics(
    val name: String,
    val state: String,
    val averageLatencyMs: Long,
    val totalRequests: Long,
    val successfulRequests: Long
)

@Serializable
data class InferenceMeshStatus(
    val activePrimary: String,
    val fallbackActive: Boolean,
    val latencyCeilingMs: Long,
    val providers: List<ProviderMetrics>
)
