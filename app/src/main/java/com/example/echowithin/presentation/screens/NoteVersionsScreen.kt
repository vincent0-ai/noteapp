package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import com.example.echowithin.presentation.components.ProposalReviewDialog
import com.example.echowithin.presentation.viewmodel.NoteVersionsUiState
import com.example.echowithin.data.model.VersionDto
import com.example.echowithin.data.network.SessionManager
import com.example.echowithin.util.DiffLine
import com.example.echowithin.util.DiffUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteVersionsScreen(
    uiState: NoteVersionsUiState,
    onBack: () -> Unit,
    onRestore: (String) -> Unit,
    onDecide: (String, Boolean, String, Boolean) -> Unit,
    onClearFeedback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var reviewTarget by remember { mutableStateOf<ReviewTarget?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onClearFeedback()
        }
    }

    LaunchedEffect(uiState.restoreSuccess) {
        if (uiState.restoreSuccess) {
            snackbarHostState.showSnackbar("Version restored successfully")
            onClearFeedback()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Version History",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Version History",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Note ID: ${uiState.noteId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (uiState.versions.isEmpty()) {
                    item {
                        Text(
                            text = "No previous versions saved for this note yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    itemsIndexed(uiState.versions, key = { _, v -> v.version_id }) { index, version ->
                        val previousContent = uiState.versions.getOrNull(index + 1)?.content
                        VersionCard(
                            version = version,
                            previousContent = previousContent,
                            onRestore = { onRestore(version.version_id) },
                            onApprove = { reviewTarget = ReviewTarget(version.version_id, true) },
                            onReject = { reviewTarget = ReviewTarget(version.version_id, false) }
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    reviewTarget?.let { target ->
        ProposalReviewDialog(
            isApprove = target.isApprove,
            onDismiss = { reviewTarget = null },
            onSubmit = { comment, autoApproveSubsequent ->
                onDecide(target.versionId, target.isApprove, comment, autoApproveSubsequent)
                reviewTarget = null
            }
        )
    }
}

@Composable
private fun VersionCard(
    version: VersionDto,
    previousContent: String?,
    onRestore: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    var showDiff by remember { mutableStateOf(false) }

    val diffResult = remember(version.content, previousContent) {
        DiffUtils.computeDiff(previousContent.orEmpty(), version.content)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayAuthor = when {
                    version.author_username == "Unknown" || version.author_username == SessionManager.username -> "You"
                    else -> version.author_username
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "Modified by $displayAuthor",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!version.created_at.isNullOrBlank()) {
                        Text(
                            text = com.example.echowithin.util.DateTimeUtils.formatFullDateTime(version.created_at),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (diffResult.hasChanges) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (diffResult.additions > 0) {
                                    Text(
                                        text = "+${diffResult.additions}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                if (diffResult.deletions > 0) {
                                    Text(
                                        text = "-${diffResult.deletions}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }

                    if (version.shouldShowStatusBadge()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                text = version.status.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // View toggle: Preview vs Diff
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !showDiff,
                    onClick = { showDiff = false },
                    label = { Text("Preview", style = MaterialTheme.typography.labelSmall) }
                )
                FilterChip(
                    selected = showDiff,
                    onClick = { showDiff = true },
                    label = { Text("Changes (Diff)", style = MaterialTheme.typography.labelSmall) }
                )
            }

            if (showDiff) {
                // Unified diff viewer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        if (diffResult.lines.isEmpty()) {
                            Text(
                                text = "(No content changes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            diffResult.lines.forEach { line ->
                                val (bg, textCol, prefix) = when (line) {
                                    is DiffLine.Added -> Triple(Color(0xFF2E7D32).copy(alpha = 0.12f), Color(0xFF2E7D32), "+ ")
                                    is DiffLine.Deleted -> Triple(Color(0xFFC62828).copy(alpha = 0.12f), Color(0xFFC62828), "- ")
                                    is DiffLine.Unchanged -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant, "  ")
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bg, shape = RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = prefix + line.text,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = textCol
                                        ),
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Preview content
                val displayContent = version.content.ifBlank { "(Content unavailable)" }
                Text(
                    text = displayContent.take(200),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore", style = MaterialTheme.typography.bodySmall)
                }

                if (version.shouldShowApproveRejectButtons()) {
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(16.dp))
                    }

                    Button(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private data class ReviewTarget(val versionId: String, val isApprove: Boolean)

fun VersionDto.shouldShowStatusBadge(): Boolean = is_proposal

fun VersionDto.shouldShowApproveRejectButtons(): Boolean =
    is_proposal && status.equals("pending", ignoreCase = true)
