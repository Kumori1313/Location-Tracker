package com.example.locationtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    isTracking: Boolean = false,
    onStartTracking: (routeId: Long?) -> Unit = {},
    onStopTracking: () -> Unit = {}
) {
    val context = LocalContext.current
    val routes by AppDatabase.getInstance(context).routeDao()
        .getAllRoutes().collectAsState(initial = emptyList())

    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Clear selection when tracking stops
    if (!isTracking) {
        LaunchedEffect(isTracking) { selectedRoute = null }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Location Tracker", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))

        // Route selector — only shown when routes exist and not currently tracking
        if (routes.isNotEmpty() && !isTracking) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedRoute?.name ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Route (optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = { selectedRoute = null; dropdownExpanded = false }
                    )
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = { selectedRoute = route; dropdownExpanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Show active route label while tracking
        if (isTracking && selectedRoute != null) {
            Text(
                "Route: ${selectedRoute!!.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = {
                if (isTracking) onStopTracking()
                else onStartTracking(selectedRoute?.id)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isTracking) "Stop Tracking" else "Start Tracking")
        }
    }
}
