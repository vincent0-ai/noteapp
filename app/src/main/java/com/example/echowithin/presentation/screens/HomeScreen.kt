package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.echowithin.data.model.AppNote
import com.example.echowithin.data.model.ProposalDto
import com.example.echowithin.data.model.ShareDto
import com.example.echowithin.data.model.NotificationDto
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber
import com.example.echowithin.ui.theme.ErrorRed

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
    // Lock-related
    hasPin: Boolean,
    isLocked: Boolean,
    lockError: String?,
    lockLoading: Boolean,
    onVerifyPin: (String) -> Unit,
    onSetupPin: (String) -> Unit,
    // Proposals (Activity tab)
    proposals: List<ProposalDto>,
    proposalsLoading: Boolean,
    onApproveProposal: (String) -> Unit,
    onRejectProposal: (String) -> Unit,
    // Share management
    activeShares: List<Pair<AppNote, List<ShareDto>>>,
    sharesLoading: Boolean,
    onManageShares: (String) -> Unit,
    onOpenShareLink: (String) -> Unit,
    // Notifications
    notifications: List<NotificationDto> = emptyList(),
    unreadNotificationsCount: Int = 0,
    onMarkAllRead: () -> Unit = {},
    // Update-related
    updateInfo: com.example.echowithin.data.network.UpdateInfo? = null,
    downloadProgress: Float? = null,
    onConfirmUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    // Navigation
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(HomeTab.NOTES) }

    if (updateInfo != null) {
        UpdateDialog(
            versionName = updateInfo.versionName,
            changelog = updateInfo.changelog,
            downloadProgress = downloadProgress,
            onDismiss = onDismissUpdate,
            onConfirm = onConfirmUpdate
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { EchoWithinTopBarTitle() },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            color = BrandOrange,
                            modifier = Modifier.size(28.dp).padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onSyncClick) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Notes",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (activeTab == HomeTab.NOTES) {
                FloatingActionButton(
                    onClick = onNewNoteClick,
                    containerColor = BrandOrange,
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
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = BrandOrange,
                edgePadding = 8.dp
            ) {
                HomeTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeTab == tab) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant
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
                HomeTab.NOTES -> NotesTabContent(
                    notes = notes.filter { !it.isLocked },
                    isLoading = isLoading,
                    error = error,
                    onNoteClick = onNoteClick,
                    onRetryClick = onRetryClick
                )
                HomeTab.LOCKED -> LockedTabContent(
                    lockedNotes = notes.filter { it.isLocked },
                    hasPin = hasPin,
                    isLocked = isLocked,
                    lockError = lockError,
                    lockLoading = lockLoading,
                    onVerifyPin = onVerifyPin,
                    onSetupPin = onSetupPin,
                    onNoteClick = onNoteClick
                )
                HomeTab.ACTIVITY -> ActivityTabContent(
                    proposals = proposals,
                    proposalsLoading = proposalsLoading,
                    notifications = notifications,
                    unreadNotificationsCount = unreadNotificationsCount,
                    onApproveProposal = onApproveProposal,
                    onRejectProposal = onRejectProposal,
                    onMarkAllRead = onMarkAllRead
                )
                HomeTab.SHARED_LINKS -> SharedLinksTabContent(
                    activeShares = activeShares,
                    sharesLoading = sharesLoading,
                    onManageShares = onManageShares,
                    onOpenShareLink = onOpenShareLink
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
    onRetryClick: () -> Unit
) {
    when {
        isLoading && notes.isEmpty() -> {
            val brush = shimmerBrush()
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
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
                    Button(onClick = onRetryClick, colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)) {
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
                        color = BrandOrange
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onNoteClick(note.id) })
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
    onNoteClick: (String) -> Unit
) {
    if (!hasPin) {
        // No PIN set up
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
                    color = BrandOrange.copy(alpha = 0.1f),
                    border = BorderStroke(2.dp, BrandOrange),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(32.dp))
                    }
                }
                Text(
                    text = "Set Up Security PIN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange
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
                        focusedBorderColor = BrandOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = BrandOrange
                    )
                )
                Button(
                    onClick = { onSetupPin(pin); pin = "" },
                    enabled = pin.length == 4 && !lockLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("Set PIN", fontWeight = FontWeight.Bold)
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
                    color = BrandOrange
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
                        focusedBorderColor = BrandOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedLabelColor = BrandOrange
                    )
                )
                if (lockError != null) {
                    Text(text = lockError, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
                if (lockLoading) {
                    CircularProgressIndicator(color = BrandOrange, modifier = Modifier.size(24.dp))
                }
                Button(
                    onClick = { onVerifyPin(pin); pin = "" },
                    enabled = pin.length == 4 && !lockLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(200.dp)
                ) {
                    Text("Unlock", fontWeight = FontWeight.Bold)
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
                        color = BrandAmber.copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, BrandAmber),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No locked notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandAmber
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(lockedNotes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onNoteClick(note.id) })
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
    onApproveProposal: (String) -> Unit,
    onRejectProposal: (String) -> Unit,
    onMarkAllRead: () -> Unit
) {
    val hasContent = proposals.isNotEmpty() || notifications.isNotEmpty()

    when {
        proposalsLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
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
                        color = BrandOrange
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                // Mark all read button
                if (unreadNotificationsCount > 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onMarkAllRead) {
                                Text(
                                    text = "Mark all as read",
                                    color = BrandOrange,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                // Proposals section
                if (proposals.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Proposals",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandAmber
                        )
                    }
                    items(proposals) { proposal ->
                        ProposalCard(
                            proposal = proposal,
                            onApprove = { onApproveProposal(proposal.version_id) },
                            onDecline = { onRejectProposal(proposal.version_id) }
                        )
                    }
                }

                // Notifications section
                if (notifications.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandAmber,
                            modifier = Modifier.padding(top = if (proposals.isNotEmpty()) 8.dp else 0.dp)
                        )
                    }
                    items(notifications) { notification ->
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
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (notification.has_unread) BrandOrange.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.has_unread)
                BrandOrange.copy(alpha = 0.04f)
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
                        color = BrandAmber.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$typeEmoji $typeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandAmber,
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
                            color = BrandOrange
                        )
                    }
                }

                // Unread indicator
                if (notification.has_unread) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandOrange
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
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                color = BrandOrange,
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
                    onClick = onDecline,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
                ) {
                    Text("Decline", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                }
                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Approve", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ── Shared Links Tab ────────────────────────────────────────

@Composable
private fun SharedLinksTabContent(
    activeShares: List<Pair<AppNote, List<ShareDto>>>,
    sharesLoading: Boolean,
    onManageShares: (String) -> Unit,
    onOpenShareLink: (String) -> Unit
) {
    when {
        sharesLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(activeShares) { (note, shares) ->
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
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Note first line preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stripMarkdown(note.content).lineSequence().firstOrNull()?.trim()?.take(60)
                        ?: "Untitled",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onManageShares,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
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
                        color = BrandAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = share.permissions,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Surprise theme badge (if not 'none')
                    if (share.surprise_theme != "none" && share.surprise_theme.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandOrange.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = share.surprise_theme,
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandOrange,
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
                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.5f))
            ) {
                Text("Open Link", style = MaterialTheme.typography.labelSmall, color = BrandOrange)
            }
        }
    }
}

