package com.yaan.backend.vault

import com.yaan.backend.auth.AuthGuard
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.vaultRoutes(vaultService: VaultBackupService) {
    route("/api/v1/vault") {
        post("/backup") {
            try {
                val envelope = call.receive<VaultBackupEnvelope>()
                if (!AuthGuard.validateUserAuth(call, envelope.userId)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized: Invalid client credentials"))
                    return@post
                }

                val response = vaultService.saveBackup(envelope)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid backup payload")))
            }
        }

        get("/backup/latest") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing userId"))
                return@get
            }

            if (!AuthGuard.validateUserAuth(call, userId)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized: Access denied to user backup"))
                return@get
            }

            val latest = vaultService.getLatestBackup(userId)
            if (latest != null) {
                call.respond(HttpStatusCode.OK, latest)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No backups found for user"))
            }
        }

        post("/purge") {
            val userId = call.request.queryParameters["userId"]
            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing userId"))
                return@post
            }

            if (!AuthGuard.validateUserAuth(call, userId)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized: Access denied to purge operation"))
                return@post
            }

            val cert = vaultService.purgeAndShred(userId)
            call.respond(HttpStatusCode.OK, cert)
        }
    }
}
