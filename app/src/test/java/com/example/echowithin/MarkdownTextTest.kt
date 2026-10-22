package com.example.echowithin

import com.example.echowithin.util.MarkdownText
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTextTest {

    // ── Title ──
    @Test
    fun title_stripsHeadingAndBold() {
        assertEquals("Title", MarkdownText.titleFrom("# **Title**"))
    }

    @Test
    fun title_stripsBoldAsterisks() {
        assertEquals("Title", MarkdownText.titleFrom("**Title**"))
    }

    @Test
    fun title_stripsItalic() {
        assertEquals("Hello world", MarkdownText.titleFrom("*Hello* _world_"))
    }

    @Test
    fun title_blankFallsBackToUntitled() {
        assertEquals("Untitled", MarkdownText.titleFrom(""))
        assertEquals("Untitled", MarkdownText.titleFrom("   \n  \n"))
    }

    @Test
    fun title_takesFirstNonEmptyLine() {
        assertEquals("First", MarkdownText.titleFrom("\n\n  First  \nSecond"))
    }

    @Test
    fun title_truncatesToMax() {
        val long = "A".repeat(100)
        assertEquals("A".repeat(60), MarkdownText.titleFrom(long))
    }

    // ── Preview (excludes title line) ──
    @Test
    fun preview_excludesTitleLine() {
        val md = "My Title\n\nThis is the body content."
        assertEquals("This is the body content.", MarkdownText.previewFrom(md))
    }

    @Test
    fun preview_doesNotRepeatTitle() {
        val md = "# **My Title**\nBody text here."
        assertEquals("Body text here.", MarkdownText.previewFrom(md))
    }

    @Test
    fun preview_stripsMarkdownMarkers() {
        val md = "Title\n- item one\n- item two"
        assertEquals("item one item two", MarkdownText.previewFrom(md))
    }

    // ── List markers ──
    @Test
    fun plainText_stripsOrderedListNumbers() {
        assertEquals("first\nsecond\nthird", MarkdownText.plainText("1. first\n2. second\n3. third"))
    }

    @Test
    fun plainText_stripsTaskListMarkers() {
        assertEquals("one\ntwo", MarkdownText.plainText("- [ ] one\n- [x] two"))
    }

    @Test
    fun plainText_stripsHeadingBlockquoteAndCode() {
        assertEquals("Heading\nquote\ninline", MarkdownText.plainText("# Heading\n> quote\n`inline`"))
    }

    // ── Links & images ──
    @Test
    fun plainText_rendersLinkText() {
        assertEquals("example", MarkdownText.plainText("[example](https://example.com)"))
    }

    @Test
    fun plainText_rendersImageAlt() {
        assertEquals("photo", MarkdownText.plainText("![photo](url.png)"))
    }

    @Test
    fun plainText_handlesBackslashEscapes() {
        assertEquals("*literal*", MarkdownText.plainText("\\*literal\\*"))
    }

    @Test
    fun plainText_keepsCodeBlockContent() {
        assertEquals("val x = 1", MarkdownText.plainText("```kotlin\nval x = 1\n```"))
    }
}
