package com.yaan.backend.briefing

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.briefingRoutes(briefingService: BriefingWorkerService) {
    route("/api/v1/briefing") {
        post("/generate") {
            try {
                val request = call.receive<GenerateBriefingRequest>()
                val briefing = briefingService.generateBriefing(request)
                call.respond(HttpStatusCode.OK, briefing)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Failed to generate briefing")))
            }
        }

        get("/latest") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing userId"))
                return@get
            }

            val briefing = briefingService.getLatestBriefing(userId)
            if (briefing != null) {
                call.respond(HttpStatusCode.OK, briefing)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No precomputed briefing found for user"))
            }
        }
    }
}
