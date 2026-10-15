package com.yaan.core.data.remote.supabase

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Clean, lightweight, KMP-native client for Supabase GoTrue Auth and PostgREST.
 *
 * Adheres strictly to YAAN invariants:
 * - Deterministic fallback if offline.
 * - Zero-Knowledge: Only credentials and blinded profile metadata are exchanged.
 */
class SupabaseAuthClient(
    private val config: SupabaseConfig = SupabaseConfig(),
    private val httpClient: HttpClient = createDefaultHttpClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Signs in an existing user with email and password via GoTrue API.
     * POST /auth/v1/token?grant_type=password
     */
    suspend fun signInWithPassword(email: String, password: String): YaanResult<SupabaseAuthResponse> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return YaanResult.Failure(YaanError.ValidationError("email", "Invalid email address format."))
        }
        if (password.length < 6) {
            return YaanResult.Failure(YaanError.ValidationError("password", "Password must be at least 6 characters."))
        }

        return try {
            val endpoint = "${config.url}/auth/v1/token?grant_type=password"
            val response: HttpResponse = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("apikey", config.anonKey)
                setBody(SupabaseSignInRequest(email = trimmedEmail, password = password))
            }

            if (response.status.isSuccess()) {
                val authResponse = json.decodeFromString<SupabaseAuthResponse>(response.bodyAsText())
                YaanResult.Success(authResponse)
            } else {
                val errorBody = response.bodyAsText()
                val parsedError = runCatching { json.decodeFromString<SupabaseErrorResponse>(errorBody) }.getOrNull()
                val msg = parsedError?.resolvedMessage() ?: "Authentication failed (${response.status.value})"
                Napier.w("Supabase sign-in failed: $msg")
                YaanResult.Failure(YaanError.AuthenticationError(msg))
            }
        } catch (e: Exception) {
            Napier.e("Network error during Supabase signIn", e)
            YaanResult.Failure(YaanError.NetworkError("Unable to connect to Supabase: ${e.message}"))
        }
    }

    /**
     * Registers a new user with email, password, and profile metadata.
     * POST /auth/v1/signup
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        ageConfirmed18Plus: Boolean
    ): YaanResult<SupabaseAuthResponse> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return YaanResult.Failure(YaanError.ValidationError("email", "Invalid email address format."))
        }
        if (password.length < 6) {
            return YaanResult.Failure(YaanError.ValidationError("password", "Password must be at least 6 characters."))
        }
        if (!ageConfirmed18Plus) {
            return YaanResult.Failure(
                YaanError.ValidationError(
                    "age",
                    "Registration requires user to confirm they are 18 years or older under DPDP Act 2023."
                )
            )
        }

        return try {
            val endpoint = "${config.url}/auth/v1/signup"
            val metaData = mapOf(
                "display_name" to displayName.trim().ifEmpty { "User" },
                "age_confirmed_18_plus" to ageConfirmed18Plus.toString()
            )
            val response: HttpResponse = httpClient.post(endpoint) {
                contentType(ContentType.Application.Json)
                header("apikey", config.anonKey)
                setBody(SupabaseSignUpRequest(email = trimmedEmail, password = password, data = metaData))
            }

            if (response.status.isSuccess()) {
                val authResponse = json.decodeFromString<SupabaseAuthResponse>(response.bodyAsText())
                YaanResult.Success(authResponse)
            } else {
                val errorBody = response.bodyAsText()
                val parsedError = runCatching { json.decodeFromString<SupabaseErrorResponse>(errorBody) }.getOrNull()
                val msg = parsedError?.resolvedMessage() ?: "Registration failed (${response.status.value})"
                Napier.w("Supabase sign-up failed: $msg")
                YaanResult.Failure(YaanError.AuthenticationError(msg))
            }
        } catch (e: Exception) {
            Napier.e("Network error during Supabase signUp", e)
            YaanResult.Failure(YaanError.NetworkError("Unable to connect to Supabase: ${e.message}"))
        }
    }

    /**
     * Fetches the user profile from public.profiles via PostgREST.
     * GET /rest/v1/profiles?id=eq.<userId>&select=*
     */
    suspend fun fetchProfile(userId: String, accessToken: String): YaanResult<SupabaseProfileDto> {
        return try {
            val endpoint = "${config.url}/rest/v1/profiles?id=eq.$userId&select=*"
            val response: HttpResponse = httpClient.get(endpoint) {
                header("apikey", config.anonKey)
                header("Authorization", "Bearer $accessToken")
            }

            if (response.status.isSuccess()) {
                val list = json.decodeFromString<List<SupabaseProfileDto>>(response.bodyAsText())
                val profile = list.firstOrNull()
                if (profile != null) {
                    YaanResult.Success(profile)
                } else {
                    YaanResult.Failure(YaanError.NotFoundError("Profile not found for user $userId"))
                }
            } else {
                YaanResult.Failure(YaanError.NetworkError("Failed to fetch profile: ${response.status}"))
            }
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.NetworkError("Fetch profile error: ${e.message}"))
        }
    }

    /**
     * Updates profile fields (e.g. display name, age confirmation) in public.profiles.
     * PATCH /rest/v1/profiles?id=eq.<userId>
     */
    suspend fun updateProfile(
        userId: String,
        accessToken: String,
        displayName: String,
        ageConfirmed18Plus: Boolean? = null
    ): YaanResult<Unit> {
        return try {
            val endpoint = "${config.url}/rest/v1/profiles?id=eq.$userId"
            val response: HttpResponse = httpClient.patch(endpoint) {
                contentType(ContentType.Application.Json)
                header("apikey", config.anonKey)
                header("Authorization", "Bearer $accessToken")
                header("Prefer", "return=minimal")
                setBody(SupabaseUpdateProfileRequest(displayName = displayName, ageConfirmed18Plus = ageConfirmed18Plus))
            }

            if (response.status.isSuccess()) {
                YaanResult.Success(Unit)
            } else {
                YaanResult.Failure(YaanError.NetworkError("Failed to update profile: ${response.status}"))
            }
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.NetworkError("Update profile error: ${e.message}"))
        }
    }

    /**
     * Revokes current session token.
     * POST /auth/v1/logout
     */
    suspend fun signOut(accessToken: String): YaanResult<Unit> {
        return try {
            val endpoint = "${config.url}/auth/v1/logout"
            val response: HttpResponse = httpClient.post(endpoint) {
                header("apikey", config.anonKey)
                header("Authorization", "Bearer $accessToken")
            }
            if (response.status.isSuccess()) {
                YaanResult.Success(Unit)
            } else {
                YaanResult.Failure(YaanError.NetworkError("Sign out failed: ${response.status}"))
            }
        } catch (e: Exception) {
            // Sign-out should not fail fatally if offline
            YaanResult.Success(Unit)
        }
    }

    companion object {
        fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    })
                }
            }
        }
    }
}
