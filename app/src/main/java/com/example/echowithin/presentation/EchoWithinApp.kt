package com.example.echowithin.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import com.example.echowithin.data.network.ApiClient
import com.example.echowithin.data.network.NetworkMonitor
import com.example.echowithin.data.network.SessionManager
import com.example.echowithin.presentation.navigation.AppNavGraph
import com.example.echowithin.presentation.navigation.AppRoute
import com.example.echowithin.presentation.viewmodel.AuthViewModel
import com.example.echowithin.presentation.viewmodel.NotesViewModel
import com.example.echowithin.ui.theme.BrandOrange

@Composable
fun EchoWithinApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory())
    val notesViewModel: NotesViewModel = viewModel(factory = NotesViewModel.factory())
    val context = LocalContext.current

    // Register the network monitor as early as possible so the very first
    // frame already shows the correct offline/online state.
    LaunchedEffect(Unit) { NetworkMonitor.ensureRegistered(context) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Check if user is in offline mode (no auth token)
    val isOfflineMode = com.example.echowithin.data.network.SessionManager.token.isNullOrBlank() ||
        com.example.echowithin.data.network.SessionManager.token == "null"
    
    // Bottom bar tabs: offline mode hides Premium (requires server)
    val bottomTabs = if (isOfflineMode) {
        listOf(AppRoute.Home, AppRoute.Search, AppRoute.Settings)
    } else {
        listOf(AppRoute.Home, AppRoute.Search, AppRoute.Premium, AppRoute.Settings)
    }
    
    // Bottom bar is shown on the main home tabs
    val showBottomBar = bottomTabs.contains(currentRoute)

    // Wire the global 401 interceptor: clear session and redirect to Welcome.
    // Uses clearSession() (preserves the device-local PIN hash + sync-mode +
    // dismissed-update flags) and clearOfflineData() (preserves genuine
    // offline-only notes) so a session death behaves like a sign-out: the
    // account token is gone and account-synced content is removed, but the
    // user can still unlock notes offline with their PIN and recover their
    // offline notes on re-login.
    LaunchedEffect(navController) {
        ApiClient.onUnauthorized = {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                SessionManager.clearSession()
                notesViewModel.clearOfflineData()
                navController.navigate(AppRoute.Welcome) {
                    popUpTo(0) { inclusive = true }
                }
                ApiClient.isHandlingUnauthorized.set(false)
            }
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                androidx.compose.foundation.layout.Column {
                    // Subtle top divider
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        bottomTabs.forEach { route ->
                            val (icon, label, selected) = when (route) {
                                AppRoute.Home -> Triple(Icons.Default.Home, "Home", currentRoute == AppRoute.Home)
                                AppRoute.Search -> Triple(Icons.Default.Search, "Search", currentRoute == AppRoute.Search)
                                AppRoute.Premium -> Triple(Icons.Default.Star, "Premium", currentRoute == AppRoute.Premium)
                                AppRoute.Settings -> Triple(Icons.Default.Settings, "Settings", currentRoute == AppRoute.Settings)
                                else -> throw IllegalArgumentException("Unknown route: $route")
                            }
                            val tint = if (selected) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                modifier = androidx.compose.ui.Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (!selected) {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = tint,
                                    modifier = androidx.compose.ui.Modifier.size(22.dp)
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                    color = tint,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.ime)
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            innerPadding = innerPadding,
            authViewModel = authViewModel,
            notesViewModel = notesViewModel
        )
    }
}
