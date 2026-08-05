package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import com.example.echowithin.data.model.AppNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.echowithin.data.model.ProposalDto
import com.example.echowithin.data.model.ShareDto
import com.example.echowithin.data.model.NotificationDto
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import com.example.echowithin.presentation.components.ProposalReviewDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.MoreVert
import com.example.echowithin.data.repository.NoteImportExportHelper
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.foundation.background
import com.example.echowithin.ui.theme.ErrorRed
import com.example.echowithin.ui.theme.BrandOrange

// ── Pre-compiled regex patterns for stripMarkdown (performance fix) ──
private val REGEX_HEADING = Regex("(?m)^#+\\s+")
private val REGEX_BLOCKQUOTE = Regex("(?m)^[\\s*+-]*>\\s*")
private val REGEX_LIST_ITEM = Regex("(?m)^[\\s]*[*+-]\\s+")
private val REGEX_FORMATTING = Regex("\\*\\*|__|\\*|_|~~")
private val REGEX_BACKTICK = Regex("`+")
private val REGEX_LINK = Regex("\\[(.*?)\\]\\(.*?\\)")
private val REGEX_IMAGE = Regex("!\\[(.*?)\\]\\(.*?\\)")

enum class HomeTab(val title: String) {
    NOTES("Notes"),
    LOCKED("Locked"),
    ACTIVITY("Activity"),
    SHARED_LINKS("Shared Links")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<AppNote>,
    isLoading: Boolean,
    isSyncing: Boolean,
    error: String?,
    onNoteClick: (String) -> Unit,
    onNewNoteClick: () -> Unit,
    onSyncClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSyncNoteClick: (String) -> Unit,
    // Lock-related
    hasPin: Boolean,
    isLocked: Boolean,
    lockError: String?,
    lockLoading: Boolean,
    onVerifyPin: (String) -> Unit,
    onSetupPin: (String) -> Unit,
    onBiometricUnlock: () -> Unit = {},
    // Proposals (Activity tab)
    proposals: List<ProposalDto>,
    proposalsLoading: Boolean,
    onApproveProposal: (String, String, Boolean) -> Unit,
    onRejectProposal: (String, String) -> Unit,
    // Share management
    activeShares: List<Pair<AppNote, List<ShareDto>>>,
    sharesLoading: Boolean,
    onManageShares: (String) -> Unit,
    onOpenShareLink: (String) -> Unit,
    onRefreshShares: () -> Unit = {},
    // Notifications
    notifications: List<NotificationDto> = emptyList(),
    unreadNotificationsCount: Int = 0,
    onMarkAllRead: () -> Unit = {},
    markingAllRead: Boolean = false,
    // Update-related
    updateInfo: com.example.echowithin.data.network.UpdateInfo? = null,
    downloadProgress: Float? = null,
    onConfirmUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    // Offline / sync
    isOnline: Boolean = true,
    pendingSyncCount: Int = 0,
    lastSyncedAt: Long = 0L,
    // Offline mode (no auth token) - hides Activity, Shared Links, sync features
    isOfflineMode: Boolean = false,
    // Offline notes backup prompt
    onBackupOfflineNotes: (() -> Unit)? = null,
    offlineNotesCount: Int = 0,
    // Privacy info dialog for offline users
    showOfflinePrivacyDialog: Boolean = false,
    onDismissOfflinePrivacy: () -> Unit = {},
    // Navigation
    onSearchClick: () -> Unit,
    onTrashClick: () -> Unit = {},
    onImportNotes: (List<NoteImportExportHelper.ImportedNote>) -> Unit,
    onBatchDeleteNotes: (List<String>) -> Unit,
    // Folders
    folders: List<String> = emptyList(),
    filterFolder: String? = null,
    onFilterFolder: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Tabs available in offline mode: only Notes and Locked
    val availableTabs = remember(isOfflineMode) {
        if (isOfflineMode) listOf(HomeTab.NOTES, HomeTab.LOCKED)
        else HomeTab.entries.toList()
    }
    
