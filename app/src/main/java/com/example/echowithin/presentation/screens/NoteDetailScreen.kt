package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.echowithin.data.model.AppNote
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber
import com.example.echowithin.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    initialNote: AppNote?,
    loadNote: suspend (String) -> Result<AppNote>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onVersions: () -> Unit,
    onDelete: () -> Unit,
    onToggleLock: ((Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var noteState by remember { mutableStateOf(initialNote) }
    var isLoading by remember { mutableStateOf(initialNote == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(noteId, initialNote) {
        if (initialNote != null) {
            noteState = initialNote
            isLoading = false
        } else {
            isLoading = true
            loadNote(noteId)
                .onSuccess {
                    noteState = it
                    isLoading = false
                }
                .onFailure {
                    errorMessage = it.message ?: "Failed to load note"
                    isLoading = false
                }
        }
    }

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
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Note",
                            tint = BrandOrange
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandOrange)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "An error occurred",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val note = noteState
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date & Metadata info row
                note?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Updated: ${it.updatedAt.take(16)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tags Display
                if (note?.tags?.isNotEmpty() == true) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        note.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("#$tag", style = MaterialTheme.typography.bodySmall) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = BrandAmber,
                                    containerColor = BrandOrange.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Reference Info
                if (!note?.reference.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BrandAmber.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Reference Link",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandAmber
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note?.reference.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Content - render full content as markdown (no first-line dropping)
                val displayContent = remember(note) {
                    val fullContent = note?.content.orEmpty()
                    if (fullContent.isBlank()) {
                        buildAnnotatedString { append("No content available") }
                    } else {
                        renderMarkdown(fullContent)
                    }
                }

                // Note Content Panel
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = displayContent,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = BrandOrange)
                    }
                    IconButton(onClick = onVersions) {
                        Icon(Icons.Default.History, contentDescription = "Versions", tint = BrandAmber)
                    }
                    IconButton(onClick = {
                        onToggleLock { newLocked ->
                            noteState = noteState?.copy(isLocked = newLocked)
                        }
                    }) {
                        Icon(
                            if (noteState?.isLocked == true) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Lock",
                            tint = if (noteState?.isLocked == true) BrandAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note?") },
            text = { Text("This action cannot be undone. Are you sure?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun renderMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        lines.forEachIndexed { index, line ->
            var isHeading = false
            var isBlockquote = false
            
            // Check for Headings
            if (line.startsWith("#")) {
                val headingMatch = Regex("^(#+)\\s+(.*)$").find(line)
                if (headingMatch != null) {
                    isHeading = true
                    val level = headingMatch.groupValues[1].length
                    val contentText = headingMatch.groupValues[2]
                    
                    val sizeMultiplier = when(level) {
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
            } 
            // Check for Blockquote
            else if (line.trimStart().startsWith(">")) {
                isBlockquote = true
                val contentText = line.replaceFirst(Regex("^\\s*>\\s*"), "")
                pushStyle(
                    SpanStyle(
                        color = BrandAmber,
                        fontStyle = FontStyle.Italic
                    )
                )
                append("▎ ") // Visual bar indicator
                appendInlineFormatting(contentText)
                pop()
            }
            
            if (!isHeading && !isBlockquote) {
                appendInlineFormatting(line)
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatting(text: String) {
    var i = 0
    while (i < text.length) {
        // Monospace Code `code`
        if (text[i] == '`') {
            val endIdx = text.indexOf('`', i + 1)
            if (endIdx != -1) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = BrandAmber,
                        background = Color(0x1AFFB000) // BrandAmber with 10% opacity
                    )
                )
                append(text.substring(i + 1, endIdx))
                pop()
                i = endIdx + 1
                continue
            }
        }
        
        // Bold **bold** or __bold__
        if (i + 1 < text.length && text[i] == '*' && text[i+1] == '*') {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInlineFormatting(text.substring(i + 2, endIdx))
                pop()
                i = endIdx + 2
                continue
            }
        }
        if (i + 1 < text.length && text[i] == '_' && text[i+1] == '_') {
            val endIdx = text.indexOf("__", i + 2)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInlineFormatting(text.substring(i + 2, endIdx))
                pop()
                i = endIdx + 2
                continue
            }
        }
        
        // Italic *italic* or _italic_
        if (text[i] == '*') {
            val endIdx = text.indexOf('*', i + 1)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInlineFormatting(text.substring(i + 1, endIdx))
                pop()
                i = endIdx + 1
                continue
            }
        }
        if (text[i] == '_') {
            val endIdx = text.indexOf('_', i + 1)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInlineFormatting(text.substring(i + 1, endIdx))
                pop()
                i = endIdx + 1
                continue
            }
        }
        
        // Regular character
        append(text[i].toString())
        i++
    }
}
