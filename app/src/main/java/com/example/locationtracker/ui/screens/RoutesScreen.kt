package com.example.locationtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.Route
import com.example.locationtracker.settings.SettingsRepository
import com.example.locationtracker.ui.components.MapPickerDialog
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

@Composable
fun RoutesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)
    val scope = rememberCoroutineScope()

    val routes by db.routeDao().getAllRoutes().collectAsState(initial = emptyList())
    val defaultRadius by remember { SettingsRepository(context) }
        .defaultArrivalRadiusMeters.collectAsState(initial = 50f)
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingRoute by remember { mutableStateOf<Route?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredRoutes = remember(routes, searchQuery) {
        if (searchQuery.isBlank()) routes
        else routes.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    if (showCreateDialog || editingRoute != null) {
        RouteDialog(
            existing = editingRoute,
            defaultRadiusMeters = defaultRadius,
            onDismiss = { showCreateDialog = false; editingRoute = null },
            onSave = { route ->
                scope.launch {
                    if (editingRoute != null) db.routeDao().update(route)
                    else db.routeDao().insert(route)
                }
                showCreateDialog = false
                editingRoute = null
            }
        )
    }

    if (routes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("No routes yet.", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { showCreateDialog = true }) {
                    Text("Create Route")
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search routes…") },
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

            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredRoutes.isEmpty()) {
                    Text(
                        "No routes match your search.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(filteredRoutes, key = { it.id }) { route ->
                            RouteCard(
                                route = route,
                                onEdit = { editingRoute = route },
                                onDelete = { scope.launch { db.routeDao().deleteById(route.id) } }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create route")
                }
            }
        }
    }
}

@Composable
private fun RouteCard(route: Route, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Route") },
            text = { Text("\"${route.name}\" will be permanently deleted.") },
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(route.name, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            val startText = if (route.startLat != null && route.startLng != null)
                "Start: %.5f, %.5f".format(route.startLat, route.startLng)
            else "Start: not set"
            val endText = if (route.endLat != null && route.endLng != null)
                "End: %.5f, %.5f".format(route.endLat, route.endLng)
            else "End: not set"
            Text(startText, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(endText, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Arrival radius: ${route.arrivalRadiusMeters.toInt()} m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Route options")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { menuExpanded = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; showDeleteDialog = true }
                )
            }
        }
    }
}

@Composable
private fun RouteDialog(
    existing: Route?,
    defaultRadiusMeters: Float,
    onDismiss: () -> Unit,
    onSave: (Route) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var startLat by remember { mutableStateOf(existing?.startLat?.toString() ?: "") }
    var startLng by remember { mutableStateOf(existing?.startLng?.toString() ?: "") }
    var endLat by remember { mutableStateOf(existing?.endLat?.toString() ?: "") }
    var endLng by remember { mutableStateOf(existing?.endLng?.toString() ?: "") }
    var radius by remember {
        mutableStateOf(
            existing?.arrivalRadiusMeters?.toInt()?.toString()
                ?: defaultRadiusMeters.toInt().toString()
        )
    }
    var nameError by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        MapPickerDialog(
            title = "start point",
            initialLatLng = startLat.toDoubleOrNull()?.let { lat ->
                startLng.toDoubleOrNull()?.let { lng -> LatLng(lat, lng) }
            },
            onConfirm = { latLng ->
                startLat = latLng.latitude.toString()
                startLng = latLng.longitude.toString()
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false }
        )
    }

    if (showEndPicker) {
        MapPickerDialog(
            title = "end point",
            initialLatLng = endLat.toDoubleOrNull()?.let { lat ->
                endLng.toDoubleOrNull()?.let { lng -> LatLng(lat, lng) }
            },
            onConfirm = { latLng ->
                endLat = latLng.latitude.toString()
                endLng = latLng.longitude.toString()
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (existing != null) "Edit Route" else "Create Route",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Route Name *") },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text("Name is required") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Start Point (optional)", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                        onClick = { showStartPicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick on Map", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startLat, onValueChange = { startLat = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = startLng, onValueChange = { startLng = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("End Point (optional)", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(
                        onClick = { showEndPicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick on Map", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = endLat, onValueChange = { endLat = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = endLng, onValueChange = { endLng = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }

                OutlinedTextField(
                    value = radius,
                    onValueChange = { radius = it },
                    label = { Text("Arrival Radius (m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (name.isBlank()) { nameError = true; return@Button }
                        onSave(
                            Route(
                                id = existing?.id ?: 0,
                                name = name.trim(),
                                startLat = startLat.toDoubleOrNull(),
                                startLng = startLng.toDoubleOrNull(),
                                endLat = endLat.toDoubleOrNull(),
                                endLng = endLng.toDoubleOrNull(),
                                arrivalRadiusMeters = radius.toFloatOrNull() ?: 50f
                            )
                        )
                    }) { Text("Save") }
                }
            }
        }
    }
}
