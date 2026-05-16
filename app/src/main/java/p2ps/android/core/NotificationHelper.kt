package p2ps.android.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

fun createProximityNotificationChannel(context: Context) {
    // Canalele de notificare sunt necesare doar de la Android 8.0 (Oreo) în sus
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "proximity_alerts"
        val channelName = "Proximity Alerts"
        val channelDescription = "Notificări când te afli în apropierea unui item de interes."
        val importance = NotificationManager.IMPORTANCE_HIGH // Obligatoriu pentru Heads-up, sunet și vibrație

        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription

            // 1. Configurare sunet (folosim sunetul default de notificare)
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(soundUri, audioAttributes)

            // 2. Configurare vibrație
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 500L, 250L, 500L) // (așteaptă, vibrează, așteaptă, vibrează)

            // 3. Sistemul Android gestionează automat Do-Not-Disturb (DND) dacă nu suprascriem noi regula prin bypassDnd
        }

        // Înregistrăm canalul în sistem
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}