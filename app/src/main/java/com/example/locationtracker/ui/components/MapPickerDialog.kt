package com.example.locationtracker.ui.components

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    val darkMode by remember { SettingsRepository(context) }
        .darkMode.collectAsState(initial = false)
    val mapStyle = remember(darkMode) {
        if (darkMode) MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
        else null
    }

    var pickedLocation by remember { mutableStateOf(initialLatLng) }
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val placesClient = remember {
        try {
            if (Places.isInitialized()) Places.createClient(context) else null
        } catch (_: Exception) { null }
    }

    // Stable MarkerState outside GoogleMap content lambda (no remember inside @GoogleMapComposable)
    val pickedMarkerState = remember(pickedLocation) {
        pickedLocation?.let { MarkerState(position = it) }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            initialLatLng ?: LatLng(20.0, 0.0),
            if (initialLatLng != null) 14f else 2f
        )
    }

    // Fetch device location once — used for autocomplete proximity bias and
    // initial camera centering when no existing coordinate was provided
    LaunchedEffect(Unit) {
        try {
            val cts = CancellationTokenSource()
            val location = suspendCancellableCoroutine<Location?> { cont ->
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
            location?.let { loc ->
                val latLng = LatLng(loc.latitude, loc.longitude)
                currentLocation = latLng
                if (initialLatLng == null) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(latLng, 16f)
                    )
                }
            }
        } catch (_: SecurityException) {}
    }

    // Debounced Places Autocomplete
    LaunchedEffect(searchText) {
        if (searchText.length < 2 || placesClient == null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        try {
            val requestBuilder = FindAutocompletePredictionsRequest.builder()
                .setQuery(searchText)
            currentLocation?.let { loc ->
                requestBuilder
                    .setLocationBias(
                        RectangularBounds.newInstance(
                            LatLng(loc.latitude - 0.5, loc.longitude - 0.5),
                            LatLng(loc.latitude + 0.5, loc.longitude + 0.5)
                        )
                    )
                    .setOrigin(loc)
            }
            placesClient.findAutocompletePredictions(requestBuilder.build())
                .addOnSuccessListener { resp ->
                    try {
                        suggestions = resp.autocompletePredictions.take(5)
                    } catch (_: Exception) {
                        suggestions = emptyList()
                    }
                }
                .addOnFailureListener {
                    suggestions = emptyList()
                }
        } catch (_: Exception) {
            suggestions = emptyList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapStyleOptions = mapStyle),
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                onMapClick = { latLng ->
                    pickedLocation = latLng
                    searchText = ""
                    suggestions = emptyList()
                }
            ) {
                pickedMarkerState?.let {
                    Marker(state = it, title = title)
                }
            }

            // Zoom controls — top right
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 8.dp),
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

            // Search bar + suggestion list
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search or tap map to set $title") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (searchText.isNotEmpty()) ({
                            IconButton(onClick = { searchText = ""; suggestions = emptyList() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }) else null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(suggestions) { prediction ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val fetchRequest = FetchPlaceRequest.newInstance(
                                                prediction.placeId,
                                                listOf(Place.Field.LAT_LNG)
                                            )
                                            placesClient?.fetchPlace(fetchRequest)
                                                ?.addOnSuccessListener { resp ->
                                                    try {
                                                        resp.place.latLng?.let { latLng ->
                                                            pickedLocation = latLng
                                                            searchText = prediction
                                                                .getPrimaryText(null).toString()
                                                            suggestions = emptyList()
                                                            scope.launch {
                                                                cameraPositionState.animate(
                                                                    CameraUpdateFactory.newLatLngZoom(
                                                                        latLng, 16f
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    } catch (_: Exception) {
                                                        suggestions = emptyList()
                                                    }
                                                }
                                                ?.addOnFailureListener {
                                                    suggestions = emptyList()
                                                }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        prediction.getPrimaryText(null).toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        prediction.getSecondaryText(null).toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Coordinate readout + action buttons
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 45.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
