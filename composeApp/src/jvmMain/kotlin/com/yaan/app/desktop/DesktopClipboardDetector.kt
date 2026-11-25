package com.yaan.app.desktop

import java.security.MessageDigest

/**
 * Data payload representing a suggested quick-capture prompt from external clipboard.
 */
data class ClipboardCapturePrompt(
    val fullContent: String,
    val previewText: String,
    val contentHash: String
)

/**
 * Desktop clipboard detector intercepting window focus gain events to streamline
 * quick note capture workflows from external copied text.
 */
class DesktopClipboardDetector(
    var isAutoDetectionEnabled: Boolean = true,
    private val maxPreviewLength: Int = DEFAULT_MAX_PREVIEW_LENGTH
) {
    var lastCapturedContentHash: String? = null
        private set

    /**
     * Evaluates clipboard text when the desktop application window regains focus.
     */
    fun onWindowFocusGained(rawClipboardText: String?): ClipboardCapturePrompt? {
        if (!isAutoDetectionEnabled) return null
        if (rawClipboardText.isNullOrBlank()) return null

        val cleanText = rawClipboardText.trim()
        val hash = computeSha256(cleanText)

        if (hash == lastCapturedContentHash) {
            return null
        }

        val preview = if (cleanText.length > maxPreviewLength) {
            cleanText.take(maxPreviewLength) + "..."
        } else {
            cleanText
        }

        return ClipboardCapturePrompt(
            fullContent = cleanText,
            previewText = preview,
            contentHash = hash
        )
    }

    /**
     * Marks the prompt content hash as captured or dismissed to prevent re-prompting.
     */
    fun markCaptured(contentHash: String) {
        lastCapturedContentHash = contentHash
    }

    private fun computeSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_MAX_PREVIEW_LENGTH = 80
    }
}
