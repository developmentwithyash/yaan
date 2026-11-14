package com.yaan.core.domain.validation

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteValidatorTest {

    @Test
    fun testValidNotePasses() {
        val result = NoteValidator.validateNote(
            title = "Quarterly Architecture Review",
            content = "Detailed plan for multiplatform state management."
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun testBlankTitleFailsValidation() {
        val resultBlank = NoteValidator.validateTitle("   ")
        assertTrue(resultBlank.isFailure)
        val error = (resultBlank as YaanResult.Failure).error
        assertTrue(error is YaanError.ValidationError)
        assertEquals("title", error.field)
        assertTrue(error.message.contains("must not be blank", ignoreCase = true))

        val resultEmpty = NoteValidator.validateTitle("")
        assertTrue(resultEmpty.isFailure)
    }

    @Test
    fun testTitleExceedingMaxLengthFailsValidation() {
        val valid255 = "A".repeat(255)
        assertTrue(NoteValidator.validateTitle(valid255).isSuccess)

        val invalid256 = "A".repeat(256)
        val result = NoteValidator.validateTitle(invalid256)
        assertTrue(result.isFailure)
        val error = (result as YaanResult.Failure).error
        assertTrue(error is YaanError.ValidationError)
        assertEquals("title", error.field)
        assertTrue(error.message.contains("cannot exceed 255 characters", ignoreCase = true))
    }

    @Test
    fun testContentExceedingMaxLengthFailsValidation() {
        val valid100k = "B".repeat(100_000)
        assertTrue(NoteValidator.validateContent(valid100k).isSuccess)

        val invalid100kPlus1 = "B".repeat(100_001)
        val result = NoteValidator.validateContent(invalid100kPlus1)
        assertTrue(result.isFailure)
        val error = (result as YaanResult.Failure).error
        assertTrue(error is YaanError.ValidationError)
        assertEquals("content", error.field)
        assertTrue(error.message.contains("cannot exceed 100000 characters", ignoreCase = true))
    }

    @Test
    fun testValidateNoteShortCircuitsOnTitleError() {
        val result = NoteValidator.validateNote(
            title = "",
            content = "B".repeat(100_001)
        )
        assertTrue(result.isFailure)
        val error = (result as YaanResult.Failure).error as YaanError.ValidationError
        assertEquals("title", error.field)
    }
}
