package com.example.locationtracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.LocationPoint
import com.example.locationtracker.database.entities.Session
import com.example.locationtracker.database.entities.SessionWithPointCount
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val scope = rememberCoroutineScope()

    val sessionsWithCount by db.sessionDao()
        .getSessionsWithPointCount()
        .collectAsState(initial = emptyList())
    val allPoints by db.locationDao()
        .getAllPoints()
        .collectAsState(initial = emptyList())

    val legacyPoints = remember(allPoints) { allPoints.filter { it.sessionId == null } }
    var expandedSessionId by remember { mutableStateOf<Long?>(null) }

    if (sessionsWithCount.isEmpty() && legacyPoints.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No location history yet.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (sessionsWithCount.isNotEmpty()) {
            item {
                RouteStatsHeader(sessionsWithCount)
                HorizontalDivider(thickness = 2.dp)
            }
        }

        items(sessionsWithCount, key = { it.session.id }) { swc ->
            val isExpanded = expandedSessionId == swc.session.id
            SessionCard(
                swc = swc,
                isExpanded = isExpanded,
                expandedPoints = if (isExpanded) {
                    allPoints.filter { it.sessionId == swc.session.id }
                } else emptyList(),
                onExpand = {
                    expandedSessionId = if (isExpanded) null else swc.session.id
                },
                onRename = { newName ->
                    scope.launch {
                        db.sessionDao().updateName(swc.session.id, newName)
                    }
                },
                onDelete = {
                    scope.launch {
                        db.locationDao().deleteBySessionId(swc.session.id)
                        db.sessionDao().deleteById(swc.session.id)
                    }
                    if (expandedSessionId == swc.session.id) expandedSessionId = null
                }
            )
            HorizontalDivider()
        }

        if (legacyPoints.isNotEmpty()) {
            item {
                Text(
                    text = "Untracked Points",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                HorizontalDivider(thickness = 2.dp)
            }
            items(legacyPoints.reversed()) { point ->
                LocationPointItem(point)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RouteStatsHeader(sessionsWithCount: List<SessionWithPointCount>) {
    val sessions = sessionsWithCount.map { it.session }
    val fastest = sessions.minByOrNull { it.durationMs }
    val averageMs = sessions.map { it.durationMs }.average().toLong()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        StatChip(label = "Sessions", value = "${sessions.size}")
        fastest?.let { StatChip(label = "Fastest", value = formatDuration(it.durationMs)) }
        StatChip(label = "Average", value = formatDuration(averageMs))
    }
}

@Composable
private fun SessionCard(
    swc: SessionWithPointCount,
    isExpanded: Boolean,
    expandedPoints: List<LocationPoint>,
    onExpand: () -> Unit,
    onRename: (String?) -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault()) }
    val displayName = swc.session.name ?: sdf.format(Date(swc.session.startTime))

    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember(swc.session.name) {
        mutableStateOf(swc.session.name ?: "")
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Session") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Session name") },
                    placeholder = { Text(sdf.format(Date(swc.session.startTime))) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRename(renameText.trim().ifEmpty { null })
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session") },
            text = { Text("\"$displayName\" and all its recorded points will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand)
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${formatDuration(swc.session.durationMs)}  •  ${swc.pointCount} points",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                              else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Session options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuExpanded = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                expandedPoints.forEach { point ->
                    LocationPointItem(point, indented = true)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LocationPointItem(point: LocationPoint, indented: Boolean = false) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.getDefault()) }
    Column(
        modifier = Modifier.padding(
            start = if (indented) 32.dp else 16.dp,
            end = 16.dp, top = 8.dp, bottom = 8.dp
        )
    ) {
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

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
