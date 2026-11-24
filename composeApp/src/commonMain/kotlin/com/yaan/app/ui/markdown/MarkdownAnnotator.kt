package com.yaan.app.ui.markdown

/**
 * CommonMark element categories supported in YAAN encrypted notes preview.
 */
enum class MarkdownElementKind {
    HEADER_1,
    HEADER_2,
    HEADER_3,
    BOLD,
    ITALIC,
    INLINE_CODE,
    BULLET_ITEM,
    BLOCKQUOTE,
    PARAGRAPH
}

/**
 * Editor interaction mode: raw text edit vs formatted CommonMark live preview.
 */
enum class NoteEditorMode {
    EDIT_RAW,
    PREVIEW_RENDERED;

    fun toggle(): NoteEditorMode = when (this) {
        EDIT_RAW -> PREVIEW_RENDERED
        PREVIEW_RENDERED -> EDIT_RAW
    }
}

/**
 * Structured Markdown block representation.
 */
data class MarkdownBlock(
    val kind: MarkdownElementKind,
    val rawText: String,
    val cleanText: String,
    val level: Int = 0
)

/**
 * High-performance, cross-platform CommonMark parser & annotator for YAAN notes.
 * Formats H1-H3 headers, bullet points, blockquotes, code spans, and emphasis.
 */
object MarkdownAnnotator {

    private val BOLD_REGEX = Regex("""\*\*(.*?)\*\*""")
    private val ITALIC_REGEX = Regex("""(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)""")
    private val CODE_REGEX = Regex("""`(.*?)`""")

    /**
     * Parses multiline markdown content into block-level elements.
     */
    fun parseBlocks(markdownText: String): List<MarkdownBlock> {
        if (markdownText.isBlank()) return emptyList()

        return markdownText.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null

            when {
                trimmed.startsWith("### ") -> MarkdownBlock(
                    kind = MarkdownElementKind.HEADER_3,
                    rawText = line,
                    cleanText = trimmed.removePrefix("### ").trim(),
                    level = 3
                )
                trimmed.startsWith("## ") -> MarkdownBlock(
                    kind = MarkdownElementKind.HEADER_2,
                    rawText = line,
                    cleanText = trimmed.removePrefix("## ").trim(),
                    level = 2
                )
                trimmed.startsWith("# ") -> MarkdownBlock(
                    kind = MarkdownElementKind.HEADER_1,
                    rawText = line,
                    cleanText = trimmed.removePrefix("# ").trim(),
                    level = 1
                )
                trimmed.startsWith("> ") -> MarkdownBlock(
                    kind = MarkdownElementKind.BLOCKQUOTE,
                    rawText = line,
                    cleanText = trimmed.removePrefix("> ").trim()
                )
                trimmed.startsWith("- ") -> MarkdownBlock(
                    kind = MarkdownElementKind.BULLET_ITEM,
                    rawText = line,
                    cleanText = trimmed.removePrefix("- ").trim()
                )
                trimmed.startsWith("* ") -> MarkdownBlock(
                    kind = MarkdownElementKind.BULLET_ITEM,
                    rawText = line,
                    cleanText = trimmed.removePrefix("* ").trim()
                )
                else -> MarkdownBlock(
                    kind = MarkdownElementKind.PARAGRAPH,
                    rawText = line,
                    cleanText = trimmed
                )
            }
        }
    }

    /**
     * Strips inline markdown delimiters (**bold**, *italic*, `code`) for clean display text.
     */
    fun stripInlineMarkdown(text: String): String {
        var result = BOLD_REGEX.replace(text, "$1")
        result = ITALIC_REGEX.replace(result, "$1")
        result = CODE_REGEX.replace(result, "$1")
        return result
    }

    /**
     * Detects if a text block contains CommonMark syntax tokens.
     */
    fun hasMarkdownSyntax(text: String): Boolean {
        if (text.contains(Regex("""(^|\n)\s*(#{1,3}|>|-|\*)\s+"""))) return true
        if (BOLD_REGEX.containsMatchIn(text)) return true
        if (CODE_REGEX.containsMatchIn(text)) return true
        return false
    }
}
