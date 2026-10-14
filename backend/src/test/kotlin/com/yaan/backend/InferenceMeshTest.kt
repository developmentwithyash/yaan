package com.yaan.backend

import com.yaan.backend.config.AppConfig
import com.yaan.backend.inference.CircuitBreaker
import com.yaan.backend.inference.CircuitState
import com.yaan.backend.inference.InferenceMeshService
import com.yaan.backend.inference.PromptRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InferenceMeshTest {

    @Test
    fun `test circuit breaker transitions from closed to open on threshold failure`() {
        val breaker = CircuitBreaker(failureThreshold = 3, resetTimeoutMs = 500)
        assertEquals(CircuitState.CLOSED, breaker.getState())

        breaker.recordFailure()
        breaker.recordFailure()
        assertEquals(CircuitState.CLOSED, breaker.getState())

        // Third failure trips breaker
        breaker.recordFailure()
        assertEquals(CircuitState.OPEN, breaker.getState())

        // Reset restores CLOSED state
        breaker.reset()
        assertEquals(CircuitState.CLOSED, breaker.getState())
    }

    @Test
    fun `test inference mesh streaming generates tokens`() = runBlocking {
        val service = InferenceMeshService(AppConfig(latencyCeilingMs = 150L))
        val request = PromptRequest(
            prompt = "Review my daily briefing and conflicts",
            maxTokens = 64
        )

        val chunks = service.streamPrompt(request).toList()
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.last().isLast)
        assertEquals("Groq LPU (Primary Asymmetric)", chunks.first().providerUsed)

        val status = service.getStatus()
        assertEquals("Groq LPU", status.activePrimary)
        assertFalse(status.fallbackActive)
    }

    @Test
    fun `test inference mesh routes to fallback when breaker is tripped`() = runBlocking {
        val service = InferenceMeshService(AppConfig())
        // Trip circuit breaker manually
        service.primaryBreaker.recordFailure()
        service.primaryBreaker.recordFailure()
        service.primaryBreaker.recordFailure()

        assertEquals(CircuitState.OPEN, service.primaryBreaker.getState())

        val request = PromptRequest(prompt = "Schedule sync")
        val chunks = service.streamPrompt(request).toList()

        assertTrue(chunks.isNotEmpty())
        assertEquals("Fireworks AI (Fallback Reserve)", chunks.first().providerUsed)
    }
}
