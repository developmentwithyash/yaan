package com.yaan.backend.planner

import com.yaan.backend.auth.AuthGuard
import com.yaan.core.domain.planner.PlannerRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.plannerRoutes(plannerService: PlannerService) {
    route("/api/v1/planner") {
        post("/plan") {
            // 1. Authenticate client
            if (!AuthGuard.validateUserAuth(call)) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Unauthorized: Invalid or missing client token")
                )
                return@post
            }

            // 2. Extract Client Identity & Integrity Tokens
            val clientId = call.request.headers[AuthGuard.CLIENT_TOKEN_HEADER]
                ?: call.request.headers[AuthGuard.AUTH_HEADER]
                ?: "anonymous_client"
            val integrityToken = call.request.headers["X-Play-Integrity-Token"]
                ?: call.request.headers["X-Apple-App-Attest"]

            val request = try {
                call.receive<PlannerRequest>()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid planner request schema: ${e.message}")
                )
                return@post
            }

            // 3. Process Plan
            val response = plannerService.plan(request, clientId, integrityToken)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
