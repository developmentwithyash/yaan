package com.yaan.feature.capture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuickCaptureParserTest {

    @Test
    fun testDetectsHttpsAndHttpUrlsWithHosts() {
        val input = "Read https://kotlinlang.org/docs/multiplatform.html and http://localhost:8080/health later"
        val previews = QuickCaptureParser.extractUrlPreviews(input)

        assertEquals(2, previews.size)
        assertEquals("https://kotlinlang.org/docs/multiplatform.html", previews[0].url)
        assertEquals("kotlinlang.org", previews[0].host)
        assertEquals("https", previews[0].scheme)

        assertEquals("http://localhost:8080/health", previews[1].url)
        assertEquals("localhost", previews[1].host)
        assertEquals("http", previews[1].scheme)
    }

    @Test
    fun testHandlesNonStandardQueryStringsAndFragments() {
        val input = "Debug relay: https://api.yaan.dev/v1/sync?token=abc_123&filter[]=notes&q=hello+world%21#section-2."
        val previews = QuickCaptureParser.extractUrlPreviews(input)

        assertEquals(1, previews.size)
        assertEquals(
            "https://api.yaan.dev/v1/sync?token=abc_123&filter[]=notes&q=hello+world%21#section-2",
            previews[0].url
        )
        assertEquals("api.yaan.dev", previews[0].host)
    }

    @Test
    fun testStripsTrailingPunctuationAndDeduplicatesUrls() {
        val input = "Check (https://github.com/developmentwithyash/yaan), also https://github.com/developmentwithyash/yaan!"
        val previews = QuickCaptureParser.extractUrlPreviews(input)

        assertEquals(1, previews.size)
        assertEquals("https://github.com/developmentwithyash/yaan", previews[0].url)
        assertEquals("github.com", previews[0].host)
    }

    @Test
    fun testIgnoresPlainTextWithoutSupportedSchemes() {
        val input = "Draft meeting notes for Q4 roadmap and ftp://legacy.internal/file"
        val previews = QuickCaptureParser.extractUrlPreviews(input)

        assertTrue(previews.isEmpty())
    }
}
