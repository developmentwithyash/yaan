package com.yaan.core.domain.security

import com.yaan.core.common.result.YaanResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiometricVaultUnlockTest {

    private class FakeBiometricAuthenticator(
        var status: BiometricStatus = BiometricStatus.AVAILABLE,
        var result: BiometricResult = BiometricResult.SUCCESS
    ) : BiometricAuthenticator {
        var authenticateCalled = false

        override fun canAuthenticate(): BiometricStatus = status

        override suspend fun authenticate(title: String, subtitle: String?): BiometricResult {
            authenticateCalled = true
            return result
        }
    }

    private class FakeVaultLockState {
        var isLocked = true
    }

    @Test
    fun testSuccessfulBiometricAuthenticationUnlocksVault() = runTest {
        val authenticator = FakeBiometricAuthenticator(
            status = BiometricStatus.AVAILABLE,
            result = BiometricResult.SUCCESS
        )
        val vaultState = FakeVaultLockState()

        val unlockFlow = BiometricVaultUnlockCoordinator(
            authenticator = authenticator,
            onUnlockSuccess = { vaultState.isLocked = false }
        )

        val outcome = unlockFlow.attemptUnlock(title = "Unlock YAAN Vault")

        assertTrue(authenticator.authenticateCalled)
        assertEquals(BiometricUnlockOutcome.Unlocked, outcome)
        assertFalse(vaultState.isLocked)
    }

    @Test
    fun testFailedBiometricAuthenticationKeepsVaultLocked() = runTest {
        val authenticator = FakeBiometricAuthenticator(
            status = BiometricStatus.AVAILABLE,
            result = BiometricResult.REJECTED
        )
        val vaultState = FakeVaultLockState()

        val unlockFlow = BiometricVaultUnlockCoordinator(
            authenticator = authenticator,
            onUnlockSuccess = { vaultState.isLocked = false }
        )

        val outcome = unlockFlow.attemptUnlock(title = "Unlock YAAN Vault")

        assertTrue(authenticator.authenticateCalled)
        assertTrue(outcome is BiometricUnlockOutcome.Failed)
        assertTrue(vaultState.isLocked)
    }

    @Test
    fun testFallbackToPasswordWhenBiometricNotEnrolled() = runTest {
        val authenticator = FakeBiometricAuthenticator(
            status = BiometricStatus.NOT_ENROLLED
        )
        val vaultState = FakeVaultLockState()

        val unlockFlow = BiometricVaultUnlockCoordinator(
            authenticator = authenticator,
            onUnlockSuccess = { vaultState.isLocked = false }
        )

        val outcome = unlockFlow.attemptUnlock(title = "Unlock YAAN Vault")

        assertFalse(authenticator.authenticateCalled)
        assertEquals(BiometricUnlockOutcome.FallbackToPasswordRequired, outcome)
        assertTrue(vaultState.isLocked)
    }

    @Test
    fun testUserCancellationDoesNotUnlock() = runTest {
        val authenticator = FakeBiometricAuthenticator(
            status = BiometricStatus.AVAILABLE,
            result = BiometricResult.CANCELLED
        )
        val vaultState = FakeVaultLockState()

        val unlockFlow = BiometricVaultUnlockCoordinator(
            authenticator = authenticator,
            onUnlockSuccess = { vaultState.isLocked = false }
        )

        val outcome = unlockFlow.attemptUnlock(title = "Unlock YAAN Vault")

        assertTrue(authenticator.authenticateCalled)
        assertEquals(BiometricUnlockOutcome.Cancelled, outcome)
        assertTrue(vaultState.isLocked)
    }
}
