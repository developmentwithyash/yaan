package com.yaan.backend

import com.yaan.backend.config.AppConfig
import com.yaan.backend.ratelimit.TokenBucketRateLimiter
import com.yaan.backend.sync.CrdtRelayService
import com.yaan.backend.sync.EncryptedSyncEnvelope
import com.yaan.backend.sync.EntityType
import com.yaan.backend.sync.SyncPushRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimitingTest {

    @Test
    fun `test TokenBucketRateLimiter allows burst up to capacity and rejects subsequent calls`() {
        val limiter = TokenBucketRateLimiter(capacity = 3, refillTokensPerSecond = 1.0)
        val now = 1000L

        assertTrue(limiter.tryAcquire("user1", nowMs = now))
        assertTrue(limiter.tryAcquire("user1", nowMs = now))
        assertTrue(limiter.tryAcquire("user1", nowMs = now))
        assertFalse(limiter.tryAcquire("user1", nowMs = now), "Excess request should be throttled")

        // 1 second later -> 1 token refilled
        assertTrue(limiter.tryAcquire("user1", nowMs = now + 1000L), "Refilled token should be acquirable")
        assertFalse(limiter.tryAcquire("user1", nowMs = now + 1000L), "Only 1 token was refilled")
    }

    @Test
    fun `test push endpoint returns 429 Too Many Requests when rate limit exceeded`() = testApplication {
        val limiter = TokenBucketRateLimiter(capacity = 2, refillTokensPerSecond = 1.0)
        val relayService = CrdtRelayService()

        application {
            module(
                config = AppConfig(environment = "test"),
                relayService = relayService,
                syncRateLimiter = limiter
            )
        }

        val pushRequest = SyncPushRequest(
            userId = "rate_limit_user",
            deviceId = "dev_1",
            envelopes = listOf(
                EncryptedSyncEnvelope(
                    id = "env_1",
                    userId = "rate_limit_user",
                    deviceId = "dev_1",
                    entityType = EntityType.TASK,
                    ciphertext = "enc",
                    nonce = "nonce",
                    lamportClock = 1L,
                    epoch = 1L,
                    timestampMs = 1000L
                )
            )
        )
        val requestJson = Json.encodeToString(SyncPushRequest.serializer(), pushRequest)

        val makeRequest = suspend {
            client.post("/api/v1/sync/push") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer dev-bypass-token")
                setBody(requestJson)
            }
        }

        val resp1 = makeRequest()
        assertEquals(HttpStatusCode.OK, resp1.status)

        val resp2 = makeRequest()
        assertEquals(HttpStatusCode.OK, resp2.status)

        val resp3 = makeRequest()
        assertEquals(HttpStatusCode.TooManyRequests, resp3.status)
        assertEquals("1", resp3.headers["Retry-After"])
    }
}
