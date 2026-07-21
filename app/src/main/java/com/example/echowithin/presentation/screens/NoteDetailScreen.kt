package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.PushPin
import com.example.echowithin.ui.theme.ErrorRed
import androidx.compose.material.icons.filled.Sync
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.MoreVert
import com.example.echowithin.data.repository.NoteImportExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

/**
 * Escapes note content so it is safe to interpolate into a JS template
 * literal: `renderContent(`$escaped`, $isDark)`. Backslashes, backticks and
 * `$` are the three characters that would otherwise break out of the
 * template literal. Kept as a top-level helper so the WebView `factory`
 * and `update` paths share one implementation.
 */
private fun escapeJsTemplateLiteral(text: String): String = text
    .replace("\\", "\\\\")
    .replace("`", "\\`")
    .replace("$", "\\$")


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
    onTogglePin: ((Boolean) -> Unit) -> Unit,
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
    val coroutineScope = rememberCoroutineScope()
    var noteState by remember { mutableStateOf(initialNote) }
    var isLoading by remember { mutableStateOf(initialNote == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val singleExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null && noteState != null) {
            val note = noteState!!
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val content = NoteImportExportHelper.exportToMarkdown(note)
                            outputStream.write(content.toByteArray(Charsets.UTF_8))
                        }
                    }
                    android.widget.Toast.makeText(context, "Note exported successfully", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
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
                    IconButton(
                        onClick = {
                            onTogglePin { newPinned ->
                                noteState = noteState?.copy(isPinned = newPinned)
                                android.widget.Toast.makeText(
                                    context,
                                    if (newPinned) "Note pinned to top" else "Note unpinned",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (noteState?.isPinned == true) "Unpin Note" else "Pin Note",
                            tint = if (noteState?.isPinned == true) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as Markdown") },
                                onClick = {
                                    menuExpanded = false
                                    val defaultName = (noteState?.title ?: "note").replace(Regex("[\\\\/:*?\"<>|]"), "_").trim() + ".md"
                                    try {
                                        singleExportLauncher.launch(defaultName)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        // Fallback: write to cache and trigger share sheet
                                        try {
                                            val note = noteState
                                            if (note != null) {
                                                val cacheFile = java.io.File(context.cacheDir, defaultName)
                                                val content = com.example.echowithin.data.repository.NoteImportExportHelper.exportToMarkdown(note)
                                                cacheFile.writeText(content, Charsets.UTF_8)
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    cacheFile
                                                )
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/markdown"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "Share note via"))
                                            }
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                            android.widget.Toast.makeText(context, "Export failed: ${ex.message}", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                            if (noteState?.sourceNoteId != null) {
                                DropdownMenuItem(
                                    text = { Text("Sync with original") },
                                    onClick = {
                                        menuExpanded = false
                                        isSyncing = true
                                        onSyncClick { msg ->
                                            isSyncing = false
                                            android.widget.Toast.makeText(context, msg ?: "Sync failed", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
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
            // Outer Box lets us stack a scrollable reading surface with a
            // sticky bottom action bar so the user can always reach Edit,
            // Share, Copy, History, Lock, etc. no matter how far they have
            // scrolled through the note.
            // Outer Box lets us stack a scrollable reading surface with a
            // sticky bottom action bar so the user can always reach Edit,
            // Share, Copy, History, Lock, etc. no matter how far they have
            // scrolled through the note.
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 96.dp),
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

                // ── WebView markdown/math renderer ──
                // Renders the note through marked.js + KaTeX inside an
                // Android WebView. A JavaScript bridge reports the
                // rendered content height so we can resize the WebView
                // to exactly match — no internal scrolling, the outer
                // Compose verticalScroll handles everything.

                val isDark = isSystemInDarkTheme()
                val contentText = note?.content.orEmpty()
                // Density for converting CSS-px → dp
                val density = androidx.compose.ui.platform.LocalDensity.current
                // Holds the measured HTML content height in dp
                var webContentHeight by remember { mutableStateOf(200.dp) }
                // Tracks what we last injected so we only re-render on real changes
                val injected = remember { mutableStateOf("" to false) }

                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        object : android.webkit.WebView(ctx) {
                            // Let the parent Compose scroll always win
                            override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
                                val result = super.onTouchEvent(event)
                                parent?.requestDisallowInterceptTouchEvent(false)
                                return result
                            }
                        }.apply {
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)

                            settings.apply {
                                javaScriptEnabled = true
                                allowFileAccess = true
                                domStorageEnabled = true
                                // Do NOT set useWideViewPort / loadWithOverviewMode —
                                // they force a wide virtual viewport (980px) which
                                // squashes content into a narrow left-aligned column.
                                layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                                isVerticalScrollBarEnabled = false
                                isHorizontalScrollBarEnabled = false
                                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                            }

                            // JS → Kotlin bridge for reporting rendered content height
                            @android.annotation.SuppressLint("JavascriptInterface")
                            class HeightBridge {
                                @android.webkit.JavascriptInterface
                                fun onContentHeight(heightPx: Int) {
                                    post {
                                        webContentHeight = heightPx.dp + 32.dp  // small bottom padding
                                    }
                                }
                            }
                            addJavascriptInterface(HeightBridge(), "AndroidBridge")

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    val (text, dark) = injected.value
                                    if (text.isNotEmpty()) {
                                        val escaped = escapeJsTemplateLiteral(text)
                                        view?.evaluateJavascript("renderContent(`$escaped`, $dark)", null)
                                    }
                                }
                            }
                            loadUrl("file:///android_asset/katex/math_renderer.html")
                        }
                    },
                    update = { webView ->
                        val last = injected.value
                        if (last.first != contentText || last.second != isDark) {
                            injected.value = contentText to isDark
                            val escaped = escapeJsTemplateLiteral(contentText)
                            webView.evaluateJavascript("renderContent(`$escaped`, $isDark)", null)
                        }
                    },
                    onRelease = { webView ->
                        webView.apply {
                            stopLoading()
                            removeJavascriptInterface("AndroidBridge")
                            loadUrl("about:blank")
                            clearHistory()
                            removeAllViews()
                            destroy()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(webContentHeight)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )

                }  // close inner Column

                // ── Sticky bottom action bar ──
                // The reading surface scrolls freely above. A bottom-anchored
                // surface always shows the four primary actions (Edit / Copy
                // / Share / Lock) so the user can reach them no matter how
                // far they have scrolled into the note. Lock used to be in
                // the inline row where it was easy to miss — promoting it
                // here makes the protection affordance discoverable and
                // also makes the lock icon change (open/closed) immediately
                // visible as the user toggles it.
                // ── Bottom Action Bar ──
                // Clean icon toolbar matching the app's Material 3 design
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit
                        ActionBarItem(
                            icon = Icons.Default.Edit,
                            label = "Edit",
                            tint = com.example.echowithin.ui.theme.BrandOrange,
                            onClick = onEdit
                        )
                        // Copy
                        ActionBarItem(
                            icon = Icons.Default.ContentCopy,
                            label = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                val rawText = noteState?.content.orEmpty()
                                if (rawText.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(rawText))
                                    android.widget.Toast.makeText(context, "Note copied", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        // Share
                        ActionBarItem(
                            icon = Icons.Default.Share,
                            label = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                if (com.example.echowithin.data.network.SessionManager.token.isNullOrBlank()) {
                                    android.widget.Toast.makeText(context, "Sign in to share notes", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onShare()
                                }
                            }
                        )
                        // Lock
                        ActionBarItem(
                            icon = if (noteState?.isLocked == true) Icons.Default.Lock else Icons.Default.LockOpen,
                            label = if (noteState?.isLocked == true) "Locked" else "Lock",
                            tint = if (noteState?.isLocked == true) com.example.echowithin.ui.theme.BrandOrange
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                val isGuest = com.example.echowithin.data.network.SessionManager.token.isNullOrBlank()
                                val isFree = com.example.echowithin.data.network.SessionManager.accountTier == "free"
                                if (isGuest || isFree) {
                                    android.widget.Toast.makeText(context, "Upgrade to Premium to lock notes!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onToggleLock { newLocked ->
                                        noteState = noteState?.copy(isLocked = newLocked)
                                        android.widget.Toast.makeText(
                                            context,
                                            if (newLocked) "Note locked" else "Note unlocked",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                        // History
                        ActionBarItem(
                            icon = Icons.Default.History,
                            label = "History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                if (com.example.echowithin.data.network.SessionManager.token.isNullOrBlank()) {
                                    android.widget.Toast.makeText(context, "Sign in to view versions", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onVersions()
                                }
                            }
                        )
                        // Delete
                        ActionBarItem(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            tint = ErrorRed,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }
            }  // close outer Box
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
        var inMathBlock = false
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

            // Toggle math block state on $$ fences
            if (line.trim() == "$$") {
                if (!inMathBlock) {
                    inMathBlock = true
                    i++
                    continue
                } else {
                    inMathBlock = false
                    i++
                    continue
                }
            }

            // Inside math block — render with serif math styling
            if (inMathBlock) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = primaryColor,
                        fontSize = 17.sp
                    )
                )
                append("    ") // Indentation
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
            // Check for task list (checkbox) items - [ ] or - [x]
            else if (line.trimStart().let { it.startsWith("- [ ] ") || it.startsWith("- [x] ") || it.startsWith("- [X] ") || it.startsWith("* [ ] ") || it.startsWith("* [x] ") || it.startsWith("* [X] ") }) {
                isList = true
                val trimmed = line.trimStart()
                val isChecked = trimmed[3] == 'x' || trimmed[3] == 'X'
                val content = trimmed.substring(6)
                val checkbox = if (isChecked) "☑ " else "☐ "
                append("  $checkbox")
                if (isChecked) {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = secondaryColor.copy(alpha = 0.6f)))
                    appendInlineFormatting(content, primaryColor, secondaryColor)
                    pop()
                } else {
                    appendInlineFormatting(content, primaryColor, secondaryColor)
                }
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
    val escapable = setOf('\\', '`', '*', '_', '{', '}', '[', ']', '(', ')', '#', '+', '-', '.', '!', '~', '$')
    while (i < text.length) {
        // Backslash escaping
        if (text[i] == '\\' && i + 1 < text.length) {
            val nextChar = text[i + 1]
            if (nextChar in escapable) {
                append(nextChar.toString())
                i += 2
                continue
            }
        }
        // Single line block math $$equation$$
        if (i + 1 < text.length && text[i] == '$' && text[i + 1] == '$') {
            val endIdx = text.indexOf("$$", i + 2)
            if (endIdx != -1) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = primaryColor,
                        fontSize = 17.sp
                    )
                )
                append(text.substring(i + 2, endIdx))
                pop()
                i = endIdx + 2
                continue
            }
        }

        // Inline math $equation$
        if (text[i] == '$') {
            val endIdx = text.indexOf('$', i + 1)
            if (endIdx != -1 && endIdx > i + 1) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        color = primaryColor
                    )
                )
                append(text.substring(i + 1, endIdx))
                pop()
                i = endIdx + 1
                continue
            }
        }
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

        // Image ![alt](url) — render as descriptive text
        if (text[i] == '!' && i + 1 < text.length && text[i + 1] == '[') {
            val imgMatch = Regex("!\\[([^]]*)]\\(([^)]+)\\)").find(text, i)
            if (imgMatch != null && imgMatch.range.first == i) {
                val altText = imgMatch.groupValues[1].ifBlank { "image" }
                pushStyle(
                    SpanStyle(
                        color = secondaryColor,
                        fontStyle = FontStyle.Italic
                    )
                )
                append("[📷 $altText]")
                pop()
                i = imgMatch.range.last + 1
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

/**
 * Compact action bar item: icon + label stacked vertically.
 * Used in the NoteDetailScreen bottom toolbar.
 */
@Composable
private fun ActionBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = tint,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
