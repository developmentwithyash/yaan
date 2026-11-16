package com.yaan.backend

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricsEndpointTest {

    @Test
    fun `test metrics returns prometheus formatted text with jvm and crdt gauges`() = testApplication {
        application {
            module()
        }

        val response = client.get("/metrics")
        assertEquals(HttpStatusCode.OK, response.status)

        val contentType = response.headers["Content-Type"] ?: ""
        assertTrue(contentType.contains("text/plain"), "Expected text/plain but was $contentType")

        val body = response.bodyAsText()
        assertTrue(body.contains("jvm_memory_used_bytes"), "Should contain jvm_memory_used_bytes")
        assertTrue(body.contains("jvm_memory_max_bytes"), "Should contain jvm_memory_max_bytes")
        assertTrue(body.contains("jvm_memory_free_bytes"), "Should contain jvm_memory_free_bytes")
        assertTrue(body.contains("yaan_crdt_users_active"), "Should contain yaan_crdt_users_active")
        assertTrue(body.contains("yaan_crdt_envelopes_total"), "Should contain yaan_crdt_envelopes_total")
        assertTrue(body.contains("yaan_crdt_highest_lamport"), "Should contain yaan_crdt_highest_lamport")
    }
}
