package com.yaan.core.domain.validation

import com.yaan.core.common.result.YaanError
import com.yaan.core.common.result.YaanResult

/**
 * Domain validator for Note titles and content payloads.
 * Satisfies Issue #61: Prevents blank titles, titles > 255 chars, and content > 100,000 chars.
 */
object NoteValidator {
    const val MAX_TITLE_LENGTH = 255
    const val MAX_CONTENT_LENGTH = 100_000

    fun validateTitle(title: String): YaanResult<Unit> {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            return YaanResult.Failure(
                YaanError.ValidationError(
                    field = "title",
                    message = "Note title must not be blank"
                )
            )
        }
        if (title.length > MAX_TITLE_LENGTH) {
            return YaanResult.Failure(
                YaanError.ValidationError(
                    field = "title",
                    message = "Note title cannot exceed $MAX_TITLE_LENGTH characters (got ${title.length})"
                )
            )
        }
        return YaanResult.Success(Unit)
    }

    fun validateContent(content: String): YaanResult<Unit> {
        if (content.length > MAX_CONTENT_LENGTH) {
            return YaanResult.Failure(
                YaanError.ValidationError(
                    field = "content",
                    message = "Note content cannot exceed $MAX_CONTENT_LENGTH characters (got ${content.length})"
                )
            )
        }
        return YaanResult.Success(Unit)
    }

    fun validateNote(title: String, content: String): YaanResult<Unit> {
        val titleRes = validateTitle(title)
        if (titleRes is YaanResult.Failure) return titleRes

        val contentRes = validateContent(content)
        if (contentRes is YaanResult.Failure) return contentRes

        return YaanResult.Success(Unit)
    }
}
