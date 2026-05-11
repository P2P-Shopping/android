package p2ps.android.fcm

/**
 * Pure utility object for proximity notification routing logic.
 * Extracted from ProximityMessagingService to enable unit testing.
 */
object ProximityNotificationUtils {

    const val DEFAULT_TITLE = "Item nearby!"
    const val DEFAULT_BODY = "A shopping list item is available near your current location."

    /**
     * Returns true if a notification should be shown for this FCM message.
     */
    fun shouldShowNotification(type: String?, deepLink: String?): Boolean =
        type == "PROXIMITY_ALERT" && !deepLink.isNullOrBlank()

    /**
     * Returns the notification title, falling back to default if null.
     */
    fun resolveTitle(notificationTitle: String?): String =
        notificationTitle ?: DEFAULT_TITLE

    /**
     * Returns the notification body, falling back to default if null.
     */
    fun resolveBody(notificationBody: String?): String =
        notificationBody ?: DEFAULT_BODY

    /**
     * Derives a notification ID from the deep link.
     */
    fun notificationId(deepLink: String): Int = deepLink.hashCode()

    /**
     * Extracts the list ID from a deep link URL.
     * e.g. "http://localhost:5173/list/abc123" -> "abc123"
     */
    fun extractListId(deepLink: String): String = deepLink.substringAfterLast("/")
}