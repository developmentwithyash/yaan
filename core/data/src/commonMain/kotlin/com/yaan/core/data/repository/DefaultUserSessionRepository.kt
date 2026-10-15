package com.yaan.core.data.repository

import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.crypto.VaultKeyManager
import com.yaan.core.data.remote.supabase.SupabaseAuthClient
import com.yaan.core.domain.model.AccountTier
import com.yaan.core.domain.model.UserProfile
import com.yaan.core.domain.repository.UserSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive

/**
 * High-performance, zero-knowledge implementation of UserSessionRepository.
 * Maintains local session state, biometric gate status, and crypto-shredding hooks,
 * with cloud synchronization via Supabase GoTrue Auth and PostgreSQL PostgREST.
 */
class DefaultUserSessionRepository(
    private val keyManager: VaultKeyManager,
    private val dispatchers: CoroutineDispatchers,
    private val supabaseAuthClient: SupabaseAuthClient? = null,
    initialProfile: UserProfile? = null
) : UserSessionRepository {

    private val profileState = MutableStateFlow(
        initialProfile ?: UserProfile(
            userId = "exec_elena_01",
            displayName = "Elena Rostova",
            email = "elena.rostova@executive.yaan.ai",
            tier = AccountTier.PRO,
            authToken = "exec_elena_01:sig_ecdsa_98a7f14b2d6e",
            biometricLockEnabled = true,
            isVaultLocked = false,
            pairedDevicesCount = 2,
            passkeyRegistered = true,
            createdAtEpochMs = 1776200000000L
        )
    )

    override fun observeProfile(): Flow<UserProfile> = profileState.asStateFlow()

    override fun getCurrentProfile(): UserProfile = profileState.value

    override suspend fun updateProfile(displayName: String, email: String?): YaanResult<UserProfile> =
        withContext(dispatchers.io) {
            val current = profileState.value
            val updated = current.copy(
                displayName = displayName.trim().ifEmpty { current.displayName },
                email = email?.trim()
            )
            profileState.value = updated
            YaanResult.Success(updated)
        }

    override suspend fun setBiometricLock(enabled: Boolean): YaanResult<Unit> =
        withContext(dispatchers.io) {
            val current = profileState.value
            profileState.value = current.copy(biometricLockEnabled = enabled)
            YaanResult.Success(Unit)
        }

    override suspend fun unlockVault(): YaanResult<Unit> =
        withContext(dispatchers.main) {
            val current = profileState.value
            profileState.value = current.copy(isVaultLocked = false)
            YaanResult.Success(Unit)
        }

    override fun lockVault() {
        val current = profileState.value
        profileState.value = current.copy(isVaultLocked = true)
    }

    override suspend fun registerPasskey(): YaanResult<Unit> =
        withContext(dispatchers.io) {
            val current = profileState.value
            profileState.value = current.copy(passkeyRegistered = true)
            YaanResult.Success(Unit)
        }

    override suspend fun pairDeviceWithQr(qrCodePayload: String): YaanResult<Int> =
        withContext(dispatchers.io) {
            if (qrCodePayload.length < 16) {
                return@withContext YaanResult.Failure(
                    YaanError.ValidationError("qrCode", "Invalid device pairing token")
                )
            }
            val current = profileState.value
            val newCount = current.pairedDevicesCount + 1
            profileState.value = current.copy(pairedDevicesCount = newCount)
            YaanResult.Success(newCount)
        }

    override suspend fun completeOnboarding(displayName: String, ageConfirmed18Plus: Boolean): YaanResult<UserProfile> =
        withContext(dispatchers.io) {
            if (!ageConfirmed18Plus) {
                return@withContext YaanResult.Failure(
                    YaanError.ValidationError(
                        field = "age",
                        message = "YAAN Beta requires users to be 18 years or older in compliance with the Digital Personal Data Protection Act 2023."
                    )
                )
            }
            val current = profileState.value
            val cleanName = displayName.trim().ifEmpty { "Elena Rostova" }
            val updated = current.copy(
                displayName = cleanName,
                isOnboardingCompleted = true,
                ageConfirmed18Plus = true,
                isVaultLocked = false
            )
            profileState.value = updated
            YaanResult.Success(updated)
        }

    override suspend fun signInWithEmail(email: String, password: String): YaanResult<UserProfile> =
        withContext(dispatchers.io) {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
                return@withContext YaanResult.Failure(
                    YaanError.ValidationError("email", "Please enter a valid email address.")
                )
            }
            if (password.length < 6) {
                return@withContext YaanResult.Failure(
                    YaanError.ValidationError("password", "Password must be at least 6 characters.")
                )
            }

            if (supabaseAuthClient != null) {
                when (val authRes = supabaseAuthClient.signInWithPassword(trimmedEmail, password)) {
                    is YaanResult.Success -> {
                        val auth = authRes.data
                        val userId = auth.user?.id ?: "usr_${(100000..999999).random()}"
                        
                        // Fetch extended profile if available
                        val profileRes = supabaseAuthClient.fetchProfile(userId, auth.accessToken)
                        val profile = (profileRes as? YaanResult.Success)?.data

                        val metaName = auth.user?.userMetadata?.get("display_name")?.jsonPrimitive?.content
                        val resolvedName = profile?.displayName?.ifEmpty { null }
                            ?: metaName?.ifEmpty { null }
                            ?: trimmedEmail.substringBefore("@")
                        val resolvedTier = parseTier(profile?.tier)

                        val updated = UserProfile(
                            userId = userId,
                            displayName = resolvedName,
                            email = trimmedEmail,
                            tier = resolvedTier,
                            authToken = auth.accessToken,
                            biometricLockEnabled = profileState.value.biometricLockEnabled,
                            isVaultLocked = false,
                            pairedDevicesCount = profileState.value.pairedDevicesCount,
                            passkeyRegistered = profileState.value.passkeyRegistered,
                            isOnboardingCompleted = true,
                            ageConfirmed18Plus = profile?.ageConfirmed18Plus ?: true,
                            createdAtEpochMs = com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()
                        )
                        profileState.value = updated
                        YaanResult.Success(updated)
                    }
                    is YaanResult.Failure -> YaanResult.Failure(authRes.error)
                }
            } else {
                // Offline fallback / mock mode
                val current = profileState.value
                val updated = current.copy(
                    userId = "usr_local_${trimmedEmail.hashCode().toString().take(8)}",
                    displayName = trimmedEmail.substringBefore("@"),
                    email = trimmedEmail,
                    authToken = "mock_supabase_jwt_${trimmedEmail.hashCode()}",
                    isVaultLocked = false,
                    isOnboardingCompleted = true
                )
                profileState.value = updated
                YaanResult.Success(updated)
            }
        }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        ageConfirmed18Plus: Boolean
    ): YaanResult<UserProfile> =
        withContext(dispatchers.io) {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
                return@withContext YaanResult.Failure(
                    YaanError.ValidationError("email", "Please enter a valid email address.")
                )
            }
            if (password.length < 6) {
                return@withContext YaanResult.Failure(
                    YaanError.ValidationError("password", "Password must be at least 6 characters.")
                )
            }
            if (!ageConfirmed18Plus) {
                return@withContext YaanResult.Failure(
                    YaanError.ValidationError(
                        field = "age",
                        message = "YAAN Beta requires users to be 18 years or older in compliance with the Digital Personal Data Protection Act 2023."
                    )
                )
            }

            val cleanName = displayName.trim().ifEmpty { trimmedEmail.substringBefore("@") }

            if (supabaseAuthClient != null) {
                when (val authRes = supabaseAuthClient.signUp(trimmedEmail, password, cleanName, ageConfirmed18Plus)) {
                    is YaanResult.Success -> {
                        val auth = authRes.data
                        val userId = auth.user?.id ?: "usr_${(100000..999999).random()}"
                        val updated = UserProfile(
                            userId = userId,
                            displayName = cleanName,
                            email = trimmedEmail,
                            tier = AccountTier.FREE,
                            authToken = auth.accessToken.ifEmpty { "pending_email_verification" },
                            biometricLockEnabled = profileState.value.biometricLockEnabled,
                            isVaultLocked = false,
                            pairedDevicesCount = 1,
                            passkeyRegistered = false,
                            isOnboardingCompleted = true,
                            ageConfirmed18Plus = true,
                            createdAtEpochMs = com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()
                        )
                        profileState.value = updated
                        YaanResult.Success(updated)
                    }
                    is YaanResult.Failure -> YaanResult.Failure(authRes.error)
                }
            } else {
                // Offline fallback / mock mode
                val current = profileState.value
                val updated = current.copy(
                    userId = "usr_local_${trimmedEmail.hashCode().toString().take(8)}",
                    displayName = cleanName,
                    email = trimmedEmail,
                    authToken = "mock_supabase_jwt_${trimmedEmail.hashCode()}",
                    isVaultLocked = false,
                    isOnboardingCompleted = true,
                    ageConfirmed18Plus = true
                )
                profileState.value = updated
                YaanResult.Success(updated)
            }
        }

    override suspend fun logoutAndCryptoShred(): YaanResult<Unit> =
        withContext(dispatchers.io) {
            val current = profileState.value
            // 1. Invalidate remote cloud session if Supabase is active
            if (supabaseAuthClient != null && current.authToken.startsWith("ey")) {
                runCatching { supabaseAuthClient.signOut(current.authToken) }
            }

            // 2. Shred local master encryption keys
            keyManager.shredKey()

            // 3. Reset session to locked anonymous un-onboarded state
            profileState.value = UserProfile(
                userId = "anonymous_user",
                displayName = "Anonymous User",
                email = null,
                tier = AccountTier.FREE,
                authToken = "anonymous:revoked",
                biometricLockEnabled = false,
                isVaultLocked = true,
                pairedDevicesCount = 1,
                passkeyRegistered = false,
                isOnboardingCompleted = false,
                ageConfirmed18Plus = false,
                createdAtEpochMs = com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()
            )
            YaanResult.Success(Unit)
        }

    private fun parseTier(tierStr: String?): AccountTier = when (tierStr?.uppercase()) {
        "PRO" -> AccountTier.PRO
        "TEAM" -> AccountTier.TEAM
        else -> AccountTier.FREE
    }
}
