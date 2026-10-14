package com.yaan.core.common.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YaanResultTest {

    @Test
    fun testSuccessOperations() {
        val result: YaanResult<String> = YaanResult.Success("test-payload")
        assertTrue(result.isSuccess)
        assertEquals("test-payload", result.getOrNull())
        assertEquals("test-payload", result.getOrDefault("fallback"))

        val mapped = result.map { it.length }
        assertTrue(mapped.isSuccess)
        assertEquals(12, mapped.getOrNull())
    }

    @Test
    fun testFailureOperations() {
        val error = YaanError.StorageError("disk-full")
        val result: YaanResult<String> = YaanResult.Failure(error)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals("fallback", result.getOrDefault("fallback"))

        val folded = result.fold(
            onSuccess = { "success" },
            onFailure = { it.message }
        )
        assertEquals("disk-full", folded)
    }
}
