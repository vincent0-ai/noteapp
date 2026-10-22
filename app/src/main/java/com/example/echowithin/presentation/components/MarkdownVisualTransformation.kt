package com.example.echowithin.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

/**
 * VisualTransformation that renders live markdown syntax highlighting and styling
 * while typing in the editor without mutating underlying plain text.
 *
 * Syntax characters (like #, *, _, ~, `) are dimmed so the user can easily see
 * the rendered effect (bold, italic, strikethrough, code, headers) in real time.
 * Because no characters are added or removed, [OffsetMapping.Identity] is used,
 * ensuring flawless cursor positioning, selection, and IME input.
 */
class MarkdownVisualTransformation(
    private val syntaxColor: Color,
    private val headingColor: Color,
    private val codeBackground: Color,
    private val quoteColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(text)

        // 1. Headings: # Heading 1 ... ###### Heading 6
        HEADING_REGEX.findAll(raw).forEach { match ->
            val markerGroup = match.groups[1]
            val contentGroup = match.groups[2]
            if (markerGroup != null) {
                builder.addStyle(
                    SpanStyle(color = syntaxColor, fontWeight = FontWeight.Normal),
                    markerGroup.range.first,
                    markerGroup.range.last + 1
                )
            }
            if (contentGroup != null) {
                builder.addStyle(
                    SpanStyle(color = headingColor, fontWeight = FontWeight.Bold),
                    contentGroup.range.first,
                    contentGroup.range.last + 1
                )
            }
        }

        // 2. Bold: **text** or __text__
        BOLD_REGEX.findAll(raw).forEach { match ->
            val fullRange = match.range
            val inner = match.groups[1] ?: match.groups[2]
            if (inner != null) {
                val prefixLen = inner.range.first - fullRange.first
                val suffixLen = fullRange.last + 1 - (inner.range.last + 1)
                builder.addStyle(
                    SpanStyle(color = syntaxColor),
                    fullRange.first,
                    fullRange.first + prefixLen
                )
                builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    inner.range.first,
                    inner.range.last + 1
                )
                builder.addStyle(
                    SpanStyle(color = syntaxColor),
                    fullRange.last + 1 - suffixLen,
                    fullRange.last + 1
                )
            }
        }

        // 3. Strikethrough: ~~text~~ (checked before italic to prevent conflict)
        STRIKE_REGEX.findAll(raw).forEach { match ->
            val fullRange = match.range
            val inner = match.groups[1]
            if (inner != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), fullRange.first, fullRange.first + 2)
                builder.addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    inner.range.first,
                    inner.range.last + 1
                )
                builder.addStyle(SpanStyle(color = syntaxColor), fullRange.last - 1, fullRange.last + 1)
            }
        }

        // 4. Italic: *text* or _text_ (excluding double asterisks/underscores)
        ITALIC_REGEX.findAll(raw).forEach { match ->
            val fullRange = match.range
            val inner = match.groups[1] ?: match.groups[2]
            if (inner != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), fullRange.first, fullRange.first + 1)
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic),
                    inner.range.first,
                    inner.range.last + 1
                )
                builder.addStyle(SpanStyle(color = syntaxColor), fullRange.last, fullRange.last + 1)
            }
        }

        // 5. Inline Code: `code`
        CODE_REGEX.findAll(raw).forEach { match ->
            val fullRange = match.range
            val inner = match.groups[1]
            if (inner != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), fullRange.first, fullRange.first + 1)
                builder.addStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground),
                    inner.range.first,
                    inner.range.last + 1
                )
                builder.addStyle(SpanStyle(color = syntaxColor), fullRange.last, fullRange.last + 1)
            }
        }

        // 6. Blockquotes: > quote line
        QUOTE_REGEX.findAll(raw).forEach { match ->
            val markerGroup = match.groups[1]
            val contentGroup = match.groups[2]
            if (markerGroup != null) {
                builder.addStyle(SpanStyle(color = syntaxColor), markerGroup.range.first, markerGroup.range.last + 1)
            }
            if (contentGroup != null) {
                builder.addStyle(
                    SpanStyle(color = quoteColor, fontStyle = FontStyle.Italic),
                    contentGroup.range.first,
                    contentGroup.range.last + 1
                )
            }
        }

        // 7. Checkboxes: - [ ] or - [x]
        TASK_REGEX.findAll(raw).forEach { match ->
            val markerGroup = match.groups[1]
            if (markerGroup != null) {
                builder.addStyle(
                    SpanStyle(color = headingColor, fontWeight = FontWeight.Bold),
                    markerGroup.range.first,
                    markerGroup.range.last + 1
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    companion object {
        private val HEADING_REGEX = Regex("""(?m)^(#{1,6}\s+)(.+)$""")
        private val BOLD_REGEX = Regex("""(?<!\*)\*\*([^*\n]+)\*\*(?!\*)|(?<!_)__([^_\n]+)__(?!_)""")
        private val ITALIC_REGEX = Regex("""(?<!\*)\*([^*\n]+)\*(?!\*)|(?<!_)_([^_\n]+)_(?!_)""")
        private val STRIKE_REGEX = Regex("""~~([^~\n]+)~~""")
        private val CODE_REGEX = Regex("""(?<!`)`([^`\n]+)`(?!`)""")
        private val QUOTE_REGEX = Regex("""(?m)^(>\s+)(.+)$""")
        private val TASK_REGEX = Regex("""(?m)^(\s*[-*]\s+\[[ xX]\]\s+)""")
    }
}

@Composable
fun rememberMarkdownVisualTransformation(): VisualTransformation {
    val syntaxColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val headingColor = MaterialTheme.colorScheme.primary
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val quoteColor = MaterialTheme.colorScheme.secondary
    return remember(syntaxColor, headingColor, codeBackground, quoteColor) {
        MarkdownVisualTransformation(
            syntaxColor = syntaxColor,
            headingColor = headingColor,
            codeBackground = codeBackground,
            quoteColor = quoteColor
        )
    }
}
