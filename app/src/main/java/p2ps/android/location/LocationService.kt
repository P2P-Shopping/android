package p2ps.android.location

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.location.*
import p2ps.android.R
import java.util.concurrent.TimeUnit
import p2ps.android.MainActivity
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing
import p2ps.android.ApiClient
import p2ps.android.core.TelemetryDispatcher
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val CHANNEL_ID = "location_tracking_channel"
    private val NOTIFICATION_ID = 12345

    private lateinit var telemetryManager: TelemetryManager
    private lateinit var telemetryDispatcher: TelemetryDispatcher

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


    override fun onCreate() {
        super.onCreate()

        telemetryManager = TelemetryManager(this)

        telemetryDispatcher = TelemetryDispatcher(ApiClient(), telemetryManager)
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
            setMaxUpdateDelayMillis(currentInterval)
            setMinUpdateIntervalMillis(currentInterval)
        }.build()

        try {
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
        android.util.Log.d("TelemetryService", "New telemetry ping generated at ${location.time}")
        val ping = TelemetryPing(
            deviceId = currentDeviceId,
            storeId = currentStoreId,
            itemId = currentItemId,
            triggerType = "BACKGROUND",
            lat = location.latitude,
            lng = location.longitude,
            accuracy = location.accuracy,
            timestamp = System.currentTimeMillis()
        )
        //telemetryManager.savePing(ping)
        telemetryDispatcher.dispatch(ping)


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
            currentDeviceId = it.getStringExtra("EXTRA_DEVICE_ID") ?: "unknown"
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
                setShowBadge(true) //bulina aplicatie
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

            val magnitude = sqrt((x * x + y * y + z * z).toDouble())
            val acceleration = abs(magnitude - 9.81)
            val currentTime = System.currentTimeMillis()

            val movingNow = acceleration > 0.5

            if (movingNow) {
                lastMoveTime = currentTime

                if (!isMoving) {
                    isMoving = true
                    Log.d("TelemetryService", "DETECTAT MIȘCARE - Trecem la 5s")
                    updateLocationInterval()
                }
            } else {
                if (isMoving && (currentTime - lastMoveTime > 2000)) {
                    isMoving = false
                    Log.d("TelemetryService", "STARE REPAUS - Trecem la 30s")
                    updateLocationInterval()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun updateLocationInterval() {
        val newInterval = if (isMoving) INTERVAL_MOVING else INTERVAL_STATIONARY


        if (currentInterval == newInterval) return

        currentInterval = newInterval
        Log.d("TelemetryService", "Interval SCHIMBAT REAL la: ${currentInterval/1000}s")

        fusedLocationClient.removeLocationUpdates(locationCallback)
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(sensorListener)
    }


}