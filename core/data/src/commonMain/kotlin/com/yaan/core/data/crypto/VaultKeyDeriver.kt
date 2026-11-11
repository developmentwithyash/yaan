package com.yaan.core.data.crypto

/**
 * Multiplatform contract for PBKDF2 key derivation, SHA-256 integrity hashing,
 * and secure cryptographic randomness.
 */
expect object VaultKeyDeriver {
    /**
     * Derives a cryptographic key using PBKDF2-HMAC-SHA256 with the specified salt and iterations.
     */
    fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int = 10_000,
        keyLengthBytes: Int = 32
    ): ByteArray

    /**
     * Computes the SHA-256 digest of [data] and returns it as a lowercase hexadecimal string.
     */
    fun sha256Hex(data: ByteArray): String

    /**
     * Generates cryptographically secure random bytes of [lengthBytes].
     */
    fun generateRandomBytes(lengthBytes: Int): ByteArray
}
