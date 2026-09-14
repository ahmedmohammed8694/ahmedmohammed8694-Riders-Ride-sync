package com.ridesync.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.ridesync.MainActivity
import com.ridesync.R
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.engine.AdaptiveLocationThrottleEngine
import com.ridesync.engine.KinematicState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class RideTrackingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val throttleEngine = AdaptiveLocationThrottleEngine()

    private val _currentLocation = MutableStateFlow<RiderLocationPing?>(null)
    val currentLocation: StateFlow<RiderLocationPing?> = _currentLocation.asStateFlow()

    private val _dispatchedPings = MutableSharedFlow<RiderLocationPing>(extraBufferCapacity = 64)
    val dispatchedPings: SharedFlow<RiderLocationPing> = _dispatchedPings.asSharedFlow()

    private var activeKinematicState: KinematicState = KinematicState.MODERATE_SPEED
    private var isTrackingStarted = false

    inner class LocalBinder : Binder() {
        fun getService(): RideTrackingService = this@RideTrackingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val speedKmh = location.speed * 3.6f
                val currentTimeMs = System.currentTimeMillis()

                val ping = RiderLocationPing(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speedKmh = speedKmh,
                    bearing = location.bearing,
                    altitude = location.altitude,
                    timestamp = currentTimeMs
                )

                _currentLocation.value = ping

                // Evaluate adaptive kinematic throttling state
                val newState = throttleEngine.evaluateKinematicState(speedKmh, currentTimeMs)
                if (newState != activeKinematicState) {
                    activeKinematicState = newState
                    reconfigureLocationRequest(newState)
                }

                // Check dispatch criteria (displacement or max dispatch interval)
                if (throttleEngine.shouldDispatchPing(ping)) {
                    serviceScope.launch {
                        _dispatchedPings.emit(ping)
                    }
                    updateNotification(speedKmh, newState)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTrackingForeground()
            ACTION_STOP_TRACKING -> stopTrackingForeground()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTrackingForeground() {
        if (isTrackingStarted) return
        isTrackingStarted = true

        val notification = buildNotification(speedKmh = 0f, state = activeKinematicState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        reconfigureLocationRequest(activeKinematicState)
        Log.d(TAG, "RideTrackingService started in foreground")
    }

    @SuppressLint("MissingPermission")
    private fun reconfigureLocationRequest(state: KinematicState) {
        if (!isTrackingStarted) return

        fusedLocationClient.removeLocationUpdates(locationCallback)

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            state.gpsIntervalMs
        ).apply {
            setMinUpdateIntervalMillis(state.minUpdateIntervalMs)
            setWaitForAccurateLocation(false)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Location request reconfigured for state: $state (${state.gpsIntervalMs}ms)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing location permission for service", e)
        }
    }

    private fun stopTrackingForeground() {
        isTrackingStarted = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "RideTrackingService stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RideSync Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification for real-time motorcycle convoy location tracking"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(speedKmh: Float, state: KinematicState) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(speedKmh, state))
    }

    private fun buildNotification(speedKmh: Float, state: KinematicState): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RideTrackingService::class.java).apply { action = ACTION_STOP_TRACKING },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val speedText = String.format("%.0f km/h", speedKmh)
        val stateText = when (state) {
            KinematicState.HIGH_SPEED -> "Cruising"
            KinematicState.MODERATE_SPEED -> "Riding"
            KinematicState.STATIONARY -> "Stationary / Stopped"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RideSync Convoy Active")
            .setContentText("$stateText • $speedText")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "Stop Ride", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val TAG = "RideTrackingService"
        const val CHANNEL_ID = "ride_tracking_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_TRACKING = "com.ridesync.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.ridesync.action.STOP_TRACKING"
    }
}
