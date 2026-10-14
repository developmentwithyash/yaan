package com.yaan.core.data.crypto

import com.yaan.core.common.result.YaanResult

/**
 * Interface contract for zero-knowledge AES-GCM-256 authenticated encryption.
 */
interface YaanCipher {
    suspend fun encrypt(plainText: String, key: ByteArray): YaanResult<ByteArray>
    suspend fun decrypt(cipherText: ByteArray, key: ByteArray): YaanResult<String>
}
