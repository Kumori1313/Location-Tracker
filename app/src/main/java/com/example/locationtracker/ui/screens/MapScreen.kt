package com.example.locationtracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
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
import com.example.locationtracker.database.entities.Session
import com.example.locationtracker.settings.SettingsRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = AppDatabase.getInstance(context)

    val allPoints by db.locationDao().getAllPoints().collectAsState(initial = emptyList())
    val sessions by db.sessionDao().getAllCompletedByDate().collectAsState(initial = emptyList())

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
    var dropdownExpanded by remember { mutableStateOf(false) }

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

    LaunchedEffect(lastPoint) {
        lastPoint?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapStyleOptions = mapStyle
            )
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
        }

        // Session selector overlay
        if (sessions.isNotEmpty()) {
            OutlinedCard(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                TextButton(onClick = { dropdownExpanded = true }) {
                    Text(sessionLabel(selectedSessionId, sessions))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Sessions") },
                        onClick = {
                            selectedSessionId = null
                            dropdownExpanded = false
                        }
                    )
                    sessions.forEach { session ->
                        DropdownMenuItem(
                            text = { Text(formatSessionDate(session)) },
                            onClick = {
                                selectedSessionId = session.id
                                dropdownExpanded = false
                            }
                        )
                    }
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
