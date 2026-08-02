package com.example.echowithin.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.echowithin.data.network.ApiClient
import com.example.echowithin.data.network.NetworkMonitor
import com.example.echowithin.data.network.SessionManager
import com.example.echowithin.presentation.screens.AppLockScreen
import com.example.echowithin.presentation.screens.LoginScreen
import com.example.echowithin.presentation.screens.HomeScreen
import com.example.echowithin.presentation.screens.NoteDetailScreen
import com.example.echowithin.presentation.screens.NoteEditorScreen
import com.example.echowithin.presentation.screens.NoteShareScreen
import com.example.echowithin.presentation.screens.NoteVersionsScreen
import com.example.echowithin.presentation.screens.PremiumScreen
import com.example.echowithin.presentation.screens.SearchScreen
import com.example.echowithin.presentation.screens.SettingsScreen
import com.example.echowithin.presentation.screens.ConfirmEmailScreen
import com.example.echowithin.presentation.screens.RegisterScreen
import com.example.echowithin.presentation.screens.WelcomeScreen
import com.example.echowithin.presentation.viewmodel.AuthViewModel
import com.example.echowithin.presentation.viewmodel.AppLockViewModel
import com.example.echowithin.presentation.viewmodel.NoteShareViewModel
import com.example.echowithin.presentation.viewmodel.NoteVersionsViewModel
import com.example.echowithin.presentation.viewmodel.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun AppNavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    authViewModel: AuthViewModel,
    notesViewModel: NotesViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Dynamic start destination based on whether user has an active session token
    val startDest = if (SessionManager.token != null && SessionManager.token != "null") AppRoute.Home else AppRoute.Welcome

    // Shared ViewModels for Home tab usage
    val appLockViewModel: AppLockViewModel = viewModel(factory = AppLockViewModel.factory())

    // Connectivity is observed ONCE at the graph level (not inside the Home
    // destination). Previously these effects lived inside
    // composable(AppRoute.Home){}, so every navigation Home -> Detail -> Home
    // tore them down and re-fired them, re-arming the sync trigger and
    // re-calling onConnectivityChanged() — which then kicked syncNotes() over
    // and over (the "frequent server syncing between page navigation" bug).
    // Hoisting them to the graph scope means they live for the whole session
    // and never re-run on navigation. The resulting `isOnline` value is also
    // handed to HomeScreen via its existing parameter.
    val isOnlineState = remember { NetworkMonitor.isOnline }.collectAsState()
    val isOnline = isOnlineState.value
    LaunchedEffect(isOnline) {
        notesViewModel.onConnectivityChanged(isOnline)
    }
    val syncTriggerState = notesViewModel.syncTrigger.collectAsState()
    val syncTrigger = syncTriggerState.value
    // Gated on syncMode == "automatic" as defense in depth — the view-model
    // also gates onConnectivityChanged() the same way, but if anything else
    // ever pings the trigger we still respect the user's manual-sync
    // preference. This effect now runs at graph scope, so it fires once per
    // trigger change rather than once per Home re-entry.
    LaunchedEffect(syncTrigger) {
        if (syncTrigger > 0L && isOnline &&
            SessionManager.syncMode == "automatic"
        ) {
            notesViewModel.syncNotes()
        }
    }
    // Ephemeral toasts (mark-all-read cleared, sync done, etc.). Hoisted to
    // graph scope so a toast queued while on Detail/Editor still fires when
    // the user returns to Home, without needing Home to be on screen.
    val ephemeral = notesViewModel.ephemeralMessage
    LaunchedEffect(ephemeral) {
        if (!ephemeral.isNullOrBlank()) {
            android.widget.Toast.makeText(context, ephemeral, android.widget.Toast.LENGTH_SHORT).show()
            notesViewModel.consumeEphemeralMessage()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ApiClient.registerFcmToken(context)
        } else {
            Log.d("FCM", "Notification permission denied")
        }
    }

            // Validate session then load data on launch.
            //
            // IMPORTANT: in offline mode (no token — the user tapped
            // "Continue Offline") we must NOT make any network call here.
            // Previously checkForUpdates() ran unconditionally: it hit the
            // server with no token → 401 → the global 401 interceptor
            // cleared the session and navigated back to Welcome, producing
            // the "I keep being returned to the welcome screen" loop and
            // the "why is it syncing on startup" complaint. Now every
            // network call (update check, appReauth, profile) is gated on
            // hasToken, and the offline branch only reads the local DB +
            // restores the local PIN state so locked notes can unlock.
            LaunchedEffect(Unit) {
                if (notesViewModel.isInitialDataLoaded) return@LaunchedEffect
                val hasToken = SessionManager.token != null && SessionManager.token != "null" && !SessionManager.token.isNullOrBlank()
                if (hasToken) {
                    // Online: check for app updates (network), then verify session.
                    notesViewModel.checkForUpdates(context)
                    try {
                        // Call appReauth to verify session/token on server
                        withContext(Dispatchers.IO) {
                            ApiClient.apiService.appReauth()
                        }

                        // Session token is valid! Register FCM token if permission granted
                        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }

                        if (hasPermission) {
                            ApiClient.registerFcmToken(context)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }

                        // Token is valid! Update account tier, then load notes.
                        try {
                            val profile = withContext(Dispatchers.IO) {
                                ApiClient.apiService.getProfile()
                            }
                            SessionManager.accountTier = profile.account_tier
                        } catch (_: Exception) { }
                        // Session confirmed — now it's safe to load notes and lock status
                        notesViewModel.loadAllData()
                        appLockViewModel.refreshStatus()
                    } catch (e: retrofit2.HttpException) {
                        // A token was present but the server rejected it
                        // (appReauth failed) — treat as session-expired and
                        // fall back to offline-only. Use clearSession() so
                        // the device-local PIN hash survives (otherwise the
                        // correct PIN can't unlock offline afterwards).
                        SessionManager.clearSession()
                        notesViewModel.clearOfflineData() // Preserves offline-only notes
                        notesViewModel.loadNotes()
                        appLockViewModel.refreshStatus()
                        navController.navigate(AppRoute.Welcome) {
                            popUpTo(0) { inclusive = true }
                        }
                    } catch (_: Exception) {
                        // Network error — load from local DB only, keep token for retry
                        notesViewModel.loadNotes()
                        appLockViewModel.refreshStatus()
                    }
                } else {
                    // Offline mode: no token, so NO network calls. Just
                    // hydrate from the local DB and restore the local PIN
                    // state so locked notes can be unlocked offline.
                    notesViewModel.loadNotes()
                    appLockViewModel.refreshStatus()
                }
            }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = Modifier.padding(innerPadding),
        enterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
        exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) },
        popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) },
        popExitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) }
    ) {
        composable(AppRoute.Welcome) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(AppRoute.Login)
                },
                onContinueOffline = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo(AppRoute.Welcome) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.Login) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    notesViewModel.loadAllData()
                    appLockViewModel.refreshStatus()
                    navController.navigate(AppRoute.Home) {
                        popUpTo(AppRoute.Login) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AppRoute.Register)
                },
                onNavigateToConfirm = { email ->
                    navController.navigate(AppRoute.confirmEmail(email))
                }
            )
        }

        composable(AppRoute.Register) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = { email ->
                    navController.navigate(AppRoute.confirmEmail(email))
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = AppRoute.ConfirmEmail,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            ConfirmEmailScreen(
                email = email,
                viewModel = authViewModel,
                onConfirmSuccess = {
                    navController.navigate(AppRoute.Login) {
                        popUpTo(AppRoute.Register) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.navigate(AppRoute.Login) {
                        popUpTo(AppRoute.Welcome) { inclusive = true }
                    }
                }
            )
        }

        composable(AppRoute.Home) {
            // Connectivity observation, the sync-trigger effect, and the
            // ephemeral-toast effect all live at the AppNavGraph scope now
            // (see top of this composable) so that navigating away from Home
            // and back does NOT re-fire them. `isOnline` is handed down via
            // the existing HomeScreen parameter.
            val isOfflineMode = SessionManager.token.isNullOrBlank() || SessionManager.token == "null"

            // Offline notes count for backup prompt
            var offlineNotesCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(isOfflineMode) {
                if (isOfflineMode) {
                    offlineNotesCount = withContext(Dispatchers.IO) {
                        notesViewModel.getOfflineNotesCount()
                    }
                }
            }

            // Offline privacy dialog — show once per offline session
            var showOfflinePrivacyDialog by remember { mutableStateOf(false) }
            LaunchedEffect(isOfflineMode) {
                if (isOfflineMode && !SessionManager.offlinePrivacyShown) {
                    showOfflinePrivacyDialog = true
                }
            }

            HomeScreen(
                notes = notesViewModel.uiState.notes,
                isLoading = notesViewModel.uiState.isLoading,
                isSyncing = notesViewModel.uiState.isSyncing,
                error = notesViewModel.uiState.error,
                onNoteClick = { noteId -> navController.navigate(AppRoute.detail(noteId)) },
                onNewNoteClick = { navController.navigate(AppRoute.editor(noteId = null)) },
                onSyncClick = {
                    if (isOfflineMode) {
                        android.widget.Toast.makeText(context, "Sign in or create an account to sync notes!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        notesViewModel.syncNotes(force = true)
                    }
                },
                onRetryClick = { notesViewModel.loadNotes() },
                onSyncNoteClick = { noteId ->
                    notesViewModel.syncNoteWithOriginal(noteId) { msg ->
                        android.widget.Toast.makeText(context, msg ?: "Sync failed", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                // Lock-related
                hasPin = appLockViewModel.uiState.hasPin,
                isLocked = appLockViewModel.uiState.isLocked,
                lockError = appLockViewModel.uiState.error,
                lockLoading = appLockViewModel.uiState.isLoading,
                onVerifyPin = { appLockViewModel.verify(it) },
                onSetupPin = { appLockViewModel.setup(it) },
                onBiometricUnlock = {
                    val activity = context as? androidx.fragment.app.FragmentActivity
                    if (activity != null) {
                        com.example.echowithin.data.local.BiometricHelper.authenticate(
                            activity = activity,
                            onSuccess = {
                                // Biometric success = bypass PIN verification, directly unlock
                                appLockViewModel.verify("__biometric__")
                            },
                            onError = { msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                // Proposals
                proposals = notesViewModel.uiState.proposals,
                proposalsLoading = notesViewModel.uiState.proposalsLoading,
                onApproveProposal = { versionId, comment, autoApproveSubsequent ->
                    notesViewModel.approveProposal(versionId, comment, autoApproveSubsequent)
                },
                onRejectProposal = { versionId, comment ->
                    notesViewModel.rejectProposal(versionId, comment)
                },
                // Share management
                activeShares = notesViewModel.uiState.activeShares,
                sharesLoading = notesViewModel.uiState.sharesLoading,
                onManageShares = { noteId -> navController.navigate(AppRoute.share(noteId)) },
                onOpenShareLink = { shareId ->
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://echowithin.xyz/share/note/$shareId")
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                // Notifications
                notifications = notesViewModel.uiState.notifications,
                unreadNotificationsCount = notesViewModel.uiState.unreadNotificationsCount,
                onMarkAllRead = { notesViewModel.markAllNotificationsAsRead() },
                markingAllRead = notesViewModel.uiState.markingAllRead,
                // Offline + sync state
                isOnline = isOnline,
                pendingSyncCount = notesViewModel.uiState.pendingSyncCount,
                lastSyncedAt = notesViewModel.uiState.lastSyncedAt,
                // Offline mode flag
                isOfflineMode = isOfflineMode,
                // Offline notes backup
                onBackupOfflineNotes = { notesViewModel.backupOfflineNotes { count ->
                    if (count > 0) {
                        android.widget.Toast.makeText(context, "Backed up $count offline note${if (count > 1) "s" else ""}!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }},
                offlineNotesCount = offlineNotesCount,
                // Offline privacy dialog
                showOfflinePrivacyDialog = showOfflinePrivacyDialog,
                onDismissOfflinePrivacy = {
                    showOfflinePrivacyDialog = false
                    SessionManager.offlinePrivacyShown = true
                },
                // Update-related
                updateInfo = notesViewModel.uiState.updateInfo,
                downloadProgress = notesViewModel.uiState.downloadProgress,
                onConfirmUpdate = { notesViewModel.downloadAndInstallUpdate(context, notesViewModel.uiState.updateInfo?.apkUrl ?: "") },
                onDismissUpdate = { notesViewModel.dismissUpdate() },
                // Navigation
                onSearchClick = { navController.navigate(AppRoute.Search) },
                onImportNotes = { imported ->
                    notesViewModel.importNotes(imported) { count ->
                        android.widget.Toast.makeText(context, "Imported $count note${if (count != 1) "s" else ""} successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onBatchDeleteNotes = { noteIds ->
                    notesViewModel.deleteNotes(noteIds) { count ->
                        android.widget.Toast.makeText(context, "Deleted $count note${if (count != 1) "s" else ""}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                onTrashClick = { navController.navigate(AppRoute.Trash) },
                // Folders
                folders = notesViewModel.uiState.folders,
                filterFolder = notesViewModel.uiState.filterFolder,
                onFilterFolder = { notesViewModel.setFilterFolder(it) }
            )
        }



        composable(AppRoute.AppLock) {
            AppLockScreen(
                hasPin = appLockViewModel.uiState.hasPin,
                isLocked = appLockViewModel.uiState.isLocked,
                isLoading = appLockViewModel.uiState.isLoading,
                error = appLockViewModel.uiState.error,
                onSetup = { appLockViewModel.setup(it) },
                onVerify = { appLockViewModel.verify(it) },
                onRemove = { appLockViewModel.remove(it) },
                onClearError = { appLockViewModel.clearError() }
            )
        }

        composable(
            route = AppRoute.EditorWithArg,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val rawNoteId = backStackEntry.arguments?.getString("noteId")
            val noteId = if (rawNoteId == "null" || rawNoteId.isNullOrBlank()) null else rawNoteId
            NoteEditorScreen(
                noteId = noteId,
                initialNote = noteId?.let(notesViewModel::getNoteById),
                isSaving = notesViewModel.uiState.isLoading,
                onBack = { navController.popBackStack() },
                onSave = { targetId, content, reference, tags ->
                    if (targetId == null) {
                        notesViewModel.createNote(content, reference, tags) {
                            navController.popBackStack()
                        }
                    } else {
                        notesViewModel.editNote(targetId, content, reference, tags) {
                            navController.popBackStack()
                        }
                    }
                },
                onSaveDraft = { targetId, content, reference, tags, onDraftId ->
                    // Save locally as draft — fire-and-forget, no navigation
                    notesViewModel.saveDraft(targetId, content, reference, tags, onDraftId)
                },
                isLocked = appLockViewModel.uiState.isLocked,
                lockError = appLockViewModel.uiState.error,
                lockLoading = appLockViewModel.uiState.isLoading,
                onVerifyPin = { appLockViewModel.verify(it) }
            )
        }

        composable(
            route = AppRoute.Detail,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId").orEmpty()
            NoteDetailScreen(
                noteId = noteId,
                initialNote = notesViewModel.getNoteById(noteId),
                loadNote = { id -> notesViewModel.getNoteFromServer(id) },
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(AppRoute.editor(noteId)) },
                onShare = { navController.navigate(AppRoute.share(noteId)) },
                onVersions = { navController.navigate(AppRoute.versions(noteId)) },
                onDelete = {
                    notesViewModel.deleteNote(noteId) {
                        navController.popBackStack()
                    }
                },
                onToggleLock = { onDone ->
                    notesViewModel.toggleNoteLock(noteId) { newLocked ->
                        onDone(newLocked)
                    }
                },
                onTogglePin = { onDone ->
                    notesViewModel.toggleNotePin(noteId) { newPinned ->
                        onDone(newPinned)
                    }
                },
                onSyncClick = { onDone ->
                    notesViewModel.syncNoteWithOriginal(noteId) { msg ->
                        onDone(msg)
                    }
                },
                isLocked = appLockViewModel.uiState.isLocked,
                lockError = appLockViewModel.uiState.error,
                lockLoading = appLockViewModel.uiState.isLoading,
                onVerifyPin = { appLockViewModel.verify(it) }
            )
        }

        composable(
            route = AppRoute.Share,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId").orEmpty()
            val shareViewModel: NoteShareViewModel = viewModel(factory = NoteShareViewModel.factory())
            LaunchedEffect(noteId) { shareViewModel.load(noteId) }
            NoteShareScreen(
                uiState = shareViewModel.uiState,
                onBack = { navController.popBackStack() },
                onCreateShare = { permissions, expiresIn, accessCode, surpriseTheme, useTypewriter, autoApprove, photoUri, audioUri ->
                    shareViewModel.createShare(context, permissions, expiresIn, accessCode, surpriseTheme, useTypewriter, autoApprove, photoUri, audioUri)
                },
                onSelectShare = { shareViewModel.selectShare(it) },
                onRevokeShare = { shareViewModel.revokeShare(it) },
                onToggleAutoApprove = { shareId, enabled -> shareViewModel.toggleAutoApprove(shareId, enabled) },
                onOpenShareLink = { shareId ->
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://echowithin.xyz/share/note/$shareId")
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            )
        }

        composable(
            route = AppRoute.Versions,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId").orEmpty()
            val versionsViewModel: NoteVersionsViewModel = viewModel(factory = NoteVersionsViewModel.factory())
            LaunchedEffect(noteId) { versionsViewModel.load(noteId) }
            NoteVersionsScreen(
                uiState = versionsViewModel.uiState,
                onBack = { navController.popBackStack() },
                onRestore = { versionsViewModel.restore(it) },
                onDecide = { versionId, approve, comment, autoApproveSubsequent ->
                    versionsViewModel.decide(versionId, approve, comment, autoApproveSubsequent)
                },
                onClearFeedback = { versionsViewModel.clearFeedback() }
            )
        }

        composable(AppRoute.Trash) {
            LaunchedEffect(Unit) { notesViewModel.loadTrash() }
            com.example.echowithin.presentation.screens.TrashScreen(
                trashedNotes = notesViewModel.uiState.trashedNotes,
                onBack = { navController.popBackStack() },
                onRestore = { noteId ->
                    notesViewModel.restoreNote(noteId) {}
                },
                onPermanentDelete = { noteId ->
                    notesViewModel.permanentDeleteNote(noteId) {}
                },
                onEmptyTrash = {
                    notesViewModel.emptyTrash {}
                }
            )
        }

        composable(AppRoute.Search) {
            SearchScreen(
                viewModel = notesViewModel,
                isLocked = appLockViewModel.uiState.isLocked,
                onNoteClick = { noteId -> navController.navigate(AppRoute.detail(noteId)) }
            )
        }

        composable(AppRoute.Premium) {
            PremiumScreen(
                onLoginClick = { navController.navigate(AppRoute.Login) }
            )
        }

        composable(AppRoute.Settings) {
            // A scope so the logout flow can AWAIT the account-data wipe
            // before navigating. Without this the wipe (async) raced the
            // navigation and the Login screen could briefly show the
            // account's notes before they were deleted.
            val settingsScope = androidx.compose.runtime.rememberCoroutineScope()
            SettingsScreen(
                onLogout = {
                    authViewModel.logout {
                        // Runs on AuthViewModel's viewModelScope coroutine.
                        // Hop onto the Settings screen's scope so we can
                        // await the (suspend) account-data clear, then
                        // navigate once the DB is actually clean.
                        settingsScope.launch {
                            notesViewModel.clearAccountData()
                            navController.navigate(AppRoute.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                },
                onLoginClick = {
                    navController.navigate(AppRoute.Login)
                },
                onAppLockClick = { navController.navigate(AppRoute.AppLock) },
                onWebsiteClick = {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://echowithin.xyz")
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                onCheckForUpdates = { notesViewModel.checkForUpdates(context, showToastIfLatest = true) },
                updateInfo = notesViewModel.uiState.updateInfo,
                downloadProgress = notesViewModel.uiState.downloadProgress,
                onConfirmUpdate = { notesViewModel.downloadAndInstallUpdate(context, notesViewModel.uiState.updateInfo?.apkUrl ?: "") },
                onDismissUpdate = { notesViewModel.dismissUpdate() },
                onSortOrderChanged = { notesViewModel.setSortOrder(it) }
            )
        }
    }
}
