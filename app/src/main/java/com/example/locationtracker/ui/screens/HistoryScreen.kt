package com.example.locationtracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.example.locationtracker.settings.SettingsRepository
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.LocationPoint
import com.example.locationtracker.database.entities.Session
import com.example.locationtracker.database.entities.SessionWithPointCount
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
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

    val repo = remember { SettingsRepository(context) }
    val dateFilterRangeMode by repo.dateFilterRangeMode.collectAsState(initial = true)

    // Filter state
    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    val datePickerState = rememberDatePickerState()
    var filterStartMs by remember { mutableStateOf<Long?>(null) }
    var filterEndMs by remember { mutableStateOf<Long?>(null) }
    var filterMinText by remember { mutableStateOf("") }
    var filterMaxText by remember { mutableStateOf("") }
    val filterMinMs = remember(filterMinText) { filterMinText.toIntOrNull()?.let { it * 60_000L } }
    val filterMaxMs = remember(filterMaxText) { filterMaxText.toIntOrNull()?.let { it * 60_000L } }

    val nameSdf = remember { SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault()) }

    val filteredSessions = remember(
        sessionsWithCount, searchQuery, filterStartMs, filterEndMs, filterMinMs, filterMaxMs
    ) {
        sessionsWithCount.filter { swc ->
            val s = swc.session
            val displayName = s.name ?: nameSdf.format(Date(s.startTime))
            val matchesSearch = searchQuery.isBlank() ||
                displayName.contains(searchQuery, ignoreCase = true)
            val matchesStart = filterStartMs == null || s.startTime >= filterStartMs!!
            val matchesEnd = filterEndMs == null || s.startTime <= filterEndMs!!
            val matchesMin = filterMinMs == null || s.durationMs >= filterMinMs!!
            val matchesMax = filterMaxMs == null || s.durationMs <= filterMaxMs!!
            matchesSearch && matchesStart && matchesEnd && matchesMin && matchesMax
        }
    }

    var expandedSessionId by remember { mutableStateOf<Long?>(null) }
    val dateFilterActive = filterStartMs != null || filterEndMs != null

    // Date picker dialog — range or single depending on setting
    if (showDatePicker) {
        if (dateFilterRangeMode) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        filterStartMs = dateRangePickerState.selectedStartDateMillis
                            ?.let { utcMidnightToLocalMidnight(it) }
                        filterEndMs = dateRangePickerState.selectedEndDateMillis
                            ?.let { utcMidnightToLocalMidnight(it) + 86_399_999L }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DateRangePicker(state = dateRangePickerState, modifier = Modifier.weight(1f))
            }
        } else {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { ms ->
                            filterStartMs = utcMidnightToLocalMidnight(ms)
                            filterEndMs = filterStartMs!! + 86_399_999L
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search sessions…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) ({
                IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }) else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // Filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date range chip
            FilterChip(
                selected = dateFilterActive,
                onClick = { showDatePicker = true },
                label = {
                    Text(
                        if (!dateFilterActive) "Date"
                        else if (dateFilterRangeMode) formatDateRange(filterStartMs, filterEndMs)
                        else formatSingleDate(filterStartMs)
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                }
            )
            // Mode toggle
            TextButton(
                onClick = { scope.launch { repo.setDateFilterRangeMode(!dateFilterRangeMode) } },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    if (dateFilterRangeMode) "Range" else "Single",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (dateFilterActive) {
                IconButton(
                    onClick = { filterStartMs = null; filterEndMs = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear date filter",
                        modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            // Duration range inputs
            OutlinedTextField(
                value = filterMinText,
                onValueChange = { filterMinText = it },
                label = { Text("Min") },
                suffix = { Text("m") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(80.dp)
            )
            OutlinedTextField(
                value = filterMaxText,
                onValueChange = { filterMaxText = it },
                label = { Text("Max") },
                suffix = { Text("m") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(80.dp)
            )
        }

        // Content
        if (filteredSessions.isEmpty() && legacyPoints.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (sessionsWithCount.isEmpty()) "No location history yet."
                           else "No sessions match your filters.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (filteredSessions.isNotEmpty()) {
                item {
                    RouteStatsHeader(filteredSessions)
                    HorizontalDivider(thickness = 2.dp)
                }
            }

            items(filteredSessions, key = { it.session.id }) { swc ->
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
                        scope.launch { db.sessionDao().updateName(swc.session.id, newName) }
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
}

// DatePicker returns UTC midnight; convert to the same calendar date's local midnight
private fun utcMidnightToLocalMidnight(utcMs: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
    return Calendar.getInstance().apply {
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatSingleDate(startMs: Long?): String {
    if (startMs == null) return "Date"
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(startMs))
}

private fun formatDateRange(startMs: Long?, endMs: Long?): String {
    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
    val start = startMs?.let { sdf.format(Date(it)) }
    val end = endMs?.let { sdf.format(Date(it)) }
    return when {
        start != null && end != null -> "$start – $end"
        start != null -> "From $start"
        end != null -> "Until $end"
        else -> "Date"
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
                        onClick = { menuExpanded = false; showRenameDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; showDeleteDialog = true }
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
