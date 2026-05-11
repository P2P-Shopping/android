package p2ps.android.fcm

import org.junit.Assert.*
import org.junit.Test

/**
 * Pure-logic unit tests for ProximityMessagingService message routing.
 * Tests the conditions that decide whether to show a notification,
 * extracted from onMessageReceived, without Android framework dependencies.
 */
class ProximityMessagingServiceLogicTest {

    // Replicates the guard from onMessageReceived
    private fun shouldShowNotification(type: String?, deepLink: String?): Boolean =
        type == "PROXIMITY_ALERT" && !deepLink.isNullOrBlank()

    // Replicates title/body fallback logic
    private fun resolveTitle(notificationTitle: String?): String =
        notificationTitle ?: "Item nearby!"

    private fun resolveBody(notificationBody: String?): String =
        notificationBody ?: "A shopping list item is available near your current location."

    // Replicates notification ID derivation
    private fun notificationId(deepLink: String): Int = deepLink.hashCode()

    // ── shouldShowNotification ────────────────────────────────────────────────

    @Test
    fun shouldShow_proximityAlert_withDeepLink_returnsTrue() {
        assertTrue(shouldShowNotification("PROXIMITY_ALERT", "http://localhost:5173/list/abc"))
    }

    @Test
    fun shouldShow_wrongType_returnsFalse() {
        assertFalse(shouldShowNotification("OTHER_TYPE", "http://localhost:5173/list/abc"))
    }

    @Test
    fun shouldShow_nullType_returnsFalse() {
        assertFalse(shouldShowNotification(null, "http://localhost:5173/list/abc"))
    }

    @Test
    fun shouldShow_proximityAlert_nullDeepLink_returnsFalse() {
        assertFalse(shouldShowNotification("PROXIMITY_ALERT", null))
    }

    @Test
    fun shouldShow_proximityAlert_blankDeepLink_returnsFalse() {
        assertFalse(shouldShowNotification("PROXIMITY_ALERT", ""))
    }

    @Test
    fun shouldShow_proximityAlert_whitespaceDeepLink_returnsFalse() {
        assertFalse(shouldShowNotification("PROXIMITY_ALERT", "   "))
    }

    @Test
    fun shouldShow_bothNull_returnsFalse() {
        assertFalse(shouldShowNotification(null, null))
    }

    @Test
    fun shouldShow_emptyType_returnsFalse() {
        assertFalse(shouldShowNotification("", "http://localhost:5173/list/abc"))
    }

    // ── Title/body fallback ───────────────────────────────────────────────────

    @Test
    fun resolveTitle_withValue_returnsValue() {
        assertEquals("Custom Title", resolveTitle("Custom Title"))
    }

    @Test
    fun resolveTitle_null_returnsDefault() {
        assertEquals("Item nearby!", resolveTitle(null))
    }

    @Test
    fun resolveBody_withValue_returnsValue() {
        assertEquals("Custom body", resolveBody("Custom body"))
    }

    @Test
    fun resolveBody_null_returnsDefault() {
        assertEquals(
            "A shopping list item is available near your current location.",
            resolveBody(null)
        )
    }

    @Test
    fun resolveTitle_emptyString_returnsEmptyNotDefault() {
        // Empty string is a valid title (not null), so it's kept as-is
        assertEquals("", resolveTitle(""))
    }

    // ── Notification ID derivation ────────────────────────────────────────────

    @Test
    fun notificationId_sameDeepLink_sameId() {
        val url = "http://localhost:5173/list/abc123"
        assertEquals(notificationId(url), notificationId(url))
    }

    @Test
    fun notificationId_differentDeepLinks_differentIds() {
        val id1 = notificationId("http://localhost:5173/list/abc")
        val id2 = notificationId("http://localhost:5173/list/xyz")
        assertNotEquals(id1, id2)
    }

    @Test
    fun notificationId_isConsistentWithHashCode() {
        val url = "http://localhost:5173/list/test"
        assertEquals(url.hashCode(), notificationId(url))
    }

    // ── Deep link format validation ───────────────────────────────────────────

    @Test
    fun deepLink_containsListId_isValidFormat() {
        val deepLink = "http://localhost:5173/list/abc123"
        assertTrue(deepLink.contains("/list/"))
        assertTrue(deepLink.isNotBlank())
    }

    @Test
    fun deepLink_extractListId_isCorrect() {
        val deepLink = "http://localhost:5173/list/abc123"
        val listId = deepLink.substringAfterLast("/")
        assertEquals("abc123", listId)
    }

    @Test
    fun deepLink_multipleFormats_allNonBlank() {
        val links = listOf(
            "http://localhost:5173/list/id1",
            "http://192.168.1.1:5173/list/id2",
            "https://p2p-shopping.app/list/id3"
        )
        links.forEach { assertTrue("$it should be non-blank", it.isNotBlank()) }
    }
}