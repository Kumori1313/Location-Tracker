package com.example.locationtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Full implementation in Phase 10.
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    isTracking: Boolean = false,
    onStartTracking: () -> Unit = {},
    onStopTracking: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Location Tracker", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = if (isTracking) onStopTracking else onStartTracking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isTracking) "Stop Tracking" else "Start Tracking")
        }
    }
}
