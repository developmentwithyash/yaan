package com.yaan.app.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopClipboardDetectorTest {

    @Test
    fun testDetectsNewClipboardTextWhenEnabled() {
        val detector = DesktopClipboardDetector(isAutoDetectionEnabled = true)
        val text = "Review Q4 cloud architecture and database performance"

        val prompt = detector.onWindowFocusGained(text)
        assertNotNull(prompt)
        assertEquals(text, prompt.fullContent)
        assertEquals(text, prompt.previewText)
        assertTrue(prompt.contentHash.isNotEmpty())
    }

    @Test
    fun testTruncatesLongPreviewText() {
        val detector = DesktopClipboardDetector(isAutoDetectionEnabled = true)
        val longText = "A".repeat(120)

        val prompt = detector.onWindowFocusGained(longText)
        assertNotNull(prompt)
        assertEquals(83, prompt.previewText.length) // 80 chars + "..."
        assertTrue(prompt.previewText.endsWith("..."))
    }

    @Test
    fun testIgnoresWhenAutoDetectionDisabled() {
        val detector = DesktopClipboardDetector(isAutoDetectionEnabled = false)
        val prompt = detector.onWindowFocusGained("Some secret copied text")
        assertNull(prompt)
    }

    @Test
    fun testIgnoresBlankOrNullClipboard() {
        val detector = DesktopClipboardDetector(isAutoDetectionEnabled = true)
        assertNull(detector.onWindowFocusGained(null))
        assertNull(detector.onWindowFocusGained(""))
        assertNull(detector.onWindowFocusGained("   \n\t"))
    }

    @Test
    fun testDeduplicatesSuccessiveFocusWithSameContent() {
        val detector = DesktopClipboardDetector(isAutoDetectionEnabled = true)
        val text = "Meeting notes from investor presentation"

        val prompt1 = detector.onWindowFocusGained(text)
        assertNotNull(prompt1)

        // Mark as captured/dismissed
        detector.markCaptured(prompt1.contentHash)

        // Second focus with same text should return null
        val prompt2 = detector.onWindowFocusGained(text)
        assertNull(prompt2)

        // New text should trigger prompt again
        val prompt3 = detector.onWindowFocusGained("Different copied text")
        assertNotNull(prompt3)
    }
}
