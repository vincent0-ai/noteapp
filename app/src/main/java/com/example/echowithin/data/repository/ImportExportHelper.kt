package com.example.echowithin.data.repository

import com.example.echowithin.data.model.AppNote
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object NoteImportExportHelper {

    data class ImportedNote(
        val title: String,
        val content: String,
        val reference: String,
        val tags: List<String>
    )

    fun exportToMarkdown(note: AppNote): String {
        val sb = StringBuilder()
        sb.append("---\n")
        sb.append("title: \"${note.title.replace("\"", "\\\"")}\"\n")
        if (note.reference.isNotBlank()) {
            sb.append("reference: \"${note.reference.replace("\"", "\\\"")}\"\n")
        }
        if (note.tags.isNotEmpty()) {
            sb.append("tags: [${note.tags.joinToString(", ") { "\"${it.replace("\"", "\\\"")}\"" }}]\n")
        }
        sb.append("updated_at: \"${note.updatedAt}\"\n")
        sb.append("---\n")
        sb.append(note.content)
        return sb.toString()
    }

    fun exportToZip(notes: List<AppNote>, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            val usedFilenames = mutableSetOf<String>()
            for (note in notes) {
                // Remove invalid characters for filenames
                val baseName = note.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "Untitled" }
                var filename = "$baseName.md"
                var counter = 1
                while (usedFilenames.contains(filename)) {
                    filename = "${baseName}_$counter.md"
                    counter++
                }
                usedFilenames.add(filename)
                val content = exportToMarkdown(note)
                zos.putNextEntry(ZipEntry(filename))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
    }

    fun parseMarkdown(text: String, defaultTitle: String): ImportedNote {
        if (text.startsWith("---")) {
            val lines = text.lines()
            var title = ""
            var reference = ""
            var tagsList = emptyList<String>()
            var contentStartIndex = -1
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line == "---") {
                    contentStartIndex = i + 1
                    break
                }
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().lowercase()
                    val value = parts[1].trim()
                    when (key) {
                        "title" -> title = value.removeSurrounding("\"").removeSurrounding("'")
                        "reference" -> reference = value.removeSurrounding("\"").removeSurrounding("'")
                        "tags" -> {
                            val cleanTags = value.removeSurrounding("[", "]").removeSurrounding("\"").removeSurrounding("'")
                            tagsList = cleanTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                    }
                }
            }
            if (contentStartIndex != -1 && contentStartIndex < lines.size) {
                val content = lines.subList(contentStartIndex, lines.size).joinToString("\n")
                return ImportedNote(
                    title = title.ifBlank { defaultTitle },
                    content = content,
                    reference = reference,
                    tags = tagsList
                )
            }
        }

        // Fallback for files without frontmatter
        val lines = text.lines()
        val firstLine = lines.firstOrNull()?.trim().orEmpty()
        val title = if (firstLine.startsWith("# ")) {
            firstLine.substring(2).trim()
        } else {
            defaultTitle
        }
        return ImportedNote(
            title = title,
            content = text,
            reference = "",
            tags = emptyList()
        )
    }

    fun importFromZip(inputStream: InputStream): List<ImportedNote> {
        val imported = mutableListOf<ImportedNote>()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (entry.name.endsWith(".md", ignoreCase = true) || entry.name.endsWith(".txt", ignoreCase = true))) {
                    val baos = ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    var len = zis.read(buffer)
                    while (len > 0) {
                        baos.write(buffer, 0, len)
                        len = zis.read(buffer)
                    }
                    val text = baos.toString("UTF-8")
                    val filename = entry.name.substringAfterLast('/').substringBeforeLast('.')
                    imported.add(parseMarkdown(text, filename))
                }
                entry = zis.nextEntry
            }
        }
        return imported
    }
}
