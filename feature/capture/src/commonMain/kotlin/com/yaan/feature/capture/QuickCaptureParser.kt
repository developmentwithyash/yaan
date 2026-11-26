package com.yaan.feature.capture

/**
 * Lightweight metadata extracted for an auto-detected HTTP/HTTPS URL in quick capture text.
 */
data class UrlPreviewMetadata(
    val url: String,
    val host: String,
    val scheme: String,
)

/**
 * Pure-Kotlin parser that detects web URLs in quick capture text and constructs preview metadata.
 */
object QuickCaptureParser {

    private val URL_REGEX = Regex(
        pattern = """(?i)\bhttps?://[^\s<>"{}|\\^`]+""",
    )

    private val TRAILING_PUNCTUATION = setOf('.', ',', ';', ':', '!', '?', ')', ']', '}')

    fun extractUrlPreviews(text: String): List<UrlPreviewMetadata> {
        if (text.isBlank()) return emptyList()

        val seen = linkedSetOf<String>()
        val results = mutableListOf<UrlPreviewMetadata>()

        for (match in URL_REGEX.findAll(text)) {
            val cleaned = trimTrailingPunctuation(match.value)
            if (cleaned.isEmpty() || !seen.add(cleaned)) continue

            val preview = parseMetadata(cleaned) ?: continue
            results.add(preview)
        }

        return results
    }

    private fun trimTrailingPunctuation(raw: String): String {
        var end = raw.length
        while (end > 0 && raw[end - 1] in TRAILING_PUNCTUATION) {
            end--
        }
        return raw.substring(0, end)
    }

    private fun parseMetadata(url: String): UrlPreviewMetadata? {
        val schemeDelimiter = url.indexOf("://")
        if (schemeDelimiter <= 0) return null

        val scheme = url.substring(0, schemeDelimiter).lowercase()
        if (scheme != "http" && scheme != "https") return null

        val afterScheme = url.substring(schemeDelimiter + 3)
        val authorityEnd = afterScheme.indexOfAny(charArrayOf('/', '?', '#')).let {
            if (it == -1) afterScheme.length else it
        }
        val authority = afterScheme.substring(0, authorityEnd)
        if (authority.isBlank()) return null

        val withoutUserInfo = authority.substringAfterLast('@')
        val host = if (withoutUserInfo.startsWith('[')) {
            withoutUserInfo.substringAfter('[').substringBefore(']')
        } else {
            withoutUserInfo.substringBefore(':')
        }.lowercase()

        if (host.isBlank()) return null

        return UrlPreviewMetadata(
            url = url,
            host = host,
            scheme = scheme,
        )
    }
}
