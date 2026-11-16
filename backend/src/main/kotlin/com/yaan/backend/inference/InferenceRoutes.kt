package com.yaan.backend.inference

import com.yaan.backend.auth.AuthGuard
import com.yaan.backend.ratelimit.TokenBucketRateLimiter
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.toList

fun Route.inferenceRoutes(
    meshService: InferenceMeshService,
    rateLimiter: TokenBucketRateLimiter = TokenBucketRateLimiter()
) {
    route("/api/v1/inference") {
        post("/stream") {
            try {
                // 1. Authenticate client
                if (!AuthGuard.validateUserAuth(call)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized: Invalid or missing client token"))
                    return@post
                }

                // 2. Client integrity validation
                val integrityToken = call.request.headers["X-Play-Integrity-Token"]
                if (integrityToken != null && integrityToken.startsWith("INVALID")) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Client integrity verification failed"))
                    return@post
                }

                val clientKey = call.request.headers["Authorization"] ?: "anonymous"
                if (!rateLimiter.tryAcquire(clientKey)) {
                    call.response.header("Retry-After", "1")
                    call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Rate limit exceeded for inference calls."))
                    return@post
                }

                val request = call.receive<PromptRequest>()

                // 3. Prompt size limit check (TM-04 defense)
                if (request.prompt.length > 8192) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Request rejected: Prompt length exceeds 8,192 character security limit."))
                    return@post
                }

                // Collect tokens and respond
                val tokens = meshService.streamPrompt(request).toList()
                call.respond(HttpStatusCode.OK, tokens)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Inference error")))
            }
        }

        get("/status") {
            call.respond(HttpStatusCode.OK, meshService.getStatus())
        }
    }
}
