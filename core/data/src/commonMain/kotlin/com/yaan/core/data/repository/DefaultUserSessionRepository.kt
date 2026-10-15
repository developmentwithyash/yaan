package com.yaan.core.data.repository

import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.crypto.VaultKeyManager
import com.yaan.core.domain.model.AccountTier
import com.yaan.core.domain.model.UserProfile
import com.yaan.core.domain.repository.UserSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * High-performance, zero-knowledge implementation of UserSessionRepository.
 * Maintains local session state, biometric gate status, and crypto-shredding hooks.
 */
class DefaultUserSessionRepository(
    private val keyManager: VaultKeyManager,
    private val dispatchers: CoroutineDispatchers,
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

    override suspend fun logoutAndCryptoShred(): YaanResult<Unit> =
        withContext(dispatchers.io) {
            // 1. Shred master encryption keys
            keyManager.shredKey()

            // 2. Reset session to locked anonymous state
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
                createdAtEpochMs = com.yaan.core.common.datetime.nowInstant().toEpochMilliseconds()
            )
            YaanResult.Success(Unit)
        }
}
