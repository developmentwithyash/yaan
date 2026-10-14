package com.yaan.backend.sync

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Sync Routes
 *
 * Exposes endpoints for blind CRDT state relaying:
 * - POST /api/v1/sync/push
 * - GET  /api/v1/sync/pull
 */
fun Route.syncRoutes(relayService: CrdtRelayService) {
    route("/api/v1/sync") {
        post("/push") {
            try {
                val request = call.receive<SyncPushRequest>()
                val response = relayService.push(request)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid push payload")))
            }
        }

        get("/pull") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing userId parameter"))
                return@get
            }

            val sinceLamport = call.request.queryParameters["since"]?.toLongOrNull() ?: 0L
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100

            val response = relayService.pull(userId = userId, sinceLamport = sinceLamport, limit = limit)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
