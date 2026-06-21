package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echowithin.data.model.AppNote
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import com.example.echowithin.ui.theme.ErrorRed
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

// Pre-compiled regex for markdown preview rendering
private val HEADING_REGEX_EDITOR = Regex("^(#+)\\s+(.*)$")
private val BLOCKQUOTE_REGEX_EDITOR = Regex("^\\s*>\\s*(.*)")
private val LINK_REGEX_EDITOR = Regex("\\[(.*?)\\]\\(.*?\\)")
private val STRIKETHROUGH_REGEX_EDITOR = Regex("~~(.*?)~~")

// Limits are dynamic: Guest (no token) -> Unlimited; Sync users -> Free is 20K, Premium is 100K.
private fun getMaxCharLimit(): Int {
    val hasToken = !com.example.echowithin.data.network.SessionManager.token.isNullOrBlank() && com.example.echowithin.data.network.SessionManager.token != "null"
    if (!hasToken) return Int.MAX_VALUE
    val isFree = com.example.echowithin.data.network.SessionManager.accountTier == "free"
    return if (isFree) 20_000 else 100_000
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    initialNote: AppNote?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: (content: String, reference: String, tags: List<String>) -> Unit,
    // Lock integration
    isLocked: Boolean,
    lockError: String?,
    lockLoading: Boolean,
    onVerifyPin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var contentField by remember { mutableStateOf(TextFieldValue(initialNote?.content.orEmpty())) }
    var reference by remember { mutableStateOf(initialNote?.reference.orEmpty()) }
    var tags by remember { mutableStateOf(initialNote?.tags?.joinToString(",").orEmpty()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val content = contentField.text
    val hasChanges = content.isNotBlank()
    val tabTitles = listOf("Write", "Preview")
    val wordCount = remember(content) {
        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
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
                            tint = if (hasChanges && !isSaving) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.ime)
    ) { innerPadding ->
        if (initialNote?.isLocked == true && isLocked) {
            // Locked note — PIN verification
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
                        text = "Enter your PIN to edit this protected note.",
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
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        )
                    }
                }

                val limit = remember { getMaxCharLimit() }
                when (selectedTab) {
                    0 -> WriteTab(
                        contentField = contentField,
                        onContentChange = {
                            if (it.text.length <= limit) {
                                contentField = it
                            }
                        },
                        reference = reference,
                        onReferenceChange = { reference = it },
                        tags = tags,
                        onTagsChange = { tags = it },
                        charCount = content.length,
                        wordCount = wordCount,
                        maxChars = limit
                    )
                    1 -> PreviewTab(content = content)
                }
            }
        }
    }
}

@Composable
private fun WriteTab(
    contentField: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    reference: String,
    onReferenceChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    charCount: Int,
    wordCount: Int,
    maxChars: Int
) {
    var isDetailsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().imePadding()
    ) {
        // Main editor wrapper that handles keyboard padding and fills available space
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Typing area: fills maximum height and scrolls internally
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                TextField(
                    value = contentField,
                    onValueChange = onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Write your thoughts...") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )

                // Word count / Character counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$wordCount words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (maxChars == Int.MAX_VALUE) "$charCount" else "$charCount / ${"%,d".format(maxChars)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            maxChars == Int.MAX_VALUE -> MaterialTheme.colorScheme.onSurfaceVariant
                            charCount > (maxChars * 0.9).toInt() -> ErrorRed
                            charCount > (maxChars * 0.75).toInt() -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Collapsible details panel (Reference & Tags)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDetailsExpanded = !isDetailsExpanded }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Details (Reference & Tags)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (isDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isDetailsExpanded) "Collapse details" else "Expand details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (isDetailsExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
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
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            )
                        )

                        TextField(
                            value = tags,
                            onValueChange = onTagsChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Tags, comma separated",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            )
                        )
                    }
                }
            }
        }

        // ── Markdown Formatting Toolbar (bottom-anchored, Notesnook-style) ──
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
        MarkdownToolbar(
            onInsert = { prefix, suffix ->
                val text = contentField.text
                val selection = contentField.selection
                val selectedText = if (selection.length > 0) text.substring(selection.start, selection.end) else ""

                val newText = if (selectedText.isNotEmpty()) {
                    text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
                } else {
                    text.substring(0, selection.start) + prefix + suffix + text.substring(selection.start)
                }

                val newCursorPos = if (selectedText.isNotEmpty()) {
                    selection.start + prefix.length + selectedText.length + suffix.length
                } else {
                    selection.start + prefix.length
                }

                onContentChange(TextFieldValue(newText, TextRange(newCursorPos)))
            }
        )
    }
}

@Composable
private fun MarkdownToolbar(
    onInsert: (prefix: String, suffix: String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Bold
            IconButton(onClick = { onInsert("**", "**") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            // Italic
            IconButton(onClick = { onInsert("*", "*") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            // Strikethrough
            IconButton(onClick = { onInsert("~~", "~~") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strikethrough", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            // Heading
            IconButton(onClick = { onInsert("## ", "") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Title, contentDescription = "Heading", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            // Code
            IconButton(onClick = { onInsert("`", "`") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Code, contentDescription = "Code", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            // Quote
            IconButton(onClick = { onInsert("> ", "") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            // List
            IconButton(onClick = { onInsert("- ", "") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "List", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
            // Link
            IconButton(onClick = { onInsert("[", "](url)") }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Link, contentDescription = "Link", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PreviewTab(content: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (content.isBlank()) {
            Text(
                text = "Nothing to preview",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        } else {
            // Memoize the markdown render on `content` so it's only
            // recomputed when the text actually changes, not on every
            // recomposition of the Preview tab (e.g. focus / config
            // changes that don't touch the content).
            val rendered = remember(content) { renderMarkdownEditor(content) }
            Text(
                text = rendered,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Markdown rendering helpers ──────────────────────────────────────────────────

private fun renderMarkdownEditor(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.split("\n")
        lines.forEachIndexed { index, line ->
            var isHeading = false
            var isBlockquote = false

            if (line.startsWith("#")) {
                val headingMatch = HEADING_REGEX_EDITOR.find(line)
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
                            fontWeight = FontWeight.Bold,
                            fontSize = (18 * sizeMultiplier).sp
                        )
                    )
                    appendInlineEditor(contentText)
                    pop()
                }
            } else if (line.trimStart().startsWith(">")) {
                isBlockquote = true
                val contentText = line.replaceFirst(Regex("^\\s*>\\s*"), "")
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append("▎ ")
                appendInlineEditor(contentText)
                pop()
            }

            if (!isHeading && !isBlockquote) {
                appendInlineEditor(line)
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}

private fun AnnotatedString.Builder.appendInlineEditor(text: String) {
    var i = 0
    while (i < text.length) {
        // Monospace Code `code`
        if (text[i] == '`') {
            val endIdx = text.indexOf('`', i + 1)
            if (endIdx != -1) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x1A4CAF50)
                    )
                )
                append(text.substring(i + 1, endIdx))
                pop()
                i = endIdx + 1
                continue
            }
        }
        // Strikethrough ~~text~~
        if (i + 1 < text.length && text[i] == '~' && text[i+1] == '~') {
            val endIdx = text.indexOf("~~", i + 2)
            if (endIdx != -1) {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendInlineEditor(text.substring(i + 2, endIdx))
                pop()
                i = endIdx + 2
                continue
            }
        }
        // Bold **bold** or __bold__
        if (i + 1 < text.length && text[i] == '*' && text[i+1] == '*') {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendInlineEditor(text.substring(i + 2, endIdx))
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
                appendInlineEditor(text.substring(i + 1, endIdx))
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
