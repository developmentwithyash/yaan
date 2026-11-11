package com.yaan.core.data.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

actual object VaultKeyDeriver {
    private val secureRandom = SecureRandom()

    actual fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int,
        keyLengthBytes: Int
    ): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBytes * 8)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    actual fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }

    actual fun generateRandomBytes(lengthBytes: Int): ByteArray {
        return ByteArray(lengthBytes).apply { secureRandom.nextBytes(this) }
    }
}
