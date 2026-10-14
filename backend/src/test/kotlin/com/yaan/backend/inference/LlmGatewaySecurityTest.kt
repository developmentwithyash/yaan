package com.yaan.backend.inference

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlmGatewaySecurityTest {

    private fun createGateway(
        primaryFails: Boolean = false,
        fallbackFails: Boolean = false
    ): DefaultLlmGateway {
        val mockGroq = MockLlmAdapter(
            config = GroqLlmAdapter.DEFAULT_GROQ_CONFIG,
            failureMode = primaryFails
        )
        val mockFireworks = MockLlmAdapter(
            config = FireworksLlmAdapter.DEFAULT_FIREWORKS_CONFIG,
            failureMode = fallbackFails
        )
        return DefaultLlmGateway(
            adapters = mapOf(
                "groq" to mockGroq,
                "fireworks" to mockFireworks
            ),
            primaryProviderId = "groq",
            fallbackProviderId = "fireworks",
            rateLimitPerMinute = 5,
            dailyTokenCapPerUser = 1000L,
            failureThreshold = 2,
            resetTimeoutMs = 10_000L
        )
    }

    @Test
    fun testAuthenticationAndIntegrityValidation() = runBlocking {
        val gateway = createGateway()
        val request = PromptRequest(prompt = "When is my flight?")

        // Valid Token
        val validChunks = gateway.streamPrompt(request, clientId = "user_1", integrityToken = "PLAY_INTEGRITY_VALID").toList()
        assertTrue(validChunks.isNotEmpty())
        assertFalse(validChunks.first().text.contains("rejected"))

        // Invalid Token
        val invalidChunks = gateway.streamPrompt(request, clientId = "user_2", integrityToken = "INVALID_TAMPERED").toList()
        assertTrue(invalidChunks.first().text.contains("Client integrity validation failed"))
        assertEquals(401, gateway.auditLogs.last().httpStatus)
    }

    @Test
    fun testRequestSizeLimitEnforcement() = runBlocking {
        val gateway = createGateway()
        val oversizedPrompt = "A".repeat(8193)
        val request = PromptRequest(prompt = oversizedPrompt)

        val chunks = gateway.streamPrompt(request, clientId = "user_1", integrityToken = "VALID").toList()
        assertTrue(chunks.first().text.contains("exceeds 8,192 character security limit"))
        assertEquals(400, gateway.auditLogs.last().httpStatus)
    }

    @Test
    fun testPerInstallRateLimiting() = runBlocking {
        val gateway = createGateway() // Limit = 5 requests / min
        val request = PromptRequest(prompt = "Quick query")

        for (i in 1..5) {
            val chunks = gateway.streamPrompt(request, clientId = "rate_test_user", integrityToken = "VALID").toList()
            assertFalse(chunks.first().text.contains("Rate limit reached"))
        }

        // 6th request must be blocked
        val blockedChunks = gateway.streamPrompt(request, clientId = "rate_test_user", integrityToken = "VALID").toList()
        assertTrue(blockedChunks.first().text.contains("Rate limit reached"))
        assertEquals(429, gateway.auditLogs.last().httpStatus)
    }

    @Test
    fun testDailyTokenCapEnforcement() = runBlocking {
        val gateway = createGateway() // Daily cap = 1,000 tokens
        val longPrompt = "X".repeat(2000) // ~500 tokens per prompt
        val request = PromptRequest(prompt = longPrompt)

        // Request 1: ~500 prompt tokens + output tokens
        gateway.streamPrompt(request, clientId = "cap_user", integrityToken = "VALID").toList()

        // Request 2: pushes total over 1,000 tokens
        gateway.streamPrompt(request, clientId = "cap_user", integrityToken = "VALID").toList()

        // Request 3: must be capped with clear offline fallback message
        val cappedChunks = gateway.streamPrompt(request, clientId = "cap_user", integrityToken = "VALID").toList()
        assertTrue(cappedChunks.first().text.contains("Daily cloud token cap reached"))
        assertTrue(cappedChunks.first().text.contains("Offline local vault search remains fully active"))
    }

    @Test
    fun testZeroPlaintextPromptAndResponseLogging() = runBlocking {
        val gateway = createGateway()
        val privatePrompt = "Confidential medical diagnosis: Stage 2 Vitamin D deficiency"
        val request = PromptRequest(prompt = privatePrompt)

        gateway.streamPrompt(request, clientId = "private_client", integrityToken = "VALID").toList()

        val logs = gateway.auditLogs
        assertTrue(logs.isNotEmpty())

        for (log in logs) {
            // Verify no string property in audit log contains user prompt content
            assertFalse(log.toString().contains("Vitamin D"))
            assertFalse(log.toString().contains("diagnosis"))
            assertFalse(log.toString().contains("Confidential"))
            // Verify metadata is logged
            assertTrue(log.promptCharCount > 0)
            assertTrue(log.estimatedPromptTokens > 0)
            assertEquals("groq", log.providerId)
            assertEquals("llama-3.3-70b-versatile", log.modelName)
        }
    }

    @Test
    fun testCircuitBreakerFailoverAndLocalFallback() = runBlocking {
        // Primary fails, Fallback fails
        val gateway = createGateway(primaryFails = true, fallbackFails = true)
        val request = PromptRequest(prompt = "Check notes")

        // First attempt: trips primary breaker
        val chunks1 = gateway.streamPrompt(request, clientId = "cb_user", integrityToken = "VALID").toList()
        assertTrue(chunks1.last().text.contains("Showing local search results from your vault"))

        // Second attempt: trips threshold
        gateway.streamPrompt(request, clientId = "cb_user", integrityToken = "VALID").toList()

        // Third attempt: circuit open, tries fallback, fallback fails -> graceful degradation to local search
        val chunks3 = gateway.streamPrompt(request, clientId = "cb_user", integrityToken = "VALID").toList()
        assertTrue(chunks3.last().text.contains("Showing local search results from your vault"))
    }
}
