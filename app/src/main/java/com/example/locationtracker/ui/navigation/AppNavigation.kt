package com.example.locationtracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.locationtracker.ui.screens.ExportScreen
import com.example.locationtracker.ui.screens.HistoryScreen
import com.example.locationtracker.ui.screens.MapScreen
import com.example.locationtracker.ui.screens.RoutesScreen
import com.example.locationtracker.ui.screens.SettingsScreen

enum class Screen(val label: String, val icon: ImageVector, val route: String) {
    Map("Map", Icons.Default.LocationOn, "map"),
    History("History", Icons.AutoMirrored.Filled.List, "history"),
    Export("Export", Icons.Default.Share, "export"),
    Routes("Routes", Icons.Default.Route, "routes"),
    Settings("Settings", Icons.Default.Settings, "settings")
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    isTracking: Boolean,
    onStartTracking: (routeId: Long?) -> Unit,
    onStopTracking: () -> Unit
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Map.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    isTracking = isTracking,
                    onStartTracking = onStartTracking,
                    onStopTracking = onStopTracking
                )
            }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Export.route) { ExportScreen() }
            composable(Screen.Routes.route) { RoutesScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
