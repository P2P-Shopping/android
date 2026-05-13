package p2ps.android.proximity.data

import org.junit.Assert.*
import org.junit.Test

class ProximityPingTest {

    private val testPing = ProximityPing(
        deviceId = "device_001",
        lat = 47.15,
        lng = 27.59,
        timestamp = 1700000000000L,
        fcmToken = "fcm_token_abc"
    )

    @Test
    fun proximityPing_fieldsStoredCorrectly() {
        assertEquals("device_001", testPing.deviceId)
        assertEquals(47.15, testPing.lat, 0.0001)
        assertEquals(27.59, testPing.lng, 0.0001)
        assertEquals(1700000000000L, testPing.timestamp)
        assertEquals("fcm_token_abc", testPing.fcmToken)
    }

    @Test
    fun proximityPing_latInValidRange() {
        assertTrue(testPing.lat in -90.0..90.0)
    }

    @Test
    fun proximityPing_lngInValidRange() {
        assertTrue(testPing.lng in -180.0..180.0)
    }

    @Test
    fun proximityPing_dataClassEquality() {
        val copy = testPing.copy()
        assertEquals(testPing, copy)
        assertEquals(testPing.hashCode(), copy.hashCode())
    }

    @Test
    fun proximityPing_inequalityOnDifferentDeviceId() {
        val other = testPing.copy(deviceId = "device_002")
        assertNotEquals(testPing, other)
    }

    @Test
    fun proximityPing_copyWithModifiedField() {
        val modified = testPing.copy(lat = 48.0)
        assertEquals(48.0, modified.lat, 0.0001)
        assertEquals(testPing.deviceId, modified.deviceId)
        assertNotEquals(testPing, modified)
    }

    @Test
    fun proximityPing_boundaryCoordinates_valid() {
        val boundary = ProximityPing("d", 90.0, 180.0, 0L, "t")
        assertTrue(boundary.lat in -90.0..90.0)
        assertTrue(boundary.lng in -180.0..180.0)
    }

    @Test
    fun proximityPing_negativeCoordinates_valid() {
        val ping = ProximityPing("d", -33.8688, -70.6693, 0L, "t")
        assertEquals(-33.8688, ping.lat, 0.0001)
        assertEquals(-70.6693, ping.lng, 0.0001)
    }

    @Test
    fun proximityPing_zeroTimestamp_doesNotThrow() {
        val ping = ProximityPing("d", 0.0, 0.0, 0L, "t")
        assertEquals(0L, ping.timestamp)
    }

    @Test
    fun proximityPing_emptyFcmToken_isAllowed() {
        val ping = ProximityPing("d", 0.0, 0.0, 0L, "")
        assertEquals("", ping.fcmToken)
    }
}