    var activeTab by rememberSaveable { mutableStateOf(availableTabs.first()) }
    // If current tab becomes unavailable (e.g., went offline while on Activity), switch to Notes
    LaunchedEffect(activeTab, availableTabs) {
        if (activeTab !in availableTabs) activeTab = availableTabs.first()
    }

    // Always refresh active shares when the Shared Links tab becomes visible.
    // loadActiveShares() only runs once at app launch, so without this, shares
    // created/revoked after startup (or an empty state observed at launch)
    // would never appear/update until the app restarted.
    LaunchedEffect(activeTab) {
        if (activeTab == HomeTab.SHARED_LINKS) onRefreshShares()
    }

    // Memoize filtered lists to avoid creating new List instances on every recomposition
    val unlockedNotes = remember(notes) { notes.filter { !it.isLocked } }
    val lockedNotes = remember(notes) { notes.filter { it.isLocked } }

    // ── Batch Selection State ──
    var selectedNoteIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode by remember { derivedStateOf { selectedNoteIds.isNotEmpty() } }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    // Remembered LazyListStates keep each tab's scroll position when switching
    // tabs or navigating to a note and back, instead of snapping to the top.
    val notesListState = rememberSaveable(saver = LazyListState.Saver, key = "notes_tab_scroll") { LazyListState() }
    val lockedListState = rememberSaveable(saver = LazyListState.Saver, key = "locked_tab_scroll") { LazyListState() }
    val activityListState = rememberSaveable(saver = LazyListState.Saver, key = "activity_tab_scroll") { LazyListState() }
    val sharedListState = rememberSaveable(saver = LazyListState.Saver, key = "shared_tab_scroll") { LazyListState() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val imported = mutableListOf<NoteImportExportHelper.ImportedNote>()
            try {
                for (uri in uris) {
                    val type = context.contentResolver.getType(uri)
                    val filename = uri.path?.substringAfterLast('/') ?: "imported_note"
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        if (type == "application/zip" || filename.endsWith(".zip", ignoreCase = true)) {
                            imported.addAll(NoteImportExportHelper.importFromZip(inputStream))
                        } else {
                            val text = inputStream.bufferedReader().use { it.readText() }
                            imported.add(NoteImportExportHelper.parseMarkdown(text, filename.substringBeforeLast('.')))
                        }
                    }
                }
                if (imported.isNotEmpty()) {
                    onImportNotes(imported)
                } else {
                    android.widget.Toast.makeText(context, "No notes found to import", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Failed to import: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            NoteImportExportHelper.exportToZip(unlockedNotes, outputStream)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Notes exported successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    if (updateInfo != null) {
        UpdateDialog(
            versionName = updateInfo.versionName,
            changelog = updateInfo.changelog,
            downloadProgress = downloadProgress,
            onDismiss = onDismissUpdate,
            onConfirm = onConfirmUpdate
        )
    }

    // Offline notes backup prompt — show once when user logs in and has offline notes
    var showBackupPrompt by remember { mutableStateOf(false) }
    LaunchedEffect(isOnline, isOfflineMode, offlineNotesCount) {
        if (isOnline && !isOfflineMode && onBackupOfflineNotes != null && offlineNotesCount > 0) {
            // Small delay to let UI settle after login
            delay(1500)
            showBackupPrompt = true
        }
    }

    if (showBackupPrompt) {
        AlertDialog(
            onDismissRequest = { showBackupPrompt = false },
            title = { Text("Backup your offline notes?") },
            text = { Text("You have $offlineNotesCount note${if (offlineNotesCount == 1) "" else "s"} created while offline. Would you like to back them up to your account now? They'll be synced securely and available on all your devices.") },
            confirmButton = {
                TextButton(onClick = { showBackupPrompt = false; onBackupOfflineNotes?.invoke() }) {
                    Text("Back Up Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupPrompt = false }) {
                    Text("Later")
                }
            }
        )
    }

    // Offline privacy info dialog — shown once when user enters offline mode
    if (showOfflinePrivacyDialog) {
        AlertDialog(
            onDismissRequest = onDismissOfflinePrivacy,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).padding(end = 8.dp)
                    )
                    Text("Complete Privacy Mode")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "You're using EchoWithin in offline mode. Here's what that means:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🔒 Notes never leave your device", style = MaterialTheme.typography.bodyMedium)
                    Text("🔍 Full-text search works offline", style = MaterialTheme.typography.bodyMedium)
                    Text("📝 Create, edit, organize freely", style = MaterialTheme.typography.bodyMedium)
                    Text("🔐 PIN lock protects your notes", style = MaterialTheme.typography.bodyMedium)
                    Text("📤 Export notes anytime (PDF, text)", style = MaterialTheme.typography.bodyMedium)
                    Text("🚫 No account, no tracking, no ads", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign in anytime to sync your notes across devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissOfflinePrivacy) {
                    Text("Got it")
                }
            }
        )
    }

