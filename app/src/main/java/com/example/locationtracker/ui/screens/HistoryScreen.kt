package com.example.locationtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.LocationPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val points by AppDatabase.getInstance(context).locationDao()
        .getAllPoints()
        .collectAsState(initial = emptyList())

    if (points.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No location history yet.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(points.reversed()) { point ->
                LocationPointItem(point)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LocationPointItem(point: LocationPoint) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.getDefault()) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            text = sdf.format(Date(point.timestamp)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "%.6f, %.6f".format(point.latitude, point.longitude),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Accuracy: %.1f m".format(point.accuracy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
