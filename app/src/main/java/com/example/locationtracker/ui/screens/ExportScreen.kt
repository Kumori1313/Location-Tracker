package com.example.locationtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Full implementation (CSV/GPX/JSON export + share) in Phase 9.
@Composable
fun ExportScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Export — implemented in Phase 9", style = MaterialTheme.typography.bodyLarge)
    }
}
