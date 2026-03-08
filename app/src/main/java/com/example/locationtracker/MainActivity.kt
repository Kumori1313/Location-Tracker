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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.locationtracker.location.LocationService
import com.example.locationtracker.settings.SettingsRepository
import com.example.locationtracker.ui.navigation.AppNavigation
import com.example.locationtracker.ui.theme.LocationTrackerTheme
import com.example.locationtracker.workers.LocationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private var isTracking by mutableStateOf(false)
    private var pendingIntervalMs = SettingsRepository.DEFAULT_INTERVAL_MS
    private var pendingRouteId: Long? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) startLocationService(pendingIntervalMs, pendingRouteId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepo = remember { SettingsRepository(applicationContext) }
            val darkMode by settingsRepo.darkMode.collectAsState(initial = false)
            val intervalMs by settingsRepo.trackingIntervalMs.collectAsState(initial = SettingsRepository.DEFAULT_INTERVAL_MS)

            LocationTrackerTheme(darkTheme = darkMode) {
                AppNavigation(
                    modifier = Modifier.fillMaxSize(),
                    isTracking = isTracking,
                    onStartTracking = { routeId -> onStartTracking(intervalMs, routeId) },
                    onStopTracking = ::onStopTracking
                )
            }
        }
    }

    private fun onStartTracking(intervalMs: Long, routeId: Long?) {
        pendingIntervalMs = intervalMs
        pendingRouteId = routeId
        if (hasLocationPermission()) {
            startLocationService(intervalMs, routeId)
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

    private fun startLocationService(intervalMs: Long, routeId: Long?) {
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            putExtra(LocationService.EXTRA_INTERVAL_MS, intervalMs)
            routeId?.let { putExtra(LocationService.EXTRA_ROUTE_ID, it) }
        }
        startForegroundService(intent)
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
        val request = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES).build()
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
