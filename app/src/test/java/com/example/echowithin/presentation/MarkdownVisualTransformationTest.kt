package com.example.echowithin.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.example.echowithin.presentation.components.MarkdownVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownVisualTransformationTest {

    private val syntaxColor = Color.Gray
    private val headingColor = Color.Blue
    private val codeBackground = Color.LightGray
    private val quoteColor = Color.Magenta

    private val transformer = MarkdownVisualTransformation(
        syntaxColor = syntaxColor,
        headingColor = headingColor,
        codeBackground = codeBackground,
        quoteColor = quoteColor
    )

    @Test
    fun `empty text returns unchanged with identity mapping`() {
        val input = AnnotatedString("")
        val result = transformer.filter(input)
        assertEquals("", result.text.text)
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun `headings are highlighted with dimmed syntax and bold text`() {
        val input = AnnotatedString("# Main Heading\nSome normal text")
        val result = transformer.filter(input)

        assertEquals(input.text, result.text.text)
        val styles = result.text.spanStyles
        assertTrue("Styles should not be empty", styles.isNotEmpty())

        // Check that marker has syntaxColor
        val markerStyle = styles.find { it.start == 0 && it.end == 2 } // "# "
        assertEquals(syntaxColor, markerStyle?.item?.color)

        // Check that heading content has bold weight and headingColor
        val contentStyle = styles.find { it.start == 2 && it.end == 14 } // "Main Heading"
        assertEquals(FontWeight.Bold, contentStyle?.item?.fontWeight)
        assertEquals(headingColor, contentStyle?.item?.color)
    }

    @Test
    fun `bold text highlights asterisks and bolds inner content`() {
        val input = AnnotatedString("Hello **world** today")
        val result = transformer.filter(input)

        assertEquals(input.text, result.text.text)
        val styles = result.text.spanStyles

        val boldContent = styles.find { it.item.fontWeight == FontWeight.Bold }
        assertEquals(8, boldContent?.start)
        assertEquals(13, boldContent?.end) // "world"
    }

    @Test
    fun `italic text highlights asterisks and italicizes inner content`() {
        val input = AnnotatedString("Hello *italic* today")
        val result = transformer.filter(input)

        assertEquals(input.text, result.text.text)
        val styles = result.text.spanStyles

        val italicContent = styles.find { it.item.fontStyle == FontStyle.Italic }
        assertEquals(7, italicContent?.start)
        assertEquals(13, italicContent?.end) // "italic"
    }

    @Test
    fun `strikethrough adds line-through decoration`() {
        val input = AnnotatedString("This is ~~deprecated~~ code")
        val result = transformer.filter(input)

        assertEquals(input.text, result.text.text)
        val styles = result.text.spanStyles

        val strikeContent = styles.find { it.item.textDecoration == TextDecoration.LineThrough }
        assertEquals(10, strikeContent?.start)
        assertEquals(20, strikeContent?.end) // "deprecated"
    }

    @Test
    fun `inline code applies monospace font family`() {
        val input = AnnotatedString("Use `val x = 42` here")
        val result = transformer.filter(input)

        assertEquals(input.text, result.text.text)
        val styles = result.text.spanStyles

        val codeContent = styles.find { it.item.fontFamily == FontFamily.Monospace }
        assertEquals(5, codeContent?.start)
        assertEquals(15, codeContent?.end) // "val x = 42"
    }

    @Test
    fun `offset mapping remains 1-to-1 identity across full length`() {
        val input = AnnotatedString("# Title\n**bold** and *italic* and `code`")
        val result = transformer.filter(input)

        for (i in 0..input.length) {
            assertEquals(i, result.offsetMapping.originalToTransformed(i))
            assertEquals(i, result.offsetMapping.transformedToOriginal(i))
        }
    }
}
