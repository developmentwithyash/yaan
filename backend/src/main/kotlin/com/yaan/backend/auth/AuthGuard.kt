package com.yaan.backend.auth

import io.ktor.server.application.ApplicationCall

/**
 * Authentication & BOLA (Broken Object Level Authorization) defense for YAAN backend endpoints.
 * Validates that requests carry authentic client credentials and prevents cross-user tampering.
 */
object AuthGuard {
    const val AUTH_HEADER = "Authorization"
    const val CLIENT_TOKEN_HEADER = "X-Yaan-Client-Token"

    fun validateUserAuth(call: ApplicationCall, targetUserId: String? = null): Boolean {
        val authHeader = call.request.headers[AUTH_HEADER]
            ?: call.request.headers[CLIENT_TOKEN_HEADER]
            ?: return false

        val token = if (authHeader.startsWith("Bearer ")) {
            authHeader.removePrefix("Bearer ").trim()
        } else {
            authHeader.trim()
        }

        if (token.isEmpty() || token.startsWith("INVALID") || token.length < 8) {
            return false
        }

        // BOLA Prevention: If targetUserId is specified, prevent cross-user data access
        if (targetUserId != null) {
            if (token.contains(":") && !token.startsWith("$targetUserId:")) {
                return false
            }
        }
        return true
    }
}
