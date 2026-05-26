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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import com.example.echowithin.ui.theme.ErrorRed
import androidx.compose.material.icons.filled.Sync

// Pre-compiled regex patterns for markdown rendering
private val headingRegex = Regex("^(#+)\\s+(.*)$")
private val blockquoteRegex = Regex("^\\s*>\\s*")
private val unorderedListRegex = Regex("^\\s*[-*]\\s+(.*)")
private val orderedListRegex = Regex("^\\s*(\\d+)\\.\\s+(.*)")
private val horizontalRuleRegex = Regex("^\\s*(---+|\\*\\*\\*+)\\s*$")
private val codeBlockFenceRegex = Regex("^```")
private val boldDoubleAsteriskRegex = Regex("\\*\\*(.+?)\\*\\*")
private val boldDoubleUnderscoreRegex = Regex("__(.+?)__")
private val strikethroughRegex = Regex("~~(.+?)~~")
private val inlineCodeRegex = Regex("`([^`]+)`")
private val linkRegex = Regex("\\[([^]]+)]\\(([^)]+)\\)")
private val italicAsteriskRegex = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
private val italicUnderscoreRegex = Regex("(?<=\\s|^)_(?!_)(.+?)(?<!_)_(?=\\s|$|[.,;:!?])")


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
    onSyncClick: (onDone: (String?) -> Unit) -> Unit,
    // Lock integration
    isLocked: Boolean,
    lockError: String?,
    lockLoading: Boolean,
    onVerifyPin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var noteState by remember { mutableStateOf(initialNote) }
    var isLoading by remember { mutableStateOf(initialNote == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

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
                            tint = MaterialTheme.colorScheme.primary
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
        if (noteState?.isLocked == true && isLocked) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                var pin by remember { mutableStateOf("") }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ErrorRed.copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, ErrorRed),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(32.dp))
                        }
                    }
                    Text(
                        text = "Locked Note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Enter your PIN to view this protected note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                if (pin.length == 4) {
                                    onVerifyPin(pin)
                                }
                            }
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.width(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 8.sp
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    if (lockError != null) {
                        Text(
                            text = lockError,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { if (pin.length == 4) onVerifyPin(pin) },
                        enabled = pin.length == 4 && !lockLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    ) {
                        if (lockLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Unlock", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                // Update Available Banner (Sync Now)
                if (noteState?.updateAvailable == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "Update Available",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "The original author updated this note. Sync to get the changes.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    isSyncing = true
                                    onSyncClick { msg ->
                                        isSyncing = false
                                        android.widget.Toast.makeText(context, msg ?: "Sync failed", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Sync Now", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
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

                // Tags Display — minimal inline text style
                if (note?.tags?.isNotEmpty() == true) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        note.tags.forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Reference Info
                if (!note?.reference.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Reference Link",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
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
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val displayContent = remember(note, primaryColor, secondaryColor) {
                    val fullContent = note?.content.orEmpty()
                    if (fullContent.isBlank()) {
                        buildAnnotatedString { append("No content available") }
                    } else {
                        renderMarkdown(fullContent, primaryColor, secondaryColor)
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
                    if (noteState?.sourceNoteId != null) {
                        IconButton(
                            onClick = {
                                isSyncing = true
                                onSyncClick { msg ->
                                    isSyncing = false
                                    android.widget.Toast.makeText(context, msg ?: "Sync failed", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync with original",
                                    tint = if (noteState?.updateAvailable == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        if (com.example.echowithin.data.network.SessionManager.token.isNullOrBlank()) {
                            android.widget.Toast.makeText(context, "Sign in or create an account to share notes!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onShare()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
                    // Copy content to clipboard
                    IconButton(onClick = {
                        val rawText = noteState?.content.orEmpty()
                        if (rawText.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(rawText))
                            android.widget.Toast.makeText(context, "Note copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        if (com.example.echowithin.data.network.SessionManager.token.isNullOrBlank()) {
                            android.widget.Toast.makeText(context, "Sign in or create an account to view versions!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onVersions()
                        }
                    }) {
                        Icon(Icons.Default.History, contentDescription = "Versions", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = {
                        val isGuest = com.example.echowithin.data.network.SessionManager.token.isNullOrBlank()
                        val isFree = com.example.echowithin.data.network.SessionManager.accountTier == "free"
                        if (isGuest || isFree) {
                            android.widget.Toast.makeText(context, "Upgrade to Premium to lock notes!", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onToggleLock { newLocked ->
                                noteState = noteState?.copy(isLocked = newLocked)
                            }
                        }
                    }) {
                        Icon(
                            if (noteState?.isLocked == true) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Lock",
                            tint = if (noteState?.isLocked == true) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
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

private fun renderMarkdown(
    markdown: String,
    primaryColor: Color,
    secondaryColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        var inCodeBlock = false
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // Toggle code block state on ``` fences
            if (codeBlockFenceRegex.containsMatchIn(line)) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    i++
                    continue
                } else {
                    inCodeBlock = false
                    i++
                    continue
                }
            }

            // Inside code block — render as monospace, no formatting
            if (inCodeBlock) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = secondaryColor,
                        background = secondaryColor.copy(alpha = 0.08f)
                    )
                )
                append(line)
                pop()
                if (i < lines.size - 1) append("\n")
                i++
                continue
            }

            // Horizontal rule
            if (horizontalRuleRegex.matches(line)) {
                append("─".repeat(30))
                if (i < lines.size - 1) append("\n")
                i++
                continue
            }

            // Empty line → extra spacing
            if (line.isBlank()) {
                append("\n")
                if (i < lines.size - 1) append("\n")
                i++
                continue
            }

            var isHeading = false
            var isBlockquote = false
            var isList = false

            // Check for Headings
            if (line.startsWith("#")) {
                val headingMatch = headingRegex.find(line)
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
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = (18 * sizeMultiplier).sp
                        )
                    )
                    appendInlineFormatting(contentText, primaryColor, secondaryColor)
                    pop()
                }
            }
            // Check for Blockquote
            else if (line.trimStart().startsWith(">")) {
                isBlockquote = true
                val contentText = blockquoteRegex.replaceFirst(line, "")
                pushStyle(
                    SpanStyle(
                        color = secondaryColor,
                        fontStyle = FontStyle.Italic
                    )
                )
                append("▎ ") // Visual bar indicator
                appendInlineFormatting(contentText, primaryColor, secondaryColor)
                pop()
            }
            // Check for unordered list
            else if (unorderedListRegex.matches(line)) {
                isList = true
                val match = unorderedListRegex.find(line)!!
                val content = match.groupValues[1]
                append("  • ")
                appendInlineFormatting(content, primaryColor, secondaryColor)
            }
            // Check for ordered list
            else if (orderedListRegex.matches(line)) {
                isList = true
                val match = orderedListRegex.find(line)!!
                val number = match.groupValues[1]
                val content = match.groupValues[2]
                append("  $number. ")
                appendInlineFormatting(content, primaryColor, secondaryColor)
            }

            if (!isHeading && !isBlockquote && !isList) {
                appendInlineFormatting(line, primaryColor, secondaryColor)
            }

            if (i < lines.size - 1) {
                append("\n")
            }
            i++
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatting(
    text: String,
    primaryColor: Color,
    secondaryColor: Color
) {
    var i = 0
    while (i < text.length) {
        // Strikethrough ~~text~~
        if (i + 1 < text.length && text[i] == '~' && text[i + 1] == '~') {
            val endIdx = text.indexOf("~~", i + 2)
            if (endIdx != -1) {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendInlineFormatting(text.substring(i + 2, endIdx), primaryColor, secondaryColor)
                pop()
                i = endIdx + 2
                continue
            }
        }

        // Markdown link [text](url) — render text only
        if (text[i] == '[') {
            val linkMatch = linkRegex.find(text, i)
            if (linkMatch != null && linkMatch.range.first == i) {
                val linkText = linkMatch.groupValues[1]
                pushStyle(
                    SpanStyle(
                        color = primaryColor,
                        textDecoration = TextDecoration.Underline
                    )
                )
                append(linkText)
                pop()
                i = linkMatch.range.last + 1
                continue
            }
        }

        // Monospace Code `code`
        if (text[i] == '`') {
            val endIdx = text.indexOf('`', i + 1)
            if (endIdx != -1) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = secondaryColor,
                        background = secondaryColor.copy(alpha = 0.10f)
                    )
                )
                append(text.substring(i + 1, endIdx))
                pop()
                i = endIdx + 1
                continue
            }
        }

        // Bold **bold**
        if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInlineFormatting(text.substring(i + 2, endIdx), primaryColor, secondaryColor)
                pop()
                i = endIdx + 2
                continue
            }
        }
        // Bold __bold__
        if (i + 1 < text.length && text[i] == '_' && text[i + 1] == '_') {
            val endIdx = text.indexOf("__", i + 2)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInlineFormatting(text.substring(i + 2, endIdx), primaryColor, secondaryColor)
                pop()
                i = endIdx + 2
                continue
            }
        }

        // Italic *italic*
        if (text[i] == '*') {
            val endIdx = text.indexOf('*', i + 1)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendInlineFormatting(text.substring(i + 1, endIdx), primaryColor, secondaryColor)
                pop()
                i = endIdx + 1
                continue
            }
        }
        // Italic _italic_ — only at word boundaries (not inside variable_names)
        if (text[i] == '_') {
            val atWordBoundary = (i == 0 || text[i - 1].isWhitespace() || text[i - 1] in ".,;:!?\"'(")
            if (atWordBoundary) {
                // Find closing _ that is also at a word boundary
                var endIdx = -1
                var searchFrom = i + 1
                while (searchFrom < text.length) {
                    val candidate = text.indexOf('_', searchFrom)
                    if (candidate == -1) break
                    val afterBoundary = (candidate == text.length - 1 || text[candidate + 1].isWhitespace() || text[candidate + 1] in ".,;:!?\"')")
                    if (afterBoundary) {
                        endIdx = candidate
                        break
                    }
                    searchFrom = candidate + 1
                }
                if (endIdx != -1 && endIdx > i + 1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    appendInlineFormatting(text.substring(i + 1, endIdx), primaryColor, secondaryColor)
                    pop()
                    i = endIdx + 1
                    continue
                }
            }
        }

        // Regular character
        append(text[i].toString())
        i++
    }
}
