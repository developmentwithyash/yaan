package com.yaan.core.data.crypto

import com.yaan.core.common.result.YaanResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CipherTest {

    @Test
    fun testAesGcmEncryptionAndDecryptionRoundTrip() = runTest {
        val cipher = AesGcmYaanCipher()
        val key = ByteArray(32) { (it + 1).toByte() }
        val plainText = "Executive note: Confidential architecture blueprint for YAAN 2026."

        val encryptedResult = cipher.encrypt(plainText, key)
        assertTrue(encryptedResult is YaanResult.Success)
        val cipherBytes = encryptedResult.data
        assertTrue(cipherBytes.size > 12)

        val decryptedResult = cipher.decrypt(cipherBytes, key)
        assertTrue(decryptedResult is YaanResult.Success)
        assertEquals(plainText, decryptedResult.data)
    }

    @Test
    fun testDecryptionFailsWithWrongKeyOrCorruptedData() = runTest {
        val cipher = AesGcmYaanCipher()
        val key1 = ByteArray(32) { 0x01 }
        val key2 = ByteArray(32) { 0x02 }
        val plainText = "Secret biometric escrow credentials"

        val encryptedResult = cipher.encrypt(plainText, key1)
        assertTrue(encryptedResult is YaanResult.Success)

        // Try decrypt with wrong key
        val wrongKeyResult = cipher.decrypt(encryptedResult.data, key2)
        // Should either fail with error or not equal plaintext
        if (wrongKeyResult is YaanResult.Success) {
            assertTrue(wrongKeyResult.data != plainText)
        } else {
            assertTrue(wrongKeyResult is YaanResult.Failure)
        }

        // Try decrypt with truncated payload
        val truncatedResult = cipher.decrypt(ByteArray(4), key1)
        assertTrue(truncatedResult is YaanResult.Failure)
    }
}
