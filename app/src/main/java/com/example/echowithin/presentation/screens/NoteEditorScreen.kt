package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echowithin.data.model.AppNote
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import com.example.echowithin.ui.theme.BrandAmber
import com.example.echowithin.ui.theme.BrandOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    initialNote: AppNote?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (content: String, reference: String, tags: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf(initialNote?.content.orEmpty()) }
    var reference by remember { mutableStateOf(initialNote?.reference.orEmpty()) }
    var tags by remember { mutableStateOf(initialNote?.tags?.joinToString(",").orEmpty()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val hasChanges = content.isNotBlank()
    val tabTitles = listOf("Write", "Preview")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { EchoWithinTopBarTitle() },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSave(
                                content,
                                reference,
                                tags.split(',').map { it.trim() }.filter { it.isNotBlank() }
                            )
                        },
                        enabled = hasChanges && !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Save Note",
                            tint = if (hasChanges && !isSaving) BrandOrange
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = BrandOrange
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) BrandOrange
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> WriteTab(
                    content = content,
                    onContentChange = { content = it },
                    reference = reference,
                    onReferenceChange = { reference = it },
                    tags = tags,
                    onTagsChange = { tags = it }
                )
                1 -> PreviewTab(content = content)
            }
        }
    }
}

@Composable
private fun WriteTab(
    content: String,
    onContentChange: (String) -> Unit,
    reference: String,
    onReferenceChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main content field – largest input
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Write your thoughts...") },
            minLines = 12,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = BrandOrange
            )
        )

        // Compact reference field
        OutlinedTextField(
            value = reference,
            onValueChange = onReferenceChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Reference link (optional)",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedLabelColor = BrandOrange
            )
        )

        // Compact tags field
        OutlinedTextField(
            value = tags,
            onValueChange = onTagsChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            placeholder = {
                Text(
                    "Tags, comma separated",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                focusedLabelColor = BrandOrange
            )
        )
    }
}

@Composable
private fun PreviewTab(content: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            if (content.isBlank()) {
                Text(
                    text = "Nothing to preview",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            } else {
                Text(
                    text = renderMarkdown(content),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ── Markdown rendering helpers ──────────────────────────────────────────────────

private fun renderMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        lines.forEachIndexed { index, line ->
            var isHeading = false
            var isBlockquote = false

            if (line.startsWith("#")) {
                val headingMatch = Regex("^(#+)\\s+(.*)$").find(line)
                if (headingMatch != null) {
                    isHeading = true
                    val level = headingMatch.groupValues[1].length
                    val contentText = headingMatch.groupValues[2]
                    val sizeMultiplier = when (level) {
                        1 -> 1.3f
                        2 -> 1.2f
                        else -> 1.1f
                    }
                    pushStyle(
                        SpanStyle(
                            color = BrandOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = (18 * sizeMultiplier).sp
                        )
                    )
                    append(contentText)
                    pop()
                }
            } else if (line.trimStart().startsWith(">")) {
                isBlockquote = true
                val contentText = line.replaceFirst(Regex("^\\s*>\\s*"), "")
                pushStyle(SpanStyle(color = BrandAmber, fontStyle = FontStyle.Italic))
                append("▎ ")
                appendInline(contentText)
                pop()
            }

            if (!isHeading && !isBlockquote) {
                appendInline(line)
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}

private fun AnnotatedString.Builder.appendInline(text: String) {
    var i = 0
    while (i < text.length) {
        if (text[i] == '`') {
            val endIdx = text.indexOf('`', i + 1)
            if (endIdx != -1) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = BrandAmber,
                        background = Color(0x1AFFB000)
                    )
                )
                append(text.substring(i + 1, endIdx))
                pop()
                i = endIdx + 1
                continue
            }
        }
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInline(text.substring(i + 2, endIdx))
                pop()
                i = endIdx + 2
                continue
            }
        }
        if (text[i] == '*') {
            val endIdx = text.indexOf('*', i + 1)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInline(text.substring(i + 1, endIdx))
                pop()
                i = endIdx + 1
                continue
            }
        }
        append(text[i].toString())
        i++
    }
}
