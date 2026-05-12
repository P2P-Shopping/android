package p2ps.android.location

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import p2ps.android.ApiClient
import p2ps.android.MainActivity
import p2ps.android.R
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.DeviceIdManager
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing
import p2ps.android.fcm.FcmTokenManager
import p2ps.android.proximity.ProximityClient
import p2ps.android.proximity.data.ProximityPing
import java.util.UUID

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val CHANNEL_ID = "location_tracking_channel"
    private val NOTIFICATION_ID = 12345

    private lateinit var telemetryManager: TelemetryManager
    private lateinit var telemetryDispatcher: TelemetryDispatcher
    private val proximityClient = ProximityClient()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentDeviceId = "unknown"
    private var currentStoreId = "unknown"
    private var currentItemId = "unknown"
    
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var isMoving = true
    private var lastMoveTime = 0L
    private val INTERVAL_MOVING = 5000L
    private val INTERVAL_STATIONARY = 30000L
    private var currentInterval = INTERVAL_MOVING
    private var lastSentLocation: Location? = null

    override fun onCreate() {
        super.onCreate()

        telemetryManager = TelemetryManager(this)
        val database = p2ps.android.data.AppDatabase.getDatabase(this)
        telemetryDispatcher = TelemetryDispatcher(database.telemetryDao(), this)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processNewLocation(location)
                }
            }
        }

        createNotificationChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            currentInterval
        ).apply {
            setMinUpdateDistanceMeters(2.0f) // Request updates every 2 meters
            setGranularity(Granularity.GRANULARITY_FINE)
            setWaitForAccurateLocation(true)
        }.build()

        try {
            val isRttSupported = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)
            Log.d("TelemetryService", "Starting updates. WiFi RTT Supported: $isRttSupported")

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Missing permissions", e)
        }
    }

    private fun processNewLocation(location: Location) {
        // 1. Movement Filter: Only send if moved more than 2 meters
        val distance = lastSentLocation?.distanceTo(location) ?: Float.MAX_VALUE
        
        // Also check if the device reports it's actually moving (or first fix)
        val isLocallyMoving = distance >= 2.0f

        if (!isLocallyMoving && lastSentLocation != null) {
            Log.d("TelemetryService", "Skipping ping: stationary (< 2m). Dist: ${String.format("%.2f", distance)}m")
            return
        }

        Log.d("TelemetryService", "New telemetry ping generated: Dist: ${String.format("%.2f", distance)}m, Accuracy: ${location.accuracy}m")
        
        val ping = TelemetryPing(
            deviceId = currentDeviceId,
            storeId = currentStoreId,
            itemId = currentItemId,
            triggerType = "BACKGROUND",
            lat = location.latitude,
            lng = location.longitude,
            accuracyMeters = location.accuracy,
            timestamp = System.currentTimeMillis(),
            pingId = UUID.randomUUID().toString()
        )

        lastSentLocation = location

        serviceScope.launch {
            telemetryDispatcher.dispatch(ping)
        }

        // Also fire a proximity ping so the backend can match nearby active lists
        // and trigger an FCM notification. Skipped silently if no FCM token is available yet
        // (e.g. first launch before Firebase finishes registering).
        val fcmToken = FcmTokenManager.getStoredToken(applicationContext)
        if (!fcmToken.isNullOrBlank()) {
            serviceScope.launch {
                proximityClient.sendPing(
                    ProximityPing(
                        deviceId = currentDeviceId,
                        lat = location.latitude,
                        lng = location.longitude,
                        timestamp = System.currentTimeMillis(),
                        fcmToken = fcmToken
                    )
                )
            }
        } else {
            Log.d("TelemetryService", "Skipping proximity ping — FCM token not yet available")
        }
    }

    private fun createNotification(): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("P2P Shopping")
            .setContentText("Telemetry service is active")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return START_NOT_STICKY
        }
        intent?.let {
            currentDeviceId = it.getStringExtra("EXTRA_DEVICE_ID") ?: DeviceIdManager.getDeviceId(this)
            currentStoreId = it.getStringExtra("EXTRA_STORE_ID") ?: "unknown"
            currentItemId = it.getStringExtra("EXTRA_ITEM_ID") ?: "unknown"
        }
        val notification = createNotification()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else 0
        )

        startLocationUpdates()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val channel = NotificationChannel(
                CHANNEL_ID,
                "P2P Telemetry Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location tracking for telemetry"
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val movingNow = calculateIsMoving(x, y, z)
            val currentTime = System.currentTimeMillis()
            val moveDebounceMs = 2000L

            if (movingNow) {
                if (lastMoveTime == 0L) lastMoveTime = currentTime
                if (!isMoving && currentTime - lastMoveTime > moveDebounceMs) {
                    isMoving = true
                    Log.d("TelemetryService", "Motion detected – switching to 5s")
                    updateLocationInterval()
                }
                if (isMoving) lastMoveTime = currentTime
            } else {
                if (isMoving && (currentTime - lastMoveTime > moveDebounceMs)) {
                    isMoving = false
                    Log.d("TelemetryService", "Stationary – switching to 30s")
                    updateLocationInterval()
                } else if (!isMoving) {
                    lastMoveTime = currentTime 
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not needed for this implementation
        }
    }

    private fun updateLocationInterval() {
        val newInterval = if (isMoving) INTERVAL_MOVING else INTERVAL_STATIONARY

        if (currentInterval == newInterval) return

        currentInterval = newInterval
        Log.d("TelemetryService", "Interval changed to: ${currentInterval/1000}s")

        fusedLocationClient.removeLocationUpdates(locationCallback)
        startLocationUpdates()
    }

    fun calculateIsMoving(x: Float, y: Float, z: Float): Boolean {
        val magnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble())
        val acceleration = kotlin.math.abs(magnitude - 9.81)
        return acceleration > 0.5
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(sensorListener)
    }
}
