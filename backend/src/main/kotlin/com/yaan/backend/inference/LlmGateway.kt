package com.yaan.backend.inference

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Configuration for an external cloud LLM inference provider.
 * All pricing, models, and endpoints are dynamically configurable.
 */
@Serializable
data class ProviderConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val modelName: String,
    val inputPricePer1MUsd: Double,
    val outputPricePer1MUsd: Double,
    val timeoutMs: Long = 10_000L,
    val isZeroDataRetention: Boolean = false,
    val maxPromptChars: Int = 8192,
    val apiKeyConfigKey: String
)

/**
 * Metadata logged per request. Notice: NO PLAINTEXT PROMPT OR RESPONSE CONTENT IS LOGGED.
 */
@Serializable
data class InferenceAuditLog(
    val requestId: String,
    val clientIdHash: String,
    val providerId: String,
    val modelName: String,
    val promptCharCount: Int,
    val estimatedPromptTokens: Int,
    val generatedTokenCount: Int,
    val latencyMs: Long,
    val httpStatus: Int,
    val isFallback: Boolean,
    val timestampMs: Long
)

/**
 * Contract for a pluggable LLM provider adapter.
 */
interface LlmProviderAdapter {
    val config: ProviderConfig
    fun isAvailable(): Boolean
    fun streamCompletion(request: PromptRequest): Flow<TokenChunk>
}

/**
 * High-level provider-agnostic gateway interface.
 */
interface LlmGateway {
    fun streamPrompt(
        request: PromptRequest,
        clientId: String,
        integrityToken: String?,
        preferredProviderId: String? = null
    ): Flow<TokenChunk>

    fun getProvider(providerId: String): LlmProviderAdapter?
    fun listProviders(): List<ProviderConfig>
    fun getDailyTokensUsed(clientId: String): Long
    fun isRateLimited(clientId: String): Boolean
}
