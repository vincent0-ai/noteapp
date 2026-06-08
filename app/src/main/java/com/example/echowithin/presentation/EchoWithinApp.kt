package com.example.echowithin.presentation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.echowithin.presentation.navigation.AppNavGraph
import com.example.echowithin.presentation.navigation.AppRoute
import com.example.echowithin.presentation.viewmodel.AuthViewModel
import com.example.echowithin.presentation.viewmodel.NotesViewModel
import androidx.compose.runtime.LaunchedEffect
import com.example.echowithin.data.network.ApiClient
import com.example.echowithin.data.network.SessionManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.echowithin.data.network.NetworkMonitor

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

    // Wire the global 401 interceptor: clear session and redirect to Welcome
    LaunchedEffect(navController) {
        ApiClient.onUnauthorized = {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                SessionManager.clear()
                notesViewModel.clearLocalData()
                navController.navigate(AppRoute.Welcome) {
                    popUpTo(0) { inclusive = true }
                }
                ApiClient.isHandlingUnauthorized.set(false)
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomTabs.forEach { route ->
                        val (icon, label, selected) = when (route) {
                            AppRoute.Home -> Triple(Icons.Default.Home, "Home", currentRoute == AppRoute.Home)
                            AppRoute.Search -> Triple(Icons.Default.Search, "Search", currentRoute == AppRoute.Search)
                            AppRoute.Premium -> Triple(Icons.Default.Star, "Premium", currentRoute == AppRoute.Premium)
                            AppRoute.Settings -> Triple(Icons.Default.Settings, "Settings", currentRoute == AppRoute.Settings)
                            else -> throw IllegalArgumentException("Unknown route: $route")
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(imageVector = icon, contentDescription = label)
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandOrange,
                                selectedTextColor = BrandOrange,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            innerPadding = innerPadding,
            authViewModel = authViewModel,
            notesViewModel = notesViewModel
        )
    }
}
