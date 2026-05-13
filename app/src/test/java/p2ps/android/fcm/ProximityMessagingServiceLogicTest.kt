package p2ps.android.fcm

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ProximityNotificationUtils — the shared production utility
 * used by ProximityMessagingService. Changes to the utility will break these tests.
 */
class ProximityMessagingServiceLogicTest {

    // ── shouldShowNotification ────────────────────────────────────────────────

    @Test
    fun shouldShow_proximityAlert_withDeepLink_returnsTrue() =
        assertTrue(ProximityNotificationUtils.shouldShowNotification(
            "PROXIMITY_ALERT", "http://localhost:5173/list/abc"))

    @Test
    fun shouldShow_wrongType_returnsFalse() =
        assertFalse(ProximityNotificationUtils.shouldShowNotification(
            "OTHER_TYPE", "http://localhost:5173/list/abc"))

    @Test
    fun shouldShow_nullType_returnsFalse() =
        assertFalse(ProximityNotificationUtils.shouldShowNotification(
            null, "http://localhost:5173/list/abc"))

    @Test
    fun shouldShow_proximityAlert_nullDeepLink_returnsFalse() =
        assertFalse(ProximityNotificationUtils.shouldShowNotification("PROXIMITY_ALERT", null))

    @Test
    fun shouldShow_proximityAlert_blankDeepLink_returnsFalse() =
        assertFalse(ProximityNotificationUtils.shouldShowNotification("PROXIMITY_ALERT", ""))

    @Test
    fun shouldShow_proximityAlert_whitespaceDeepLink_returnsFalse() =
        assertFalse(ProximityNotificationUtils.shouldShowNotification("PROXIMITY_ALERT", "   "))

    @Test
    fun shouldShow_bothNull_returnsFalse() =
        assertFalse(ProximityNotificationUtils.shouldShowNotification(null, null))

    @Test
    fun shouldShow_emptyType_returnsFalse() =
        assertFalse(ProximityNotificationUtils.shouldShowNotification(
            "", "http://localhost:5173/list/abc"))

    // ── resolveTitle ──────────────────────────────────────────────────────────

    @Test
    fun resolveTitle_withValue_returnsValue() =
        assertEquals("Custom Title", ProximityNotificationUtils.resolveTitle("Custom Title"))

    @Test
    fun resolveTitle_null_returnsDefault() =
        assertEquals(ProximityNotificationUtils.DEFAULT_TITLE,
            ProximityNotificationUtils.resolveTitle(null))

    @Test
    fun resolveTitle_emptyString_returnsEmpty() =
        assertEquals("", ProximityNotificationUtils.resolveTitle(""))

    // ── resolveBody ───────────────────────────────────────────────────────────

    @Test
    fun resolveBody_withValue_returnsValue() =
        assertEquals("Custom body", ProximityNotificationUtils.resolveBody("Custom body"))

    @Test
    fun resolveBody_null_returnsDefault() =
        assertEquals(ProximityNotificationUtils.DEFAULT_BODY,
            ProximityNotificationUtils.resolveBody(null))

    // ── notificationId ────────────────────────────────────────────────────────

    @Test
    fun notificationId_sameDeepLink_sameId() {
        val url = "http://localhost:5173/list/abc123"
        assertEquals(ProximityNotificationUtils.notificationId(url),
            ProximityNotificationUtils.notificationId(url))
    }

    @Test
    fun notificationId_differentDeepLinks_differentIds() {
        val id1 = ProximityNotificationUtils.notificationId("http://localhost:5173/list/abc")
        val id2 = ProximityNotificationUtils.notificationId("http://localhost:5173/list/xyz")
        assertNotEquals(id1, id2)
    }

    @Test
    fun notificationId_isConsistentWithHashCode() {
        val url = "http://localhost:5173/list/test"
        assertEquals(url.hashCode(), ProximityNotificationUtils.notificationId(url))
    }

    // ── extractListId ─────────────────────────────────────────────────────────

    @Test
    fun extractListId_returnsCorrectId() =
        assertEquals("abc123",
            ProximityNotificationUtils.extractListId("http://localhost:5173/list/abc123"))

    @Test
    fun extractListId_multipleFormats() {
        assertEquals("id1", ProximityNotificationUtils.extractListId("http://localhost:5173/list/id1"))
        assertEquals("id2", ProximityNotificationUtils.extractListId("http://192.168.1.1:5173/list/id2"))
        assertEquals("id3", ProximityNotificationUtils.extractListId("https://p2p-shopping.app/list/id3"))
    }
}