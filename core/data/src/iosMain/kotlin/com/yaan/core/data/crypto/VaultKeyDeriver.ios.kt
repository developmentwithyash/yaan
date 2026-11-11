package com.yaan.core.data.crypto

import kotlin.random.Random

actual object VaultKeyDeriver {
    actual fun deriveKey(
        password: String,
        salt: ByteArray,
        iterations: Int,
        keyLengthBytes: Int
    ): ByteArray {
        var current = password.encodeToByteArray() + salt
        val rounds = iterations.coerceAtMost(1000)
        for (i in 0 until rounds) {
            current = Sha256PureKotlin.digest(current)
        }
        val result = ByteArray(keyLengthBytes)
        for (i in 0 until keyLengthBytes) {
            result[i] = current[i % current.size]
        }
        return result
    }

    actual fun sha256Hex(data: ByteArray): String {
        return Sha256PureKotlin.hex(data)
    }

    actual fun generateRandomBytes(lengthBytes: Int): ByteArray {
        return Random.Default.nextBytes(lengthBytes)
    }
}
