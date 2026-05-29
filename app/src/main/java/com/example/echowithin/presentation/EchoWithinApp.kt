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
import com.example.echowithin.ui.theme.BrandOrange
import com.example.echowithin.ui.theme.BrandAmber
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun EchoWithinApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory())
    val notesViewModel: NotesViewModel = viewModel(factory = NotesViewModel.factory())
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Bottom bar is shown on the 4 main home tabs
    val showBottomBar = currentRoute == AppRoute.Home ||
        currentRoute == AppRoute.Search ||
        currentRoute == AppRoute.Premium ||
        currentRoute == AppRoute.Settings

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
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Home,
                        onClick = {
                            if (currentRoute != AppRoute.Home) {
                                navController.navigate(AppRoute.Home) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOrange,
                            selectedTextColor = BrandOrange,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Search,
                        onClick = {
                            if (currentRoute != AppRoute.Search) {
                                navController.navigate(AppRoute.Search) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        label = { Text("Search") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOrange,
                            selectedTextColor = BrandOrange,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Premium,
                        onClick = {
                            if (currentRoute != AppRoute.Premium) {
                                navController.navigate(AppRoute.Premium) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Premium"
                            )
                        },
                        label = { Text("Premium") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOrange,
                            selectedTextColor = BrandOrange,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Settings,
                        onClick = {
                            if (currentRoute != AppRoute.Settings) {
                                navController.navigate(AppRoute.Settings) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
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
    ) { innerPadding ->
        AppNavGraph(
            navController = navController,
            innerPadding = innerPadding,
            authViewModel = authViewModel,
            notesViewModel = notesViewModel
        )
    }
}
