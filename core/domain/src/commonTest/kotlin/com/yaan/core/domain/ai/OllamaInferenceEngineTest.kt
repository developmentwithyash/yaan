package com.yaan.core.domain.ai

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OllamaInferenceEngineTest {

    private class MockHttpTransport : OllamaHttpTransport {
        var lastPostUrl: String? = null
        var lastPostBody: String? = null
        var postResult: Result<String> = Result.success("{}")
        var getResult: Result<String> = Result.success("{}")

        override suspend fun postJson(url: String, jsonBody: String): Result<String> {
            lastPostUrl = url
            lastPostBody = jsonBody
            return postResult
        }

        override suspend fun get(url: String): Result<String> {
            return getResult
        }
    }

    @Test
    fun testGenerateSuccessfulResponseExtraction() = runTest {
        val mock = MockHttpTransport()
        mock.postResult = Result.success(
            """{"model":"llama3.2:3b","response":"I have scheduled your reminder.","done":true,"total_duration":420000000}"""
        )

        val engine = OllamaInferenceEngine(
            baseUrl = "http://localhost:11434",
            modelName = "llama3.2:3b",
            transport = mock
        )

        val result = engine.generate("Schedule team meeting")
        assertTrue(result.isSuccess)
        assertEquals("I have scheduled your reminder.", result.getOrNull())
        assertEquals("http://localhost:11434/api/generate", mock.lastPostUrl)
        assertTrue(mock.lastPostBody!!.contains("llama3.2:3b"))
        assertTrue(mock.lastPostBody!!.contains("Schedule team meeting"))
    }

    @Test
    fun testOfflineServiceGracefulFailure() = runTest {
        val mock = MockHttpTransport()
        mock.postResult = Result.failure(Exception("Connection refused (localhost:11434)"))
        mock.getResult = Result.failure(Exception("Connection refused"))

        val engine = OllamaInferenceEngine(
            baseUrl = "http://localhost:11434",
            modelName = "llama3.2:3b",
            transport = mock
        )

        val result = engine.generate("Hello")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Connection refused"))

        val available = engine.isAvailable()
        assertFalse(available)

        val status = engine.checkHealth()
        assertFalse(status.isOnline)
        assertEquals("http://localhost:11434", status.endpoint)
    }

    @Test
    fun testHealthCheckWithModelListing() = runTest {
        val mock = MockHttpTransport()
        mock.getResult = Result.success(
            """{"models":[{"name":"llama3.2:3b","size":2000000000},{"name":"qwen2.5:3b","size":1900000000}]}"""
        )

        val engine = OllamaInferenceEngine(
            baseUrl = "http://localhost:11434",
            transport = mock
        )

        val status = engine.checkHealth()
        assertTrue(status.isOnline)
        assertEquals(2, status.availableModels.size)
        assertTrue(status.availableModels.contains("llama3.2:3b"))
        assertTrue(status.availableModels.contains("qwen2.5:3b"))
    }

    @Test
    fun testEndpointAndModelReconfiguration() = runTest {
        val mock = MockHttpTransport()
        mock.postResult = Result.success("""{"model":"qwen2.5:7b","response":"Ready","done":true}""")

        val engine = OllamaInferenceEngine(
            baseUrl = "http://localhost:11434",
            modelName = "llama3.2:3b",
            transport = mock
        )

        // Reconfigure in Settings
        engine.baseUrl = "http://192.168.1.100:11434"
        engine.modelName = "qwen2.5:7b"

        val result = engine.generate("Status check")
        assertTrue(result.isSuccess)
        assertEquals("http://192.168.1.100:11434/api/generate", mock.lastPostUrl)
        assertTrue(mock.lastPostBody!!.contains("qwen2.5:7b"))
    }
}
