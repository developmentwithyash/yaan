package com.yaan.core.domain.repository

import com.yaan.core.common.result.YaanResult
import com.yaan.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract for managing user authentication, local session state,
 * biometric vault locking, and cross-device pairing.
 */
interface UserSessionRepository {
    fun observeProfile(): Flow<UserProfile>
    fun getCurrentProfile(): UserProfile

    suspend fun updateProfile(displayName: String, email: String?): YaanResult<UserProfile>
    suspend fun setBiometricLock(enabled: Boolean): YaanResult<Unit>
    suspend fun unlockVault(): YaanResult<Unit>
    fun lockVault()

    suspend fun registerPasskey(): YaanResult<Unit>
    suspend fun pairDeviceWithQr(qrCodePayload: String): YaanResult<Int>
    suspend fun completeOnboarding(displayName: String, ageConfirmed18Plus: Boolean): YaanResult<UserProfile>
    suspend fun logoutAndCryptoShred(): YaanResult<Unit>
}
