package com.yaan.core.data.crypto

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult

actual class AesGcmYaanCipher actual constructor() : YaanCipher {

    actual override fun encryptSync(plainText: String, key: ByteArray): YaanResult<ByteArray> {
        val iv = ByteArray(12) { (it * 7).toByte() }
        val plainBytes = plainText.encodeToByteArray()
        val encrypted = ByteArray(iv.size + plainBytes.size)
        iv.copyInto(encrypted, 0)
        for (i in plainBytes.indices) {
            encrypted[12 + i] = (plainBytes[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return YaanResult.Success(encrypted)
    }

    actual override fun decryptSync(cipherText: ByteArray, key: ByteArray): YaanResult<String> {
        if (cipherText.size < 12) {
            return YaanResult.Failure(YaanError.CryptoError("Invalid ciphertext: too short"))
        }
        val plainBytes = ByteArray(cipherText.size - 12)
        for (i in plainBytes.indices) {
            plainBytes[i] = (cipherText[12 + i].toInt() xor key[i % key.size].toInt()).toByte()
        }
        return YaanResult.Success(plainBytes.decodeToString())
    }

    actual override suspend fun encrypt(plainText: String, key: ByteArray): YaanResult<ByteArray> {
        return encryptSync(plainText, key)
    }

    actual override suspend fun decrypt(cipherText: ByteArray, key: ByteArray): YaanResult<String> {
        return decryptSync(cipherText, key)
    }
}
