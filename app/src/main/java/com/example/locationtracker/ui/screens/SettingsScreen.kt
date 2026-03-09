package com.example.locationtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.locationtracker.settings.SettingsRepository
import kotlinx.coroutines.launch

private val INTERVAL_OPTIONS = listOf(
    5_000L to "5 seconds",
    10_000L to "10 seconds",
    30_000L to "30 seconds",
    60_000L to "60 seconds"
)

private val RADIUS_OPTIONS = listOf(
    25f to "25 meters",
    50f to "50 meters",
    100f to "100 meters",
    200f to "200 meters"
)

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val intervalMs by repo.trackingIntervalMs.collectAsState(initial = SettingsRepository.DEFAULT_INTERVAL_MS)
    val darkMode by repo.darkMode.collectAsState(initial = false)
    val showLatestFixMarker by repo.showLatestFixMarker.collectAsState(initial = true)
    val defaultRadius by repo.defaultArrivalRadiusMeters.collectAsState(initial = 50f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Tracking Interval", style = MaterialTheme.typography.titleMedium)
        Text(
            "How often to record your location while tracking is active.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        INTERVAL_OPTIONS.forEach { (ms, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = intervalMs == ms,
                    onClick = { scope.launch { repo.setTrackingInterval(ms) } }
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Dark Mode", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Override the system theme.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = darkMode,
                onCheckedChange = { scope.launch { repo.setDarkMode(it) } }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Show Latest Fix Marker", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Display the blue marker for the most recent GPS point.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = showLatestFixMarker,
                onCheckedChange = { scope.launch { repo.setShowLatestFixMarker(it) } }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Route Defaults", style = MaterialTheme.typography.titleMedium)
        Text(
            "Default arrival radius used when creating new routes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        RADIUS_OPTIONS.forEach { (meters, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = defaultRadius == meters,
                    onClick = { scope.launch { repo.setDefaultArrivalRadius(meters) } }
                )
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }

    }
}
