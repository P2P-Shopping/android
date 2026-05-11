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
    fun shouldShowNotification(type: String?, deepLink: String?): Boolean {
        if (type != "PROXIMITY_ALERT" || deepLink.isNullOrBlank()) return false
        return try {
            val uri = java.net.URI(deepLink.trim())
            val allowedSchemes = setOf("http", "https")
            val allowedHosts = setOf("localhost", "127.0.0.1", "10.0.2.2", "p2p-shopping.app")
            uri.scheme in allowedSchemes &&
                    uri.host in allowedHosts &&
                    uri.path?.startsWith("/list/") == true
        } catch (e: Exception) {
            false
        }
    }

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
    fun extractListId(deepLink: String): String =
        deepLink
            .substringBefore('#')
            .substringBefore('?')
            .trimEnd('/')
            .substringAfterLast("/")
}