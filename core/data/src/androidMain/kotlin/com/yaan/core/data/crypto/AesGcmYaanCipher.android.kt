package com.yaan.core.data.crypto

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual class AesGcmYaanCipher actual constructor() : YaanCipher {
    private val secureRandom = SecureRandom()

    actual override fun encryptSync(plainText: String, key: ByteArray): YaanResult<ByteArray> {
        return try {
            val iv = ByteArray(12)
            secureRandom.nextBytes(iv)
            val secretKey = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val cipherText = cipher.doFinal(plainText.encodeToByteArray())

            val result = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(cipherText, 0, result, iv.size, cipherText.size)
            YaanResult.Success(result)
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.CryptoError("AES-GCM encryption failed: ${e.message}"))
        }
    }

    actual override fun decryptSync(cipherText: ByteArray, key: ByteArray): YaanResult<String> {
        return try {
            if (cipherText.size < 12 + 16) {
                return YaanResult.Failure(YaanError.CryptoError("Invalid ciphertext: payload too short"))
            }
            val iv = ByteArray(12)
            System.arraycopy(cipherText, 0, iv, 0, 12)
            val actualCipher = ByteArray(cipherText.size - 12)
            System.arraycopy(cipherText, 12, actualCipher, 0, actualCipher.size)

            val secretKey = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(actualCipher)
            YaanResult.Success(plainBytes.decodeToString())
        } catch (e: Exception) {
            YaanResult.Failure(YaanError.CryptoError("AES-GCM decryption failed: ${e.message}"))
        }
    }

    actual override suspend fun encrypt(plainText: String, key: ByteArray): YaanResult<ByteArray> {
        return encryptSync(plainText, key)
    }

    actual override suspend fun decrypt(cipherText: ByteArray, key: ByteArray): YaanResult<String> {
        return decryptSync(cipherText, key)
    }
}
