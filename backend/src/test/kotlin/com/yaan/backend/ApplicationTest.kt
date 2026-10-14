package com.yaan.backend

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun `test healthz returns healthy with 4 invariants`() = testApplication {
        application {
            module()
        }

        val response = client.get("/healthz")
        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("HEALTHY"))
        assertTrue(body.contains("YAAN Asymmetric Acceleration Hub"))
        assertTrue(body.contains("Deterministic State Decoupled from Probabilistic AI"))
        assertTrue(body.contains("Zero-Knowledge E2EE Blind Relay"))
        assertTrue(body.contains("Sub-150ms Interaction Ceiling"))
    }
}
