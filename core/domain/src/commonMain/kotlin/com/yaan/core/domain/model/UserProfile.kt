package com.yaan.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccountTier {
    FREE, PRO, TEAM
}

/**
 * Domain entity representing an authenticated user identity and security session.
 */
@Serializable
data class UserProfile(
    val userId: String,
    val displayName: String,
    val email: String? = null,
    val tier: AccountTier = AccountTier.PRO,
    val authToken: String,
    val biometricLockEnabled: Boolean = true,
    val isVaultLocked: Boolean = true,
    val pairedDevicesCount: Int = 1,
    val passkeyRegistered: Boolean = false,
    val isOnboardingCompleted: Boolean = true,
    val ageConfirmed18Plus: Boolean = true,
    val createdAtEpochMs: Long = 0L
)
