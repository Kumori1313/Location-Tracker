package com.example.locationtracker.workers

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.LocationPoint
import com.example.locationtracker.location.LocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Periodic background location worker — runs every 15 minutes via WorkManager.
// Complements the foreground service: captures a single location fix when the
// foreground service is not active (e.g. after the system has killed the app).
class LocationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!hasLocationPermission()) return Result.failure()

        val location = getCurrentLocation() ?: return Result.retry()

        val db = AppDatabase.getInstance(applicationContext)
        LocationRepository(db.locationDao(), db.sessionDao()).save(
            LocationPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time,
                accuracy = location.accuracy
            )
        )

        return Result.success()
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private suspend fun getCurrentLocation(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            try {
                LocationServices.getFusedLocationProviderClient(applicationContext)
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            } catch (e: SecurityException) {
                cont.resume(null)
            }
        }
}
