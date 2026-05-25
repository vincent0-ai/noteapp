package com.example.echowithin.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.echowithin.data.network.ApiClient
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
    val startDest = if (SessionManager.token != null) AppRoute.Home else AppRoute.Welcome

    // Shared ViewModels for Home tab usage
    val appLockViewModel: AppLockViewModel = viewModel(factory = AppLockViewModel.factory())

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            ApiClient.registerFcmToken(context)
        } else {
            Log.d("FCM", "Notification permission denied")
        }
    }

    // Validate session then load data on launch
    LaunchedEffect(Unit) {
        if (SessionManager.token != null) {
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
                notesViewModel.loadNotes()
                appLockViewModel.refreshStatus()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401 || e.code() == 403) {
                    // Token is invalid/expired — redirect to Welcome
                    SessionManager.clear()
                    notesViewModel.clearLocalData()
                    navController.navigate(AppRoute.Welcome) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    // Other HTTP error — still load local notes
                    notesViewModel.loadNotes()
                }
            } catch (_: Exception) {
                // Network error — load from local DB only
                notesViewModel.loadNotes()
            }
        } else {
            // No token — load whatever is in local DB
            notesViewModel.loadNotes()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest,
        modifier = Modifier.padding(innerPadding)
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
                    notesViewModel.loadNotes()
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
            HomeScreen(
                notes = notesViewModel.uiState.notes,
                isLoading = notesViewModel.uiState.isLoading,
                isSyncing = notesViewModel.uiState.isSyncing,
                error = notesViewModel.uiState.error,
                onNoteClick = { noteId -> navController.navigate(AppRoute.detail(noteId)) },
                onNewNoteClick = { navController.navigate(AppRoute.editor(noteId = null)) },
                onSyncClick = {
                    if (SessionManager.token.isNullOrBlank() || SessionManager.token == "null") {
                        android.widget.Toast.makeText(context, "Sign in or create an account to sync notes!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        notesViewModel.syncNotes()
                    }
                },
                onRetryClick = { notesViewModel.loadNotes() },
                // Lock-related
                hasPin = appLockViewModel.uiState.hasPin,
                isLocked = appLockViewModel.uiState.isLocked,
                lockError = appLockViewModel.uiState.error,
                lockLoading = appLockViewModel.uiState.isLoading,
                onVerifyPin = { appLockViewModel.verify(it) },
                onSetupPin = { appLockViewModel.setup(it) },
                // Proposals
                proposals = notesViewModel.uiState.proposals,
                proposalsLoading = notesViewModel.uiState.proposalsLoading,
                onApproveProposal = { notesViewModel.approveProposal(it) },
                onRejectProposal = { notesViewModel.rejectProposal(it) },
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
                // Navigation
                onSearchClick = { navController.navigate(AppRoute.Search) }
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
                onRemove = { appLockViewModel.remove() }
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
                onSave = { content, reference, tags ->
                    if (noteId == null) {
                        notesViewModel.createNote(content, reference, tags) {
                            navController.popBackStack()
                        }
                    } else {
                        notesViewModel.editNote(noteId, content, reference, tags) {
                            navController.popBackStack()
                        }
                    }
                }
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
                }
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
                onDecide = { versionId, approve -> versionsViewModel.decide(versionId, approve) }
            )
        }

        composable(AppRoute.Search) {
            SearchScreen(
                viewModel = notesViewModel,
                onNoteClick = { noteId -> navController.navigate(AppRoute.detail(noteId)) }
            )
        }

        composable(AppRoute.Premium) {
            PremiumScreen(
                onLoginClick = { navController.navigate(AppRoute.Login) }
            )
        }

        composable(AppRoute.Settings) {
            SettingsScreen(
                onLogout = {
                    authViewModel.logout {
                        notesViewModel.clearLocalData()
                        navController.navigate(AppRoute.Login) {
                            popUpTo(AppRoute.Home) { inclusive = true }
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
                }
            )
        }
    }
}
