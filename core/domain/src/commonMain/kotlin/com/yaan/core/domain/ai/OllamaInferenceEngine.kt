package com.yaan.core.domain.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Common contract for local and remote LLM inference engines.
 */
interface LlmInferenceEngine {
    suspend fun generate(prompt: String): Result<String>
    suspend fun isAvailable(): Boolean
}

/**
 * Interface abstraction for HTTP transport communicating with Ollama REST API.
 */
interface OllamaHttpTransport {
    suspend fun postJson(url: String, jsonBody: String): Result<String>
    suspend fun get(url: String): Result<String>
}

@Serializable
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val options: Map<String, String>? = null
)

@Serializable
data class OllamaGenerateResponse(
    val model: String = "",
    val response: String = "",
    val done: Boolean = true,
    @SerialName("total_duration")
    val totalDuration: Long? = null
)

@Serializable
data class OllamaModelTag(
    val name: String,
    val size: Long? = null
)

@Serializable
data class OllamaTagsResponse(
    val models: List<OllamaModelTag> = emptyList()
)

data class OllamaStatus(
    val isOnline: Boolean,
    val endpoint: String,
    val availableModels: List<String> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Local Ollama / llama.cpp REST client implementing LlmInferenceEngine.
 * Satisfies Issue #10:
 * - Direct routing to local endpoint (default: http://localhost:11434).
 * - Configurable endpoint and model (e.g. llama3.2:3b, qwen2.5:3b).
 * - Graceful error handling and health checking when local service is offline.
 * - JSON serialization of generate requests and tag listings.
 */
class OllamaInferenceEngine(
    var baseUrl: String = DEFAULT_BASE_URL,
    var modelName: String = DEFAULT_MODEL,
    private val transport: OllamaHttpTransport
) : LlmInferenceEngine {

    companion object {
        const val DEFAULT_BASE_URL = "http://localhost:11434"
        const val DEFAULT_MODEL = "llama3.2:3b"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    override suspend fun generate(prompt: String): Result<String> {
        val endpoint = "${baseUrl.trimEnd('/')}/api/generate"
        val request = OllamaGenerateRequest(
            model = modelName,
            prompt = prompt,
            stream = false
        )

        val requestJson = json.encodeToString(OllamaGenerateRequest.serializer(), request)
        val postResult = transport.postJson(endpoint, requestJson)

        return postResult.mapCatching { body ->
            val parsed = json.decodeFromString(OllamaGenerateResponse.serializer(), body)
            parsed.response
        }
    }

    override suspend fun isAvailable(): Boolean {
        return checkHealth().isOnline
    }

    suspend fun checkHealth(): OllamaStatus {
        val endpoint = "${baseUrl.trimEnd('/')}/api/tags"
        val getResult = transport.get(endpoint)

        return getResult.fold(
            onSuccess = { body ->
                try {
                    val parsed = json.decodeFromString(OllamaTagsResponse.serializer(), body)
                    OllamaStatus(
                        isOnline = true,
                        endpoint = baseUrl,
                        availableModels = parsed.models.map { it.name }
                    )
                } catch (e: Throwable) {
                    OllamaStatus(
                        isOnline = false,
                        endpoint = baseUrl,
                        errorMessage = e.message ?: "Failed to parse Ollama tags response"
                    )
                }
            },
            onFailure = { err ->
                OllamaStatus(
                    isOnline = false,
                    endpoint = baseUrl,
                    errorMessage = err.message ?: "Service offline"
                )
            }
        )
    }
}
