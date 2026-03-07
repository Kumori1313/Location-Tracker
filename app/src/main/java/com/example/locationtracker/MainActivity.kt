package com.example.locationtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.locationtracker.location.LocationService
import com.example.locationtracker.ui.screens.HomeScreen
import com.example.locationtracker.ui.theme.LocationTrackerTheme
import com.example.locationtracker.workers.LocationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private var isTracking by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) startLocationService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        isTracking = isTracking,
                        onStartTracking = ::onStartTracking,
                        onStopTracking = ::onStopTracking
                    )
                }
            }
        }
    }

    private fun onStartTracking() {
        if (hasLocationPermission()) {
            startLocationService()
        } else {
            val permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            permissionLauncher.launch(permissions)
        }
    }

    private fun onStopTracking() {
        sendActionToService(LocationService.ACTION_STOP)
        cancelPeriodicTracking()
        isTracking = false
    }

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun startLocationService() {
        sendActionToService(LocationService.ACTION_START)
        schedulePeriodicTracking()
        isTracking = true
    }

    private fun sendActionToService(action: String) {
        val intent = Intent(this, LocationService::class.java).apply {
            this.action = action
        }
        startForegroundService(intent)
    }

    private fun schedulePeriodicTracking() {
        val request = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun cancelPeriodicTracking() {
        WorkManager.getInstance(this).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "periodic_location_tracking"
    }
}
