package com.yaan.core.data.remote.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SupabaseSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class SupabaseSignInRequest(
    val email: String,
    val password: String
)

@Serializable
data class SupabaseAuthResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: SupabaseUserDto? = null
)

@Serializable
data class SupabaseUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class SupabaseProfileDto(
    val id: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String = "",
    val tier: String = "FREE",
    @SerialName("age_confirmed_18_plus") val ageConfirmed18Plus: Boolean = false
)

@Serializable
data class SupabaseUpdateProfileRequest(
    @SerialName("display_name") val displayName: String,
    @SerialName("age_confirmed_18_plus") val ageConfirmed18Plus: Boolean? = null
)

@Serializable
data class SupabaseErrorResponse(
    val message: String? = null,
    val msg: String? = null,
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
) {
    fun resolvedMessage(): String =
        errorDescription ?: message ?: msg ?: error ?: "Unknown Supabase error"
}
