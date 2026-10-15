package com.yaan.backend

import com.yaan.backend.inference.PromptRequest
import com.yaan.backend.sync.SyncPushRequest
import com.yaan.backend.vault.VaultBackupEnvelope
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecurityAuthTest {

    @Test
    fun `unauthenticated access to vault backup is rejected with 401 Unauthorized`() = testApplication {
        application { module() }

        val response = client.get("/api/v1/vault/backup/latest?userId=exec_user_1")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Unauthorized"))
    }

    @Test
    fun `unauthenticated vault purge is rejected with 401 Unauthorized`() = testApplication {
        application { module() }

        val response = client.post("/api/v1/vault/purge?userId=exec_user_1")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Unauthorized"))
    }

    @Test
    fun `cross-tenant BOLA attack is rejected when client token does not match target user`() = testApplication {
        application { module() }

        // Attacker has token for "attacker_user", but tries to purge "victim_user"
        val response = client.post("/api/v1/vault/purge?userId=victim_user") {
            header("Authorization", "Bearer attacker_user:sig12345678")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Unauthorized"))
    }

    @Test
    fun `unauthenticated sync pull and push are rejected with 401 Unauthorized`() = testApplication {
        application { module() }

        // Pull
        val pullRes = client.get("/api/v1/sync/pull?userId=user_abc")
        assertEquals(HttpStatusCode.Unauthorized, pullRes.status)

        // Push
        val pushReq = SyncPushRequest(
            userId = "user_abc",
            deviceId = "dev_1",
            envelopes = emptyList()
        )
        val pushRes = client.post("/api/v1/sync/push") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(pushReq))
        }
        assertEquals(HttpStatusCode.Unauthorized, pushRes.status)
    }

    @Test
    fun `inference route rejects unauthenticated calls and invalid integrity tokens`() = testApplication {
        application { module() }

        val validPrompt = PromptRequest(prompt = "Summarize today's goals")

        // 1. Missing Auth -> 401
        val noAuthRes = client.post("/api/v1/inference/stream") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(validPrompt))
        }
        assertEquals(HttpStatusCode.Unauthorized, noAuthRes.status)

        // 2. Invalid Play Integrity Token -> 403 Forbidden
        val invalidIntegrityRes = client.post("/api/v1/inference/stream") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer valid_user_token_12345")
            header("X-Play-Integrity-Token", "INVALID_DEVICE_VERDICT")
            setBody(Json.encodeToString(validPrompt))
        }
        assertEquals(HttpStatusCode.Forbidden, invalidIntegrityRes.status)

        // 3. Oversized prompt (> 8192 chars) -> 400 Bad Request
        val hugePrompt = PromptRequest(prompt = "A".repeat(8193))
        val oversizedRes = client.post("/api/v1/inference/stream") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer valid_user_token_12345")
            setBody(Json.encodeToString(hugePrompt))
        }
        assertEquals(HttpStatusCode.BadRequest, oversizedRes.status)
        assertTrue(oversizedRes.bodyAsText().contains("8,192"))
    }

    @Test
    fun `authenticated requests with valid tokens succeed`() = testApplication {
        application { module() }

        val userId = "authorized_exec"
        val validToken = "Bearer authorized_exec:valid_signature_1234"

        // 1. Save backup with auth
        val envelope = VaultBackupEnvelope(
            backupId = "backup_auth_1",
            userId = userId,
            ciphertextBlob = "ENCRYPTED_VAULT_BYTES",
            sha256Checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sizeBytes = 1024L,
            createdAtMs = 1000L
        )

        val saveRes = client.post("/api/v1/vault/backup") {
            contentType(ContentType.Application.Json)
            header("Authorization", validToken)
            setBody(Json.encodeToString(envelope))
        }
        assertEquals(HttpStatusCode.OK, saveRes.status)

        // 2. Fetch latest backup with auth
        val fetchRes = client.get("/api/v1/vault/backup/latest?userId=$userId") {
            header("Authorization", validToken)
        }
        assertEquals(HttpStatusCode.OK, fetchRes.status)

        // 3. Purge with matching auth
        val purgeRes = client.post("/api/v1/vault/purge?userId=$userId") {
            header("Authorization", validToken)
        }
        assertEquals(HttpStatusCode.OK, purgeRes.status)
        assertTrue(purgeRes.bodyAsText().contains("GDPR-Article-17-Crypto-Shredding"))
    }
}
