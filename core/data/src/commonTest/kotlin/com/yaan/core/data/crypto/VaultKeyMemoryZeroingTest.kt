package com.yaan.core.data.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VaultKeyMemoryZeroingTest {

    @Test
    fun testByteArrayZeroizeSetsAllBytesToZero() {
        val sensitiveData = ByteArray(32) { (it + 1).toByte() }
        assertTrue(sensitiveData.any { it != 0.toByte() })

        sensitiveData.zeroize()
        assertTrue(sensitiveData.all { it == 0.toByte() }, "All bytes must be zeroized")
    }

    @Test
    fun testUseZeroizedWipesBufferOnNormalCompletion() {
        val key = ByteArray(32) { 0x5A.toByte() }
        val result = key.useZeroized { buffer ->
            assertEquals(0x5A.toByte(), buffer[0])
            "processed"
        }
        assertEquals("processed", result)
        assertTrue(key.all { it == 0.toByte() }, "Buffer must be zeroized after useZeroized")
    }

    @Test
    fun testUseZeroizedWipesBufferOnException() {
        val key = ByteArray(32) { 0x3C.toByte() }
        assertFailsWith<IllegalStateException> {
            key.useZeroized {
                throw IllegalStateException("Simulated crypto failure")
            }
        }
        assertTrue(key.all { it == 0.toByte() }, "Buffer must be zeroized even when exception thrown")
    }

    @Test
    fun testInMemoryVaultKeyManagerShredZeroizesKey() {
        val initialKey = ByteArray(32) { 0x7E.toByte() }
        val manager = InMemoryVaultKeyManager(initialKey)
        val retrievedKey = manager.getOrCreateMasterKey()
        assertEquals(0x7E.toByte(), retrievedKey[0])

        manager.shredKey()
        assertTrue(initialKey.all { it == 0.toByte() }, "Original key array must be zeroized on shred")
        assertTrue(retrievedKey.all { it == 0.toByte() }, "Retrieved reference must be zeroized on shred")
    }
}
