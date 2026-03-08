package com.example.locationtracker.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.locationtracker.MainActivity
import com.example.locationtracker.database.AppDatabase
import com.example.locationtracker.database.entities.LocationPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var repository: LocationRepository
    private lateinit var db: AppDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var currentSessionId: Long = -1L
    @Volatile private var sessionStartTime: Long = 0L
    @Volatile private var currentRouteId: Long? = null
    @Volatile private var routeEndLat: Double? = null
    @Volatile private var routeEndLng: Double? = null
    @Volatile private var routeArrivalRadius: Float = 50f
    @Volatile private var hasArrived: Boolean = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        db = AppDatabase.getInstance(this)
        repository = LocationRepository(db.locationDao(), db.sessionDao())
        createArrivalNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    serviceScope.launch {
                        repository.save(
                            LocationPoint(
                                sessionId = currentSessionId.takeIf { it > 0 },
                                latitude = location.latitude,
                                longitude = location.longitude,
                                timestamp = location.time,
                                accuracy = location.accuracy
                            )
                        )

                        // Auto-stop if within arrival radius of the route end point
                        val endLat = routeEndLat
                        val endLng = routeEndLng
                        if (!hasArrived && endLat != null && endLng != null) {
                            val dist = haversineMeters(
                                location.latitude, location.longitude, endLat, endLng
                            )
                            if (dist <= routeArrivalRadius) {
                                hasArrived = true
                                onArrived()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, DEFAULT_INTERVAL_MS)
                val routeId = if (intent.hasExtra(EXTRA_ROUTE_ID))
                    intent.getLongExtra(EXTRA_ROUTE_ID, -1L).takeIf { it > 0 }
                else null
                startTracking(intervalMs, routeId)
            }
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking(intervalMs: Long, routeId: Long? = null) {
        currentRouteId = routeId
        hasArrived = false
        routeEndLat = null
        routeEndLng = null
        routeArrivalRadius = 50f
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        sessionStartTime = System.currentTimeMillis()
        serviceScope.launch {
            currentSessionId = repository.startSession(sessionStartTime, currentRouteId)
            currentRouteId?.let { id ->
                val route = db.routeDao().getById(id)
                routeEndLat = route?.endLat
                routeEndLng = route?.endLng
                routeArrivalRadius = route?.arrivalRadiusMeters ?: 50f
            }
        }

        val minIntervalMs = (intervalMs / 2).coerceAtLeast(1_000L)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(minIntervalMs)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val sessionId = currentSessionId
        val startTime = sessionStartTime
        if (sessionId > 0) {
            serviceScope.launch {
                repository.closeSession(sessionId, startTime, System.currentTimeMillis())
            }
            currentSessionId = -1L
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun onArrived() {
        showArrivalNotification()
        sendBroadcast(Intent(ACTION_ARRIVED).apply { setPackage(packageName) })
        stopTracking()
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    private fun showArrivalNotification() {
        val notification = NotificationCompat.Builder(this, ARRIVAL_CHANNEL_ID)
            .setContentTitle("You've arrived!")
            .setContentText("Tracking stopped automatically.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(ARRIVAL_NOTIFICATION_ID, notification)
    }

    private fun createArrivalNotificationChannel() {
        val channel = NotificationChannel(
            ARRIVAL_CHANNEL_ID,
            "Arrival Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracker")
            .setContentText("Tracking your location...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_ARRIVED = "com.example.locationtracker.ACTION_ARRIVED"
        const val EXTRA_INTERVAL_MS = "extra_interval_ms"
        const val EXTRA_ROUTE_ID = "extra_route_id"
        const val DEFAULT_INTERVAL_MS = 10_000L
        private const val NOTIFICATION_ID = 1
        private const val ARRIVAL_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "location_tracking"
        private const val ARRIVAL_CHANNEL_ID = "arrival_alerts"
    }
}
