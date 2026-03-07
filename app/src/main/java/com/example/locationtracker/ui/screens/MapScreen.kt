package com.example.locationtracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.locationtracker.database.AppDatabase
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val points by AppDatabase.getInstance(context).locationDao()
        .getAllPoints()
        .collectAsState(initial = emptyList())

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    val lastPoint = points.lastOrNull()
    val latLngPoints = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }

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

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
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
}