// ── Note Card ───────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCard(note: AppNote, onClick: () -> Unit) {
    val strippedContent = remember(note.content) { stripMarkdown(note.content) }
    val previewTitle = remember(strippedContent) {
        strippedContent.lineSequence().firstOrNull()?.trim()?.take(60) ?: "Untitled"
    }
    val previewBody = remember(strippedContent) {
        val lines = strippedContent.lineSequence().toList()
        if (lines.size <= 1) strippedContent else lines.drop(1).joinToString(" ").trim()
    }
    val relativeTime = remember(note.updatedAt) { formatRelativeTime(note.updatedAt) }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "clickScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = previewTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!note.isSynced) {
                    val badgeText = if (com.example.echowithin.data.network.SessionManager.accountTier == "free") "Local" else "Pending"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BrandAmber.copy(alpha = 0.15f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (previewBody.isNotBlank()) {
                Text(
                    text = previewBody.take(180),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    note.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandOrange.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!note.reference.isNullOrBlank()) {
                    Text(
                        text = "Ref: ${note.reference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandAmber.copy(alpha = 0.7f),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
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
        
        val diffMs = System.currentTimeMillis() - date.time
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

private fun stripMarkdown(text: String): String {
    var clean = text
    clean = clean.replace(Regex("(?m)^#+\\s+"), "")
    clean = clean.replace(Regex("(?m)^[\\s*+-]*>\\s*"), "")
    clean = clean.replace(Regex("(?m)^[\\s]*[*+-]\\s+"), "")
    clean = clean.replace(Regex("\\*\\*|__|\\*|_|~~"), "")
    clean = clean.replace(Regex("`+"), "")
    clean = clean.replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
    clean = clean.replace(Regex("!\\[(.*?)\\]\\(.*?\\)"), "$1")
    return clean
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(20.dp)
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
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 18.dp)
                        .background(brush, shape = RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 18.dp)
                        .background(brush, shape = RoundedCornerShape(6.dp))
                )
            }
        }
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
                    tint = BrandOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Update Available!", fontWeight = FontWeight.Bold, color = BrandOrange)
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
                            color = BrandOrange,
                            trackColor = BrandOrange.copy(alpha = 0.2f),
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
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
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
