package com.example.locationtracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.locationtracker.R
import com.example.locationtracker.settings.SettingsRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun MapPickerDialog(
    title: String,
    initialLatLng: LatLng?,
    onConfirm: (LatLng) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val darkMode by remember { SettingsRepository(context) }
        .darkMode.collectAsState(initial = false)
    val mapStyle = remember(darkMode) {
        if (darkMode) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
        else null
    }

    var pickedLocation by remember { mutableStateOf(initialLatLng) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            initialLatLng ?: LatLng(20.0, 0.0),
            if (initialLatLng != null) 14f else 2f
        )
    }

    // If no initial location was provided, center on the device's current position
    if (initialLatLng == null) {
        val fusedLocationClient = remember {
            LocationServices.getFusedLocationProviderClient(context)
        }
        LaunchedEffect(Unit) {
            try {
                val cts = CancellationTokenSource()
                val location = suspendCancellableCoroutine { cont ->
                    fusedLocationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                    cont.invokeOnCancellation { cts.cancel() }
                }
                location?.let {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
                    )
                }
            } catch (_: SecurityException) {
                // Permission not granted — stay at default world view
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapStyleOptions = mapStyle),
                onMapClick = { latLng -> pickedLocation = latLng }
            ) {
                pickedLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = title
                    )
                }
            }

            // Instruction card at the top
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Tap the map to set $title",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            // Coordinate readout + action buttons at the bottom
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (pickedLocation != null) {
                        Text(
                            text = "%.6f, %.6f".format(
                                pickedLocation!!.latitude, pickedLocation!!.longitude
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }

                        if (pickedLocation != null) {
                            OutlinedButton(
                                onClick = { pickedLocation = null },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Clear") }
                        }

                        Button(
                            onClick = { pickedLocation?.let { onConfirm(it) } },
                            enabled = pickedLocation != null,
                            modifier = Modifier.weight(1f)
                        ) { Text("Confirm") }
                    }
                }
            }
        }
    }
}
