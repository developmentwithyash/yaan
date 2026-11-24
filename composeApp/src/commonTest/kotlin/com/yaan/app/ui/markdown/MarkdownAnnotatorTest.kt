package com.yaan.app.ui.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownAnnotatorTest {

    @Test
    fun testParseHeaders() {
        val markdown = """
            # Heading One
            ## Heading Two
            ### Heading Three
            Regular Paragraph
        """.trimIndent()

        val blocks = MarkdownAnnotator.parseBlocks(markdown)
        assertEquals(4, blocks.size)

        assertEquals(MarkdownElementKind.HEADER_1, blocks[0].kind)
        assertEquals("Heading One", blocks[0].cleanText)

        assertEquals(MarkdownElementKind.HEADER_2, blocks[1].kind)
        assertEquals("Heading Two", blocks[1].cleanText)

        assertEquals(MarkdownElementKind.HEADER_3, blocks[2].kind)
        assertEquals("Heading Three", blocks[2].cleanText)

        assertEquals(MarkdownElementKind.PARAGRAPH, blocks[3].kind)
        assertEquals("Regular Paragraph", blocks[3].cleanText)
    }

    @Test
    fun testParseBulletItemsAndBlockquotes() {
        val markdown = """
            - First bullet point
            * Second bullet point
            > Executive strategic quote
        """.trimIndent()

        val blocks = MarkdownAnnotator.parseBlocks(markdown)
        assertEquals(3, blocks.size)

        assertEquals(MarkdownElementKind.BULLET_ITEM, blocks[0].kind)
        assertEquals("First bullet point", blocks[0].cleanText)

        assertEquals(MarkdownElementKind.BULLET_ITEM, blocks[1].kind)
        assertEquals("Second bullet point", blocks[1].cleanText)

        assertEquals(MarkdownElementKind.BLOCKQUOTE, blocks[2].kind)
        assertEquals("Executive strategic quote", blocks[2].cleanText)
    }

    @Test
    fun testDetectMarkdownSyntax() {
        assertTrue(MarkdownAnnotator.hasMarkdownSyntax("# Title"))
        assertTrue(MarkdownAnnotator.hasMarkdownSyntax("This is **bold** text"))
        assertTrue(MarkdownAnnotator.hasMarkdownSyntax("Code: `val x = 1`"))
        assertTrue(MarkdownAnnotator.hasMarkdownSyntax("> quote"))
        assertTrue(MarkdownAnnotator.hasMarkdownSyntax("- bullet"))
        assertFalse(MarkdownAnnotator.hasMarkdownSyntax("Plain simple text with no markup."))
    }

    @Test
    fun testStripInlineFormatting() {
        val raw = "Here is **bold** text and *italic* words with `code` block."
        val stripped = MarkdownAnnotator.stripInlineMarkdown(raw)
        assertEquals("Here is bold text and italic words with code block.", stripped)
    }

    @Test
    fun testEditorModeToggle() {
        var mode = NoteEditorMode.EDIT_RAW
        mode = mode.toggle()
        assertEquals(NoteEditorMode.PREVIEW_RENDERED, mode)
        mode = mode.toggle()
        assertEquals(NoteEditorMode.EDIT_RAW, mode)
    }
}
