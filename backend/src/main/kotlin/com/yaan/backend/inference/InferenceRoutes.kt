package com.yaan.backend.inference

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.inferenceRoutes(meshService: InferenceMeshService) {
    route("/api/v1/inference") {
        post("/stream") {
            try {
                val request = call.receive<PromptRequest>()
                // Collect tokens and respond or stream as NDJSON
                val tokens = meshService.streamPrompt(request).toList()
                val jsonPayload = Json.encodeToString(tokens)
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
