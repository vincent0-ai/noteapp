package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import com.example.echowithin.presentation.viewmodel.NoteShareUiState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.OpenInBrowser
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber
import com.example.echowithin.ui.theme.ErrorRed

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ContentCopy
import com.example.echowithin.data.network.SessionManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteShareScreen(
    uiState: NoteShareUiState,
    onBack: () -> Unit,
    onCreateShare: (permissions: String, expiresIn: String?, accessCode: String?, surpriseTheme: String, useTypewriter: Boolean, autoApprove: Boolean, photoUri: String?, audioUri: String?) -> Unit,
    onSelectShare: (String) -> Unit,
    onRevokeShare: (String) -> Unit,
    onOpenShareLink: (String) -> Unit,
    onToggleAutoApprove: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    // Share creation dialog state
    var selectedPermission by remember { mutableStateOf("view") }
    var accessCode by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf("none") }
    var useTypewriter by remember { mutableStateOf(false) }
    var autoApprove by remember { mutableStateOf(false) }
    var selectedExpiry by remember { mutableStateOf<String?>(null) }
    var themeDropdownExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var audioUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        photoUri = uri
    }

    val audioLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        audioUri = uri
    }

    fun getFileName(uri: android.net.Uri?): String? {
        if (uri == null) return null
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) name = it.getString(idx)
                }
            }
        } catch (_: Exception) {}
        return name ?: uri.lastPathSegment
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
                Text(
                    text = "Share Note Controls",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange
                )
                Text(
                    text = "Note ID: ${uiState.noteId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Create Share button
            item {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Create Share Link")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Share Link", fontWeight = FontWeight.Bold)
                }
            }

            // Active Shares Section
            item {
                Text(
                    text = "Active Share Links",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandAmber
                )
            }

            if (uiState.shares.isEmpty()) {
                item {
                    Text(
                        text = "No active share links. Generate one above to allow others to view this note.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                    items(uiState.shares, key = { it.share_id }) { share ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Share: ${share.share_id.take(8)}...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandOrange
                                    )
                                    Text(
                                        text = "Permissions: ${share.permissions}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (share.has_password) {
                                    Text(
                                        text = "🔒 Password Protected",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Surprise theme and auto-approve status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (share.surprise_theme != "none") {
                                    Text(
                                        text = "🎨 Theme: ${share.surprise_theme}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandAmber
                                    )
                                }
                                if (share.auto_approve) {
                                    Text(
                                        text = "✅ Auto-approve",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandAmber
                                    )
                                }
                                if (share.use_typewriter) {
                                    Text(
                                        text = "⌨️ Typewriter",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandAmber
                                    )
                                }
                            }

                            // Auto-approve toggle. Editable per share so the
                            // owner can flip the link-wide flag after
                            // creation (server returns 403 with
                            // `upgrade_required` for free-tier accounts —
                            // the snackbar surfaces the message).
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Auto-approve edits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Switch(
                                    checked = share.auto_approve,
                                    onCheckedChange = { onToggleAutoApprove(share.share_id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BrandOrange
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Copy Link
                                Button(
                                    onClick = {
                                        val url = "https://echowithin.xyz/share/note/${share.share_id}"
                                        clipboardManager.setText(AnnotatedString(url))
                                        android.widget.Toast.makeText(context, "Link copied", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandOrange,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }

                                // Share via other apps
                                Button(
                                    onClick = {
                                        val url = "https://echowithin.xyz/share/note/${share.share_id}"
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, url)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share link"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BrandOrange,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Link", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }

                                // Open in browser
                                Button(
                                    onClick = { onOpenShareLink(share.share_id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Link", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }

                                // Revoke
                                Button(
                                    onClick = { onRevokeShare(share.share_id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Revoke", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Revoke", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
            }

            // Attachments section (Only shown if a share is selected)
            if (uiState.selectedShareId != null && uiState.attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "Attachments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandAmber
                    )
                }

                items(uiState.attachments, key = { it.id }) { attachment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Attachment, contentDescription = "Attachment", tint = BrandOrange)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = attachment.filename ?: "Attachment",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = attachment.file_type ?: "unknown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = attachment.file_url ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Share Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "Create Share Link",
                    fontWeight = FontWeight.Bold,
                    color = BrandOrange
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Permissions
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPermission == "view",
                                onClick = { selectedPermission = "view" },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandOrange)
                            )
                            Text("View")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPermission == "edit",
                                onClick = { selectedPermission = "edit" },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandOrange)
                            )
                            Text("Edit")
                        }
                    }

                    // Access Code
                    Text(
                        text = "Access Code (optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = accessCode,
                        onValueChange = { accessCode = it },
                        placeholder = { Text("Enter access code...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandOrange,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Surprise Theme
                    Text(
                        text = "Surprise Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    ExposedDropdownMenuBox(
                        expanded = themeDropdownExpanded,
                        onExpandedChange = { themeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (selectedTheme) {
                                "none" -> "None"
                                "typewriter" -> "Typewriter"
                                else -> selectedTheme.replaceFirstChar { it.uppercase() }
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandOrange,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = themeDropdownExpanded,
                            onDismissRequest = { themeDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedTheme = "none"
                                    useTypewriter = false
                                    themeDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Typewriter") },
                                onClick = {
                                    selectedTheme = "typewriter"
                                    useTypewriter = true
                                    themeDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Valentine") },
                                onClick = {
                                    selectedTheme = "valentine"
                                    themeDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Birthday") },
                                onClick = {
                                    selectedTheme = "birthday"
                                    themeDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Anniversary") },
                                onClick = {
                                    selectedTheme = "anniversary"
                                    themeDropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Celebration") },
                                onClick = {
                                    selectedTheme = "celebration"
                                    themeDropdownExpanded = false
                                }
                            )
                        }
                    }

                    // Premium surprise themes photo + music upload
                    if (selectedTheme != "none") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = BrandAmber.copy(alpha = 0.08f)
                            ),
                            border = BorderStroke(1.dp, BrandAmber.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "✨ Custom Surprise Media",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandAmber
                                    )
                                    if (SessionManager.accountTier != "premium") {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = BrandOrange.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Premium lock",
                                                    tint = BrandOrange,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "PREMIUM",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BrandOrange,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Photo Picker Box
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Photo Attachment (image/*)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clickable {
                                                if (SessionManager.accountTier != "premium") {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Premium Upgrade required for custom media themes!",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    photoLauncher.launch("image/*")
                                                }
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = getFileName(photoUri) ?: "Tap to select photo...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (photoUri != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        if (SessionManager.accountTier != "premium") {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Attachment,
                                                contentDescription = "Attachment",
                                                tint = BrandOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Music Picker Box
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Music Attachment (audio/*)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .clickable {
                                                if (SessionManager.accountTier != "premium") {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Premium Upgrade required for custom media themes!",
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    audioLauncher.launch("audio/*")
                                                }
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = getFileName(audioUri) ?: "Tap to select music...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (audioUri != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                        if (SessionManager.accountTier != "premium") {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Attachment,
                                                contentDescription = "Attachment",
                                                tint = BrandOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Typewriter Checkbox inside theme card for ease
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Use typewriter effect",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Checkbox(
                                        checked = useTypewriter,
                                        onCheckedChange = { useTypewriter = it },
                                        colors = CheckboxDefaults.colors(checkedColor = BrandOrange)
                                    )
                                }
                            }
                        }
                    }

                    // Auto-approve edits
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-approve edits",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = autoApprove,
                            onCheckedChange = { autoApprove = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BrandOrange
                            )
                        )
                    }

                    // Expiry
                    Text(
                        text = "Expiry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Column {
                        val expiryOptions = listOf(
                            null to "Never",
                            "1h" to "1 Hour",
                            "1d" to "1 Day",
                            "7d" to "7 Days"
                        )
                        expiryOptions.forEach { (value, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedExpiry == value,
                                    onClick = { selectedExpiry = value },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandOrange)
                                )
                                Text(label)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        onCreateShare(
                            selectedPermission,
                            selectedExpiry,
                            accessCode.ifBlank { null },
                            selectedTheme,
                            useTypewriter,
                            autoApprove,
                            photoUri?.toString(),
                            audioUri?.toString()
                        )
                        // Reset dialog state
                        selectedPermission = "view"
                        accessCode = ""
                        selectedTheme = "none"
                        useTypewriter = false
                        autoApprove = false
                        selectedExpiry = null
                        photoUri = null
                        audioUri = null
                    }
                ) {
                    Text("Create", color = BrandOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Creating Share Link Loading Dialog Overlay
    if (uiState.isLoading) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = BrandOrange,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Creating Share Link...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandOrange
                    )
                    Text(
                        text = "Please wait while we upload custom media and generate your secure share link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
}