    // Batch delete confirmation dialog
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("Delete ${selectedNoteIds.size} note${if (selectedNoteIds.size != 1) "s" else ""}?") },
            text = { Text("This action cannot be undone. The selected notes will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatchDeleteDialog = false
                        onBatchDeleteNotes(selectedNoteIds.toList())
                        selectedNoteIds = emptySet()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // ── Selection Mode Top Bar ──
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selectedNoteIds = emptySet() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "${selectedNoteIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        // Select All / Deselect All toggle
                        val allSelected = selectedNoteIds.size == unlockedNotes.size && unlockedNotes.isNotEmpty()
                        IconButton(
                            onClick = {
                                selectedNoteIds = if (allSelected) emptySet()
                                else unlockedNotes.map { it.id }.toSet()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = if (allSelected) "Deselect All" else "Select All",
                                tint = if (allSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Delete selected
                        IconButton(
                            onClick = { showBatchDeleteDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    ),
                    windowInsets = WindowInsets(0.dp)
                )
            } else {
                // ── Normal Top Bar ──
                TopAppBar(
                    title = { EchoWithinTopBarTitle() },
                    actions = {
                        // Offline mode indicator
                        if (isOfflineMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Offline Mode",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        // "Last synced Xm ago" — only when we actually have a
                        // timestamp and we're online. Cheap reassurance that
                        // the device is up to date.
                        if (lastSyncedAt > 0L && isOnline && !isOfflineMode) {
                            Text(
                                text = "Synced ${formatLastSynced(lastSyncedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp).padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = onSyncClick) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Notes",
                                    tint = if (isOnline && !isOfflineMode) MaterialTheme.colorScheme.onSurface
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
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
                                    text = { Text("Trash") },
                                    onClick = {
                                        menuExpanded = false
                                        onTrashClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import Notes") },
                                    onClick = {
                                        menuExpanded = false
                                        importLauncher.launch(arrayOf("text/*", "application/zip", "application/x-zip-compressed"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export All Notes (ZIP)") },
                                    onClick = {
                                        menuExpanded = false
                                        try {
                                            exportLauncher.launch("echowithin_export.zip")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            // Fallback: write to cache and trigger share sheet
                                            try {
                                                val cacheFile = java.io.File(context.cacheDir, "echowithin_export.zip")
                                                java.io.FileOutputStream(cacheFile).use { fos ->
                                                    com.example.echowithin.data.repository.NoteImportExportHelper.exportToZip(unlockedNotes, fos)
                                                }
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    cacheFile
                                                )
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "application/zip"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "Share notes ZIP via"))
                                            } catch (ex: Exception) {
                                                ex.printStackTrace()
                                                android.widget.Toast.makeText(context, "Export failed: ${ex.message}", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    windowInsets = WindowInsets(0.dp)
                )
            }
        },
        floatingActionButton = {
            if (activeTab == HomeTab.NOTES && !isSelectionMode) {
                FloatingActionButton(
                    onClick = onNewNoteClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Note",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline banner — slides down when we lose connectivity.
            // Tapping "Retry" fires the same onSyncClick as the top bar.
            com.example.echowithin.presentation.components.OfflineBanner(
                visible = !isOnline,
                pendingChanges = pendingSyncCount,
                onRetry = onSyncClick
            )

            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = availableTabs.indexOf(activeTab),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 8.dp
            ) {
                availableTabs.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (tab == HomeTab.ACTIVITY && unreadNotificationsCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = ErrorRed,
                                        modifier = Modifier.padding(start = 2.dp)
                                    ) {
                                        Text(
                                            text = unreadNotificationsCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content
            when (activeTab) {
                HomeTab.NOTES -> {
                    Column {
                        // Folder filter chips
                        if (folders.isNotEmpty()) {
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = filterFolder == null,
                                        onClick = { onFilterFolder(null) },
                                        label = { Text("All") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandOrange,
                                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                        )
                                    )
                                }
                                items(folders.size) { index ->
                                    val folder = folders[index]
                                    FilterChip(
                                        selected = filterFolder == folder,
                                        onClick = { onFilterFolder(if (filterFolder == folder) null else folder) },
                                        label = { Text(folder) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BrandOrange,
                                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                        )
                                    )
                                }
                            }
                        }
                        NotesTabContent(
                            notes = unlockedNotes,
                            isLoading = isLoading,
                            error = error,
                            onNoteClick = onNoteClick,
                            onRetryClick = onRetryClick,
                            onSyncNoteClick = onSyncNoteClick,
                            selectedNoteIds = selectedNoteIds,
                            onToggleSelection = { noteId ->
                                selectedNoteIds = if (noteId in selectedNoteIds) {
                                    selectedNoteIds - noteId
                                } else {
                                    selectedNoteIds + noteId
                                }
                            },
                            onStartSelection = { noteId ->
                                selectedNoteIds = setOf(noteId)
                            },
                            isSelectionMode = isSelectionMode,
                            listState = notesListState
                        )
                    }
                }
                HomeTab.LOCKED -> LockedTabContent(
                    lockedNotes = lockedNotes,
                    hasPin = hasPin,
                    isLocked = isLocked,
                    lockError = lockError,
                    lockLoading = lockLoading,
                    onVerifyPin = onVerifyPin,
                    onSetupPin = onSetupPin,
                    onNoteClick = onNoteClick,
                    onSyncNoteClick = onSyncNoteClick,
                    onBiometricUnlock = onBiometricUnlock,
                    listState = lockedListState
                )
                HomeTab.ACTIVITY -> if (HomeTab.ACTIVITY in availableTabs) ActivityTabContent(
                    proposals = proposals,
                    proposalsLoading = proposalsLoading,
                    notifications = notifications,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onApproveProposal = onApproveProposal,
                    onRejectProposal = onRejectProposal,
                    onMarkAllRead = onMarkAllRead,
                    markingAllRead = markingAllRead,
                    listState = activityListState
                ) else NotesTabContent(
                    notes = unlockedNotes,
                    isLoading = isLoading,
                    error = error,
                    onNoteClick = onNoteClick,
                    onRetryClick = onRetryClick,
                    onSyncNoteClick = onSyncNoteClick
                )
                HomeTab.SHARED_LINKS -> if (HomeTab.SHARED_LINKS in availableTabs) SharedLinksTabContent(
                    activeShares = activeShares,
                    sharesLoading = sharesLoading,
                    onManageShares = onManageShares,
                    onOpenShareLink = onOpenShareLink,
                    listState = sharedListState
                ) else NotesTabContent(
                    notes = unlockedNotes,
                    isLoading = isLoading,
                    error = error,
                    onNoteClick = onNoteClick,
                    onRetryClick = onRetryClick,
                    onSyncNoteClick = onSyncNoteClick
                )
            }
        }
    }
}

// ── Notes Tab ───────────────────────────────────────────────

@Composable
private fun NotesTabContent(
    notes: List<AppNote>,
    isLoading: Boolean,
    error: String?,
    onNoteClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    onSyncNoteClick: (String) -> Unit,
    selectedNoteIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    onStartSelection: (String) -> Unit = {},
    isSelectionMode: Boolean = false,
    listState: LazyListState = rememberLazyListState()
) {
    val haptic = LocalHapticFeedback.current
    when {
        isLoading && notes.isEmpty() -> {
            val brush = shimmerBrush()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(4) {
                    NoteCardPlaceholder(brush = brush)
                }
            }
        }
        error != null && notes.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Could not load notes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRetryClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
        notes.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Unspoken thoughts start here",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the button below to add your first note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(notes, key = { it.id }, contentType = { "note_card" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelection(note.id)
                            } else {
                                onNoteClick(note.id)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStartSelection(note.id)
                            }
                        },
                        onSyncClick = { onSyncNoteClick(note.id) },
                        isSelected = note.id in selectedNoteIds,
                        isSelectionMode = isSelectionMode,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

// ── Locked Tab ──────────────────────────────────────────────

@Composable
private fun LockedTabContent(
    lockedNotes: List<AppNote>,
    hasPin: Boolean,
    isLocked: Boolean,
    lockError: String?,
    lockLoading: Boolean,
    onVerifyPin: (String) -> Unit,
    onSetupPin: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onSyncNoteClick: (String) -> Unit,
    onBiometricUnlock: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    if (!hasPin) {
        // No PIN set up — check if we're offline with a previously configured PIN
        val isOfflineConfigured = com.example.echowithin.data.network.SessionManager.localPinConfigured
        if (isOfflineConfigured) {
            // PIN was configured before but offline check returned false — show verify prompt
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
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
                        text = "Locked Notes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Enter your 4-digit PIN to view locked notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("Enter PIN") },
                        modifier = Modifier.width(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    if (lockError != null) {
                        Text(text = lockError, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                    if (lockLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Button(
                        onClick = { onVerifyPin(pin); pin = "" },
                        enabled = pin.length == 4 && !lockLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(200.dp)
                    ) {
                        Text("Unlock", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Truly no PIN set up — show setup UI
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                var pin by remember { mutableStateOf("") }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        }
                    }
                    Text(
                        text = "Set Up Security PIN",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Create a 4-digit PIN to protect sensitive notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("4-digit PIN") },
                        modifier = Modifier.width(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Button(
                        onClick = { onSetupPin(pin); pin = "" },
                        enabled = pin.length == 4 && !lockLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(200.dp)
                    ) {
                        Text("Set PIN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else if (isLocked) {
        // PIN set but locked → prompt to unlock
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
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
                    text = "Locked Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Enter your 4-digit PIN to view locked notes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("Enter PIN") },
                    modifier = Modifier.width(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                if (lockError != null) {
                    Text(text = lockError, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
                if (lockLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Button(
                    onClick = { onVerifyPin(pin); pin = "" },
                    enabled = pin.length == 4 && !lockLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("Unlock", fontWeight = FontWeight.Bold)
                }
                if (com.example.echowithin.data.local.PreferencesManager.biometricEnabled) {
                    OutlinedButton(
                        onClick = onBiometricUnlock,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(200.dp),
                        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric",
                            tint = BrandOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Fingerprint", color = BrandOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // Unlocked → show locked notes
        if (lockedNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No locked notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Lock a note from its detail page to protect it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(lockedNotes, key = { it.id }, contentType = { "note_card" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note.id) },
                        onSyncClick = { onSyncNoteClick(note.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

// ── Activity Tab (Notifications + Version Proposals) ────────────────────────

@Composable
private fun ActivityTabContent(
    proposals: List<ProposalDto>,
    proposalsLoading: Boolean,
    notifications: List<NotificationDto>,
    unreadNotificationsCount: Int,
    onApproveProposal: (String, String, Boolean) -> Unit,
    onRejectProposal: (String, String) -> Unit,
    onMarkAllRead: () -> Unit,
    markingAllRead: Boolean = false,
    listState: LazyListState = rememberLazyListState()
) {
    val hasContent = proposals.isNotEmpty() || notifications.isNotEmpty()

    when {
        proposalsLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        !hasContent -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "All quiet for now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No pending proposals or recent activity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                // Mark all read row: shows a spinner while the request is
                // in flight so the tap is never silently dropped, and
                // hides itself the moment the badge clears.
                if (unreadNotificationsCount > 0 || markingAllRead) {
                    item(key = "mark_all_read") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (markingAllRead) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Marking as read…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            } else {
                                TextButton(onClick = onMarkAllRead) {
                                    Text(
                                        text = "Mark all as read",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Proposals section
                if (proposals.isNotEmpty()) {
                    item(key = "proposals_header") {
                        Text(
                            text = "Pending Proposals",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(proposals, key = { it.version_id }, contentType = { "proposal" }) { proposal ->
                        ProposalCard(
                            proposal = proposal,
                            onApprove = { comment, autoApproveSubsequent ->
                                onApproveProposal(proposal.version_id, comment, autoApproveSubsequent)
                            },
                            onDecline = { comment ->
                                onRejectProposal(proposal.version_id, comment)
                            }
                        )
                    }
                }

                // Notifications section
                if (notifications.isNotEmpty()) {
                    item(key = "notifications_header") {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = if (proposals.isNotEmpty()) 8.dp else 0.dp)
                        )
                    }
                    items(notifications, key = { it._id }, contentType = { "notification" }) { notification ->
                        NotificationCard(notification = notification)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (notification.has_unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.has_unread)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Activity type badge
                    val (typeEmoji, typeLabel) = when (notification.activity_type) {
                        "comment" -> "💬" to "Comment"
                        "proposal" -> "📝" to "Proposal"
                        "share_unlock" -> "🔓" to "Unlocked"
                        "version" -> "📋" to "Version"
                        else -> "🔔" to "Activity"
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$typeEmoji $typeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Surprise theme icon
                    if (!notification.surprise_theme.isNullOrBlank() && notification.surprise_theme != "none") {
                        val themeEmoji = when (notification.surprise_theme) {
                            "valentine" -> "💝"
                            "birthday" -> "🎂"
                            "anniversary" -> "💍"
                            "celebration" -> "🎉"
                            else -> "🎁"
                        }
                        Text(
                            text = "$themeEmoji ${notification.surprise_theme.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Unread indicator
                if (notification.has_unread) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Note title
            Text(
                text = notification.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Content preview
            if (notification.content.isNotBlank()) {
                Text(
                    text = notification.content.take(120),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Author + timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "by ${notification.author}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = notification.timestamp.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: ProposalDto,
    onApprove: (comment: String, autoApproveSubsequent: Boolean) -> Unit,
    onDecline: (comment: String) -> Unit
) {
    // Approve / Decline are routed through a small review dialog so
    // the owner can leave a comment (mirroring the web flow) and, on
    // accept, optionally mark this editor as auto-approved for future
    // proposals on the same share.
    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewApprove by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Note preview
            Text(
                text = proposal.note_preview.take(60),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Author and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "by ${proposal.author_username}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = proposal.created_at?.take(10) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Proposed content preview
            if (proposal.content.isNotBlank()) {
                Text(
                    text = proposal.content.take(100),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = {
                        reviewApprove = false
                        showReviewDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                ) {
                    Text("Decline", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                }
                Button(
                    onClick = {
                        reviewApprove = true
                        showReviewDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Approve", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showReviewDialog) {
        ProposalReviewDialog(
            isApprove = reviewApprove,
            onDismiss = { showReviewDialog = false },
            onSubmit = { comment, autoApproveSubsequent ->
                showReviewDialog = false
                if (reviewApprove) onApprove(comment, autoApproveSubsequent)
                else onDecline(comment)
            }
        )
    }
}

// ── Shared Links Tab ────────────────────────────────────────

@Composable
private fun SharedLinksTabContent(
    activeShares: List<Pair<AppNote, List<ShareDto>>>,
    sharesLoading: Boolean,
    onManageShares: (String) -> Unit,
    onOpenShareLink: (String) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    when {
        sharesLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        activeShares.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No notes with active share links",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(activeShares, key = { (note, _) -> note.id }, contentType = { "shared_note" }) { (note, shares) ->
                    SharedNoteCard(
                        note = note,
                        shares = shares,
                        onManageShares = { onManageShares(note.id) },
                        onOpenShareLink = onOpenShareLink
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedNoteCard(
    note: AppNote,
    shares: List<ShareDto>,
    onManageShares: () -> Unit,
    onOpenShareLink: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: first-char avatar + first line of content as subtitle + lock badge + Manage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FirstCharAvatar(
                    content = note.content,
                    titleFallback = note.title,
                    isLocked = note.isLocked
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (note.isLocked) "🔒 Locked Note"
                               else stripMarkdown(note.content).lineSequence().firstOrNull()?.trim()?.take(60)
                                   ?: "Untitled",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.isLocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = ErrorRed,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                "Locked on website",
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Button(
                    onClick = onManageShares,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Manage", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Individual share links
            shares.forEach { share ->
                ShareLinkRow(
                    share = share,
                    onOpenLink = { onOpenShareLink(share.share_id) }
                )
            }
        }
    }
}

/**
 * Circular avatar showing the first non-whitespace character of the note's
 * content (after stripping markdown). This is the "first char" visual cue
 * the user asked for on the Shared Links tab — Gmail-style — so they can
 * scan a long list of shares at a glance.
 *
 * - If the note is locked, a small lock badge sits in the bottom-right
 *   corner of the avatar so the locked state is unmissable.
 * - The accent colour is derived from the character's codepoint so the
 *   same note always shows the same colour across reloads.
 * - Falls back to "?" for genuinely empty notes.
 */
@Composable
private fun FirstCharAvatar(
    content: String,
    titleFallback: String,
    isLocked: Boolean,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    val stripped = if (isLocked) "" else stripMarkdown(content).trim()
    val firstChar = if (isLocked) "🔒"
        else stripped.firstOrNull()?.toString()?.take(1)?.uppercase()
            ?: titleFallback.trim().firstOrNull()?.toString()?.take(1)?.uppercase()
            ?: "?"

    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
    )
    val codepoint = firstChar.firstOrNull()?.code ?: 0
    val accent = palette[codepoint.mod(palette.size)]

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = firstChar,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        if (isLocked) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = ErrorRed,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
private fun ShareLinkRow(
    share: ShareDto,
    onOpenLink: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Share ID (truncated)
                Text(
                    text = share.share_id.take(12) + if (share.share_id.length > 12) "…" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Permissions badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = share.permissions,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Surprise theme badge (if not 'none')
                    if (share.surprise_theme != "none" && share.surprise_theme.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = share.surprise_theme,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenLink,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Text("Open Link", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Note Card (Notesnook-inspired flat design) ─────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: AppNote,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onSyncClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strippedContent = remember(note.content) { stripMarkdown(note.content) }
    val previewTitle = remember(strippedContent) {
        strippedContent.lineSequence().firstOrNull()?.trim()?.take(60) ?: "Untitled"
    }
    val previewBody = remember(strippedContent) {
        val lines = strippedContent.lineSequence().toList()
        if (lines.size <= 1) strippedContent else lines.drop(1).joinToString(" ").trim()
    }
    val relativeTime = remember(note.updatedAt) { formatRelativeTime(note.updatedAt) }

    val selectionBgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    Column(modifier = modifier) {
        // Flat note item — no Card, just content + divider (Notesnook style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(selectionBgColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Selection checkbox
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp, top = 2.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = previewTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (note.pendingOp == "draft") {
                        Text(
                            text = "Draft",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandOrange,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else if (!note.isSynced) {
                        val badgeText = if (com.example.echowithin.data.network.SessionManager.accountTier == "free") "Local" else "Pending"
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Update available banner
                if (note.updateAvailable) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSyncClick?.invoke() }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "⚠️", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "Update Available",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Sync Now",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Body preview
                if (previewBody.isNotBlank()) {
                    Text(
                        text = previewBody.take(140),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tags as inline text (Notesnook-style: plain #tag, no chips)
                if (note.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        note.tags.take(5).forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (note.tags.size > 5) {
                            Text(
                                text = "+${note.tags.size - 5}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Metadata row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!note.reference.isNullOrBlank()) {
                        Text(
                            text = "Ref: ${note.reference}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Thin divider (Notesnook flat list style)
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    }
}

// ── Utilities ───────────────────────────────────────────────

private val otaFormats = listOf(
    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") },
    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") },
    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US),
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
)
private val otaDisplayFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)

private fun formatRelativeTime(timestamp: String): String {
    if (timestamp.isBlank()) return ""
    try {
        var date: java.util.Date? = null
        synchronized(otaFormats) {
            for (format in otaFormats) {
                try {
                    date = format.parse(timestamp)
                    if (date != null) break
                } catch (_: Exception) {}
            }
        }

        if (date == null) {
            if (timestamp.length >= 10) return timestamp.take(10)
            return timestamp
        }

        val diffMs = System.currentTimeMillis() - date!!.time
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        val diffDay = diffHour / 24

        return when {
            diffMs < 0 -> "Just now"
            diffSec < 60 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHour < 24 -> "${diffHour}h ago"
            diffDay == 1L -> "Yesterday"
            diffDay < 7L -> "${diffDay}d ago"
            else -> {
                synchronized(otaDisplayFormat) {
                    otaDisplayFormat.format(date)
                }
            }
        }
    } catch (e: Exception) {
        return timestamp.take(10)
    }
}

/**
 * Compact "X ago" formatter used in the top app bar's "Synced 5m ago" pill.
 * Caps at "now" for sub-minute deltas, never shows seconds.
 */
private fun formatLastSynced(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val diffSec = (System.currentTimeMillis() - epochMillis) / 1000
    return when {
        diffSec < 60 -> "now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86_400 -> "${diffSec / 3600}h ago"
        else -> "${diffSec / 86_400}d ago"
    }
}

private fun stripBackslashEscapes(text: String): String {
    val sb = StringBuilder()
    var i = 0
    val escapable = setOf('\\', '`', '*', '_', '{', '}', '[', ']', '(', ')', '#', '+', '-', '.', '!', '~', '$')
    while (i < text.length) {
        if (text[i] == '\\' && i + 1 < text.length && text[i + 1] in escapable) {
            sb.append(text[i + 1])
            i += 2
        } else {
            sb.append(text[i])
            i++
        }
    }
    return sb.toString()
}

private fun stripMarkdown(text: String): String {
    var clean = text
    clean = REGEX_HEADING.replace(clean, "")
    clean = REGEX_BLOCKQUOTE.replace(clean, "")
    clean = REGEX_LIST_ITEM.replace(clean, "")
    clean = REGEX_FORMATTING.replace(clean, "")
    clean = REGEX_BACKTICK.replace(clean, "")
    clean = REGEX_LINK.replace(clean, "$1")
    clean = REGEX_IMAGE.replace(clean, "$1")
    return stripBackslashEscapes(clean)
}

@Composable
private fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnim = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1000,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnim.value, y = translateAnim.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
private fun NoteCardPlaceholder(brush: Brush) {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(18.dp)
                    .background(brush, shape = RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(brush, shape = RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .background(brush, shape = RoundedCornerShape(4.dp))
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 14.dp)
                        .background(brush, shape = RoundedCornerShape(4.dp))
                )
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 14.dp)
                        .background(brush, shape = RoundedCornerShape(4.dp))
                )
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun UpdateDialog(
    versionName: String,
    changelog: String,
    downloadProgress: Float?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (downloadProgress == null) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Update Available!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "A new version of EchoWithin ($versionName) is ready.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (changelog.isNotBlank()) {
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = changelog,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                if (downloadProgress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Text(
                            text = "Downloading... ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (downloadProgress == null) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Update Now", color = Color.White)
                }
            }
        },
        dismissButton = {
            if (downloadProgress == null) {
                TextButton(onClick = onDismiss) {
                    Text("Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
