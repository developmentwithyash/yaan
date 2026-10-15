package com.yaan.core.data.repository

import com.yaan.core.common.dispatcher.CoroutineDispatchers
import com.yaan.core.common.result.YaanResult
import com.yaan.core.data.crypto.InMemoryVaultKeyManager
import com.yaan.core.domain.model.AccountTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserSessionRepositoryTest {

    private val testDispatchers = object : CoroutineDispatchers {
        override val main = Dispatchers.Unconfined
        override val io = Dispatchers.Unconfined
        override val default = Dispatchers.Unconfined
        override val unconfined = Dispatchers.Unconfined
    }

    @Test
    fun `test default profile and profile updating`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        val initial = repo.getCurrentProfile()
        assertEquals("Elena Rostova", initial.displayName)
        assertEquals(AccountTier.PRO, initial.tier)
        assertTrue(initial.biometricLockEnabled)

        // Update profile
        val updateRes = repo.updateProfile("Elena R. Senior", "elena@yaan.ai")
        assertTrue(updateRes is YaanResult.Success)
        val updated = repo.getCurrentProfile()
        assertEquals("Elena R. Senior", updated.displayName)
        assertEquals("elena@yaan.ai", updated.email)
    }

    @Test
    fun `test biometric vault lock and unlock lifecycle`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        repo.lockVault()
        assertTrue(repo.getCurrentProfile().isVaultLocked)

        val unlockRes = repo.unlockVault()
        assertTrue(unlockRes is YaanResult.Success)
        assertFalse(repo.getCurrentProfile().isVaultLocked)

        repo.setBiometricLock(false)
        assertFalse(repo.getCurrentProfile().biometricLockEnabled)
    }

    @Test
    fun `test passkey registration and qr device pairing`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        // Passkey registration
        val passkeyRes = repo.registerPasskey()
        assertTrue(passkeyRes is YaanResult.Success)
        assertTrue(repo.getCurrentProfile().passkeyRegistered)

        // QR Device Pairing
        val pairRes = repo.pairDeviceWithQr("yaan_pair_qr_token_super_secret_999")
        assertTrue(pairRes is YaanResult.Success)
        assertEquals(3, (pairRes as YaanResult.Success).data)
        assertEquals(3, repo.getCurrentProfile().pairedDevicesCount)

        // Malformed QR payload rejected
        val badPairRes = repo.pairDeviceWithQr("too_short")
        assertTrue(badPairRes is YaanResult.Failure)
    }

    @Test
    fun `test logout and crypto-shredding clears keys and resets session`() = runTest {
        val keyManager = InMemoryVaultKeyManager()
        val repo = DefaultUserSessionRepository(keyManager, testDispatchers)

        val initialKey = keyManager.getOrCreateMasterKey()
        assertTrue(initialKey.any { it != 0.toByte() })

        val logoutRes = repo.logoutAndCryptoShred()
        assertTrue(logoutRes is YaanResult.Success)

        val session = repo.getCurrentProfile()
        assertEquals("Anonymous User", session.displayName)
        assertEquals("anonymous:revoked", session.authToken)
        assertTrue(session.isVaultLocked)
    }
}
