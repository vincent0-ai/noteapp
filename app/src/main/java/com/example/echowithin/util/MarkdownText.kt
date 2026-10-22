package com.example.echowithin.util

/**
 * Pure-Kotlin helpers that derive plain text, a note title, and a preview
 * from raw Markdown source. This is the single source of truth for
 * "strip the markdown and show me the words" — previously three separate,
 * subtly different implementations lived in HomeScreen, SearchScreen, and
 * NotesRepository (which didn't strip at all), producing the "**Title** as
 * title" and "preview repeats the title" bugs.
 *
 * No Android dependencies, so it is unit-testable on the host JVM.
 */
object MarkdownText {

    const val DEFAULT_TITLE_MAX = 60
    const val DEFAULT_PREVIEW_MAX = 140

    // Block-level markers, anchored to the start of a line.
    private val HEADING = Regex("^\\s{0,3}#{1,6}\\s+")
    private val BLOCKQUOTE = Regex("^\\s{0,3}(?:>\\s*)+")
    private val TASK_LIST = Regex("^\\s{0,3}[-*+]\\s+\\[[ xX]]\\s+")
    private val BULLET_LIST = Regex("^\\s{0,3}[-*+]\\s+")
    private val ORDERED_LIST = Regex("^\\s{0,3}\\d{1,9}[.)]\\s+")
    private val CODE_FENCE = Regex("^\\s{0,3}```")
    private val LINK = Regex("\\[([^]]*)]\\([^)]*\\)")
    private val IMAGE = Regex("!\\[([^]]*)]\\([^)]*\\)")

    /**
     * The note title: the first non-empty line, stripped of all Markdown
     * markers and trimmed. Falls back to "Untitled" for blank input.
     */
    fun titleFrom(markdown: String, maxChars: Int = DEFAULT_TITLE_MAX): String {
        val line = markdown.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
        val plain = line?.let { stripBlocks(it).trim() }.orEmpty()
        return plain.ifBlank { "Untitled" }.take(maxChars)
    }

    /**
     * A short plain-text preview of the body, EXCLUDING the title line.
     */
    fun previewFrom(markdown: String, maxChars: Int = DEFAULT_PREVIEW_MAX): String {
        val lines = markdown.lines()
        val titleIndex = lines.indexOfFirst { it.isNotBlank() }
        if (titleIndex == -1) return ""
        val sb = StringBuilder(maxChars)
        var inCodeBlock = false
        for (i in (titleIndex + 1) until lines.size) {
            val line = lines[i]
            if (CODE_FENCE.containsMatchIn(line)) {
                inCodeBlock = !inCodeBlock
                continue
            }
            val plain = if (inCodeBlock) line else stripBlocks(line).trim()
            if (plain.isEmpty()) continue
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(plain)
            if (sb.length >= maxChars) break
        }
        return sb.toString().trim().take(maxChars)
    }

    /**
     * Full plain-text rendering (newline-joined), used for avatars, search
     * snippets, and anywhere we need "just the words" of a note.
     */
    fun plainText(markdown: String): String {
        val sb = StringBuilder()
        var inCodeBlock = false
        for (line in markdown.lines()) {
            if (CODE_FENCE.containsMatchIn(line)) {
                inCodeBlock = !inCodeBlock
                continue
            }
            val plain = if (inCodeBlock) line else stripBlocks(line).trim()
            if (plain.isEmpty()) continue
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(plain)
        }
        return sb.toString()
    }

    /** Strips a line's leading block marker, then its inline markers. */
    private fun stripBlocks(line: String): String {
        var s = line
        s = s.replaceFirst(HEADING, "")
        s = s.replaceFirst(BLOCKQUOTE, "")
        s = s.replaceFirst(TASK_LIST, "")
        s = s.replaceFirst(BULLET_LIST, "")
        s = s.replaceFirst(ORDERED_LIST, "")
        return stripInline(s)
    }

    /** Strips inline emphasis, code, links, images, and escapes. */
    private fun stripInline(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '\\' && i + 1 < text.length -> {
                    sb.append(text[i + 1]); i += 2
                }
                c == '`' -> {
                    i++ // skip opening backtick, keep the code content as text
                    while (i < text.length && text[i] != '`') {
                        sb.append(text[i]); i++
                    }
                    if (i < text.length) i++ // skip closing backtick
                }
                c == '~' && i + 1 < text.length && text[i + 1] == '~' -> i += 2
                c == '*' && i + 1 < text.length && text[i + 1] == '*' -> i += 2
                c == '_' && i + 1 < text.length && text[i + 1] == '_' -> i += 2
                c == '*' || c == '_' -> i++
                c == '!' && i + 1 < text.length && text[i + 1] == '[' -> {
                    val m = IMAGE.find(text, i)
                    if (m != null && m.range.first == i) {
                        sb.append(m.groupValues[1].ifBlank { "image" })
                        i = m.range.last + 1
                    } else i++
                }
                c == '[' -> {
                    val m = LINK.find(text, i)
                    if (m != null && m.range.first == i) {
                        sb.append(m.groupValues[1])
                        i = m.range.last + 1
                    } else if (i + 2 < text.length &&
                        (text.startsWith("[ ]", i) || text.startsWith("[x]", i) || text.startsWith("[X]", i))
                    ) {
                        i += 3
                    } else i++
                }
                else -> {
                    sb.append(c); i++
                }
            }
        }
        return sb.toString()
    }
}
