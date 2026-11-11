package com.yaan.core.domain.security

/**
 * Hardware and enrollment status of the biometric subsystem.
 */
enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    LOCKED_OUT
}

/**
 * Result of an individual biometric authentication prompt invocation.
 */
enum class BiometricResult {
    SUCCESS,
    REJECTED,
    CANCELLED,
    FAILED,
    FALLBACK_REQUESTED
}

/**
 * High-level domain outcome of attempting to unlock the vault.
 */
sealed interface BiometricUnlockOutcome {
    data object Unlocked : BiometricUnlockOutcome
    data object FallbackToPasswordRequired : BiometricUnlockOutcome
    data object Cancelled : BiometricUnlockOutcome
    data class Failed(val reason: String) : BiometricUnlockOutcome
}

/**
 * Platform-independent abstraction for biometric hardware authentication.
 * Wraps Android BiometricPrompt, iOS LocalAuthentication, and JVM fallback.
 */
interface BiometricAuthenticator {
    fun canAuthenticate(): BiometricStatus
    suspend fun authenticate(title: String, subtitle: String? = null): BiometricResult
}

/**
 * High-level coordinator enforcing the zero-knowledge biometric unlock flow.
 * Ensures:
 * 1. Checks hardware and enrollment availability before prompting.
 * 2. Falls back cleanly to master password if biometrics are not enrolled or available.
 * 3. Never unlocks unless explicit affirmative biometric confirmation is returned.
 */
class BiometricVaultUnlockCoordinator(
    private val authenticator: BiometricAuthenticator,
    private val onUnlockSuccess: suspend () -> Unit
) {
    suspend fun attemptUnlock(
        title: String = "Unlock YAAN Secure Vault",
        subtitle: String? = "Verify your identity to decrypt your local memory"
    ): BiometricUnlockOutcome {
        val status = authenticator.canAuthenticate()
        if (status != BiometricStatus.AVAILABLE) {
            return BiometricUnlockOutcome.FallbackToPasswordRequired
        }

        return when (authenticator.authenticate(title, subtitle)) {
            BiometricResult.SUCCESS -> {
                onUnlockSuccess()
                BiometricUnlockOutcome.Unlocked
            }
            BiometricResult.FALLBACK_REQUESTED -> BiometricUnlockOutcome.FallbackToPasswordRequired
            BiometricResult.CANCELLED -> BiometricUnlockOutcome.Cancelled
            BiometricResult.REJECTED -> BiometricUnlockOutcome.Failed("Biometric verification rejected")
            BiometricResult.FAILED -> BiometricUnlockOutcome.Failed("Biometric sensor error")
        }
    }
}
