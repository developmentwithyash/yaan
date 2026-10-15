package com.yaan.core.data.crypto

import com.yaan.core.common.result.YaanResult

/**
 * Interface contract for zero-knowledge AES-GCM-256 authenticated encryption.
 */
interface YaanCipher {
    suspend fun encrypt(plainText: String, key: ByteArray): YaanResult<ByteArray>
    suspend fun decrypt(cipherText: ByteArray, key: ByteArray): YaanResult<String>

    fun encryptSync(plainText: String, key: ByteArray): YaanResult<ByteArray>
    fun decryptSync(cipherText: ByteArray, key: ByteArray): YaanResult<String>
}

/**
 * Platform expect declaration for hardware-backed AES-GCM-256 authenticated encryption.
 */
expect class AesGcmYaanCipher() : YaanCipher {
    override suspend fun encrypt(plainText: String, key: ByteArray): YaanResult<ByteArray>
    override suspend fun decrypt(cipherText: ByteArray, key: ByteArray): YaanResult<String>

    override fun encryptSync(plainText: String, key: ByteArray): YaanResult<ByteArray>
    override fun decryptSync(cipherText: ByteArray, key: ByteArray): YaanResult<String>
}
