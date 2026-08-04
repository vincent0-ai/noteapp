package com.example.echowithin.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.echowithin.presentation.components.EchoWithinTopBarTitle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import com.example.echowithin.data.network.SessionManager
import com.example.echowithin.data.local.PreferencesManager
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber
import com.example.echowithin.ui.theme.ErrorRed
import androidx.compose.runtime.*
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onLoginClick: () -> Unit,
    onAppLockClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    onCheckForUpdates: () -> Unit,
    updateInfo: com.example.echowithin.data.network.UpdateInfo?,
    downloadProgress: Float?,
    onConfirmUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    onSortOrderChanged: (String) -> Unit = {},
    onVerifyBiometricPin: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val isLoggedIn = !SessionManager.token.isNullOrBlank() && SessionManager.token != "null"
    val username = if (isLoggedIn) (SessionManager.username ?: "User") else "Guest User"
    var showSyncDialog by remember { mutableStateOf(false) }
    var currentSyncMode by remember { mutableStateOf(SessionManager.syncMode) }

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = BrandOrange
            )

            if (showSyncDialog) {
                AlertDialog(
                    onDismissRequest = { showSyncDialog = false },
                    title = { Text("Select Sync Mode") },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SessionManager.syncMode = "automatic"
                                        currentSyncMode = "automatic"
                                        showSyncDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentSyncMode == "automatic",
                                    onClick = {
                                        SessionManager.syncMode = "automatic"
                                        currentSyncMode = "automatic"
                                        showSyncDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Automatic Sync", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Syncs on startup and note changes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SessionManager.syncMode = "manual"
                                        currentSyncMode = "manual"
                                        showSyncDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentSyncMode == "manual",
                                    onClick = {
                                        SessionManager.syncMode = "manual"
                                        currentSyncMode = "manual"
                                        showSyncDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Manual Sync", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Only sync when clicking Sync button", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSyncDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }


            // Account section
            SettingsSectionHeader(title = "Account Info")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BrandOrange.copy(alpha = 0.1f),
                        modifier = Modifier.size(48.dp),
                        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = BrandOrange
                            )
                        }
                    }
                    Column {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isLoggedIn) "Sync status: Active" else "Sync status: Offline (No Account)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLoggedIn) BrandAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sync section
            SettingsSectionHeader(title = "Sync Preferences")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                SettingsRowItem(
                    icon = Icons.Default.Refresh,
                    title = "Sync Option",
                    subtitle = "Current: ${currentSyncMode.replaceFirstChar { it.uppercase() }}",
                    onClick = { showSyncDialog = true }
                )
            }

            // App Features section
            SettingsSectionHeader(title = "App Features")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                val biometricContext = androidx.compose.ui.platform.LocalContext.current
                val canUseBiometric = remember {
                    com.example.echowithin.data.local.BiometricHelper.canAuthenticate(biometricContext)
                }
                var biometricEnabled by remember { mutableStateOf(PreferencesManager.biometricEnabled) }
                var showBiometricPinDialog by remember { mutableStateOf(false) }
                var biometricPin by remember { mutableStateOf("") }
                var biometricPinError by remember { mutableStateOf<String?>(null) }
                var biometricVerifying by remember { mutableStateOf(false) }

                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Lock,
                        title = "App Security Lock",
                        subtitle = "Protect your notes with a secure PIN",
                        onClick = onAppLockClick
                    )
                    if (canUseBiometric) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric",
                                tint = BrandOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Biometric Unlock",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Use fingerprint or face to unlock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        // Require the PIN to be re-entered and
                                        // verified before enabling biometric unlock.
                                        biometricPin = ""
                                        biometricPinError = null
                                        showBiometricPinDialog = true
                                    } else {
                                        biometricEnabled = false
                                        PreferencesManager.biometricEnabled = false
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BrandOrange,
                                    checkedTrackColor = BrandOrange.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                if (showBiometricPinDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            if (!biometricVerifying) showBiometricPinDialog = false
                        },
                        title = {
                            Text("Confirm PIN", color = MaterialTheme.colorScheme.onSurface)
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Enter your app lock PIN to enable biometric unlock.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = biometricPin,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                            biometricPin = it
                                            if (biometricPinError != null) biometricPinError = null
                                        }
                                    },
                                    label = { Text("PIN") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    isError = biometricPinError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (biometricPinError != null) ErrorRed else BrandOrange,
                                        unfocusedBorderColor = if (biometricPinError != null) ErrorRed.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        focusedLabelColor = if (biometricPinError != null) ErrorRed else BrandOrange
                                    )
                                )
                                if (biometricPinError != null) {
                                    Text(
                                        text = biometricPinError!!,
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (biometricPin.length != 4) {
                                        biometricPinError = "Enter your 4-digit PIN."
                                        return@TextButton
                                    }
                                    biometricVerifying = true
                                    onVerifyBiometricPin(biometricPin) { success ->
                                        biometricVerifying = false
                                        if (success) {
                                            biometricEnabled = true
                                            PreferencesManager.biometricEnabled = true
                                            showBiometricPinDialog = false
                                        } else {
                                            biometricPinError = "Incorrect PIN. Please try again."
                                        }
                                    }
                                },
                                enabled = !biometricVerifying
                            ) {
                                if (biometricVerifying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = BrandOrange
                                    )
                                } else {
                                    Text("Enable")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showBiometricPinDialog = false },
                                enabled = !biometricVerifying
                            ) { Text("Cancel") }
                        }
                    )
                }
            }

            // Notes Preferences section
            SettingsSectionHeader(title = "Notes Preferences")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                var showSortDialog by remember { mutableStateOf(false) }
                var currentSort by remember { mutableStateOf(PreferencesManager.sortOrder) }
                var autoPurge by remember { mutableStateOf(PreferencesManager.autoPurgeTrash) }

                Column {
                    SettingsRowItem(
                        icon = Icons.AutoMirrored.Filled.Sort,
                        title = "Default Sort Order",
                        subtitle = "Current: ${sortDisplayName(currentSort)}",
                        onClick = { showSortDialog = true }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Auto Purge",
                            tint = BrandOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Purge Trash",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Delete trashed notes after 30 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoPurge,
                            onCheckedChange = {
                                autoPurge = it
                                PreferencesManager.autoPurgeTrash = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BrandOrange,
                                checkedTrackColor = BrandOrange.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                if (showSortDialog) {
                    val sortOptions = listOf(
                        "updated_desc" to "Last Modified (Newest)",
                        "updated_asc" to "Last Modified (Oldest)",
                        "title_asc" to "Title (A-Z)",
                        "title_desc" to "Title (Z-A)",
                        "created_desc" to "Created (Newest)",
                        "created_asc" to "Created (Oldest)"
                    )
                    AlertDialog(
                        onDismissRequest = { showSortDialog = false },
                        title = { Text("Default Sort Order") },
                        text = {
                            Column {
                                sortOptions.forEach { (key, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentSort = key
                                                PreferencesManager.sortOrder = key
                                                onSortOrderChanged(key)
                                                showSortDialog = false
                                            }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = currentSort == key,
                                            onClick = {
                                                currentSort = key
                                                PreferencesManager.sortOrder = key
                                                onSortOrderChanged(key)
                                                showSortDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showSortDialog = false }) { Text("Close") }
                        }
                    )
                }
            }

            // About EchoWithin section
            SettingsSectionHeader(title = "About EchoWithin")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "Visit EchoWithin Website",
                        subtitle = "For communities, surprise themes, and full features visit echowithin.xyz",
                        onClick = onWebsiteClick
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    SettingsRowItem(
                        icon = Icons.Default.Refresh,
                        title = "Check for Updates",
                        subtitle = "Verify if a new version is available",
                        onClick = onCheckForUpdates
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // App Version Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Version ${com.example.echowithin.BuildConfig.VERSION_NAME} (${com.example.echowithin.BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Logout action
            if (isLoggedIn) {
                // Logout action
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed.copy(alpha = 0.1f),
                        contentColor = ErrorRed
                    ),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = ErrorRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logout Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Login action
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandOrange,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Sign In",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign In / Create Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun sortDisplayName(key: String): String = when (key) {
    "updated_desc" -> "Last Modified (Newest)"
    "updated_asc" -> "Last Modified (Oldest)"
    "title_asc" -> "Title (A-Z)"
    "title_desc" -> "Title (Z-A)"
    "created_desc" -> "Created (Newest)"
    "created_asc" -> "Created (Oldest)"
    else -> key.replaceFirstChar { it.uppercase() }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = BrandAmber,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = BrandOrange,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "→",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
