package p2ps.android.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import p2ps.android.MainActivity
import p2ps.android.R
import p2ps.android.WebViewActivity

class ProximityMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ProximityMessaging"
        private const val CHANNEL_ID = "proximity_alerts"
        private const val CHANNEL_NAME = "Proximity Alerts"
        const val EXTRA_DEEP_LINK = "deepLink"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")
        FcmTokenManager.saveToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"]
        val deepLink = message.data["deepLink"]
        val title = message.notification?.title ?: "Item nearby!"
        val body = message.notification?.body
            ?: "A shopping list item is available near your current location."

        Log.d(TAG, "FCM message received. type=$type, deepLink=$deepLink")

        if (type == "PROXIMITY_ALERT" && !deepLink.isNullOrBlank()) {
            showProximityNotification(title, body, deepLink)
        }
    }

    private fun showProximityNotification(title: String, body: String, deepLink: String) {
        createChannelIfNeeded()

        val intent = Intent(this, WebViewActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_DEEP_LINK, deepLink)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            deepLink.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_location)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(deepLink.hashCode(), notification)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when a shopping list item is nearby"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}