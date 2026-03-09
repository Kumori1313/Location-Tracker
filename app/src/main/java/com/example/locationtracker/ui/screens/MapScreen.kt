package com.example.locationtracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.locationtracker.R
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.Route
import com.example.locationtracker.database.entities.Session
import com.example.locationtracker.settings.SettingsRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    isTracking: Boolean = false,
    onStartTracking: (routeId: Long?) -> Unit = {},
    onStopTracking: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)

    val allPoints by db.locationDao().getAllPoints().collectAsState(initial = emptyList())
    val sessions by db.sessionDao().getAllCompletedByDate().collectAsState(initial = emptyList())
    val routes by db.routeDao().getAllRoutes().collectAsState(initial = emptyList())

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val darkMode by remember { SettingsRepository(context) }
        .darkMode.collectAsState(initial = false)
    val mapStyle = remember(darkMode) {
        if (darkMode) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
        else null
    }

    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var sessionDropdownExpanded by remember { mutableStateOf(false) }
    var hasCenteredCamera by remember { mutableStateOf(false) }
    var activeRoute by remember { mutableStateOf<Route?>(null) }

    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    var routeDropdownExpanded by remember { mutableStateOf(false) }

    // Clear route selection when tracking stops
    LaunchedEffect(isTracking) { if (!isTracking) selectedRoute = null }

    // Load route associated with the selected session
    LaunchedEffect(selectedSessionId, sessions) {
        val routeId = sessions.firstOrNull { it.id == selectedSessionId }?.routeId
        activeRoute = routeId?.let { db.routeDao().getById(it) }
    }

    val displayedPoints = remember(allPoints, selectedSessionId) {
        if (selectedSessionId == null) allPoints
        else allPoints.filter { it.sessionId == selectedSessionId }
    }
    val latLngPoints = remember(displayedPoints) {
        displayedPoints.map { LatLng(it.latitude, it.longitude) }
    }
    val lastPoint = displayedPoints.lastOrNull()

    val cameraPositionState = rememberCameraPositionState {
        lastPoint?.let {
            position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 15f)
        }
    }

    // Reset centering whenever the selected session changes
    LaunchedEffect(selectedSessionId) { hasCenteredCamera = false }

    // Only center once per session selection — preserves user zoom/pan after that
    LaunchedEffect(lastPoint, hasCenteredCamera) {
        if (lastPoint != null && !hasCenteredCamera) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(lastPoint.latitude, lastPoint.longitude), 15f)
            )
            hasCenteredCamera = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(
                bottom = if (routes.isNotEmpty() && !isTracking) 160.dp else 84.dp
            ),
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapStyleOptions = mapStyle
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            if (latLngPoints.size >= 2) {
                Polyline(points = latLngPoints, color = Color.Blue, width = 8f)
            }
            lastPoint?.let {
                Marker(
                    state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                    title = "Latest fix"
                )
            }

            // Route start/end markers
            activeRoute?.let { route ->
                if (route.startLat != null && route.startLng != null) {
                    Marker(
                        state = MarkerState(LatLng(route.startLat, route.startLng)),
                        title = "Start: ${route.name}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }
                if (route.endLat != null && route.endLng != null) {
                    Marker(
                        state = MarkerState(LatLng(route.endLat, route.endLng)),
                        title = "End: ${route.name}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                    Circle(
                        center = LatLng(route.endLat, route.endLng),
                        radius = route.arrivalRadiusMeters.toDouble(),
                        strokeColor = Color.Red,
                        fillColor = Color.Red.copy(alpha = 0.12f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Session selector overlay — top start
        if (sessions.isNotEmpty()) {
            OutlinedCard(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                TextButton(onClick = { sessionDropdownExpanded = true }) {
                    Text(sessionLabel(selectedSessionId, sessions))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = sessionDropdownExpanded,
                    onDismissRequest = { sessionDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Sessions") },
                        onClick = {
                            selectedSessionId = null
                            sessionDropdownExpanded = false
                        }
                    )
                    sessions.forEach { session ->
                        DropdownMenuItem(
                            text = { Text(formatSessionDate(session)) },
                            onClick = {
                                selectedSessionId = session.id
                                sessionDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Zoom controls — top end (below the my-location button)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 100.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilledTonalIconButton(onClick = {
                cameraPositionState.move(com.google.android.gms.maps.CameraUpdateFactory.zoomIn())
            }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom in")
            }
            FilledTonalIconButton(onClick = {
                cameraPositionState.move(com.google.android.gms.maps.CameraUpdateFactory.zoomOut())
            }) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom out")
            }
        }

        // Tracking controls — bottom overlay
        OutlinedCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (routes.isNotEmpty() && !isTracking) {
                    ExposedDropdownMenuBox(
                        expanded = routeDropdownExpanded,
                        onExpandedChange = { routeDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRoute?.name ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Route (optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(routeDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = routeDropdownExpanded,
                            onDismissRequest = { routeDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = { selectedRoute = null; routeDropdownExpanded = false }
                            )
                            routes.forEach { route ->
                                DropdownMenuItem(
                                    text = { Text(route.name) },
                                    onClick = { selectedRoute = route; routeDropdownExpanded = false }
                                )
                            }
                        }
                    }
                }

                if (isTracking && selectedRoute != null) {
                    Text(
                        "Route: ${selectedRoute!!.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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
    }
}

private fun sessionLabel(selectedId: Long?, sessions: List<Session>): String {
    if (selectedId == null) return "All Sessions"
    val session = sessions.firstOrNull { it.id == selectedId } ?: return "All Sessions"
    return formatSessionDate(session)
}

private fun formatSessionDate(session: Session): String {
    val sdf = SimpleDateFormat("MMM dd  HH:mm", Locale.getDefault())
    return sdf.format(Date(session.startTime))
}
