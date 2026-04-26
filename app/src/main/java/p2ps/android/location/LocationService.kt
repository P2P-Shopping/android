package p2ps.android.location

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import p2ps.android.R
import java.util.concurrent.TimeUnit
import p2ps.android.MainActivity

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val CHANNEL_ID = "location_tracking_channel"
    private val NOTIFICATION_ID = 12345

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processNewLocation(location)
                }
            }
        }

        createNotificationChannel()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.SECONDS.toMillis(10)
        ).apply {
            setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(5))
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (unlikely: SecurityException) {
            stopSelf()
        }
    }

    private fun processNewLocation(location: Location) {
        android.util.Log.d("TelemetryService", "New telemetry ping generated at ${location.time}")


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

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Creăm notificarea
        val notification = createNotification()

        startForeground(NOTIFICATION_ID, notification)

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

            // 2. Îl înregistrăm în sistem
            // Dacă există deja, Android nu face nimic (deci e safe)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}