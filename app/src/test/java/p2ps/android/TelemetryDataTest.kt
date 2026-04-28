package p2ps.android

import org.junit.Test
import org.junit.Assert.*
import p2ps.android.data.TelemetryPing

class TelemetryDataTest {

    @Test
    fun telemetryPing_isCorrect() {
        val currentTime = System.currentTimeMillis()
        val ping = TelemetryPing(
            deviceId = "test_dev",
            storeId = "test_store",
            itemId = "test_item",
            triggerType = "BACKGROUND",
            lat = 44.4268,
            lng = 26.1025,
            accuracy = 10.0f,
            timestamp = currentTime
        )

        assertEquals("test_dev", ping.deviceId)
        assertEquals(44.4268, ping.lat, 0.0001)
        assertEquals("BACKGROUND", ping.triggerType)
        assertEquals(currentTime, ping.timestamp)
        assertEquals(10.0f, ping.accuracy)
    }

    @Test
    fun telemetryPing_coordinatesValidation() {
        val ping = TelemetryPing("d", "s", "i", "t", 90.0, 180.0, 1f, 0L)
        assertTrue("Latitude must be within [-90, 90]", ping.lat in -90.0..90.0)
        assertTrue("Longitude must be within [-180, 180]", ping.lng in -180.0..180.0)

        assertTrue("Latitude should be within range", ping.lat <= 180.0)
    }

    @Test
    fun telemetryPing_allFieldsAccessible() {
        val ping = TelemetryPing(
            deviceId = "dev_001",
            storeId = "store_XYZ",
            itemId = "item_ABC",
            triggerType = "MANUAL",
            lat = 48.8566,
            lng = 2.3522,
            accuracy = 5.0f,
            timestamp = 1700000000000L
        )

        assertEquals("dev_001", ping.deviceId)
        assertEquals("store_XYZ", ping.storeId)
        assertEquals("item_ABC", ping.itemId)
        assertEquals("MANUAL", ping.triggerType)
        assertEquals(48.8566, ping.lat, 0.0001)
        assertEquals(2.3522, ping.lng, 0.0001)
        assertEquals(5.0f, ping.accuracy)
        assertEquals(1700000000000L, ping.timestamp)
    }

    @Test
    fun telemetryPing_negativeCoordinates() {
        val ping = TelemetryPing("d", "s", "i", "t", -33.8688, -70.6693, 8.0f, 0L)
        assertTrue("Southern hemisphere latitude valid", ping.lat in -90.0..90.0)
        assertTrue("Western hemisphere longitude valid", ping.lng in -180.0..180.0)
        assertEquals(-33.8688, ping.lat, 0.0001)
        assertEquals(-70.6693, ping.lng, 0.0001)
    }

    @Test
    fun telemetryPing_zeroCoordinates() {
        val ping = TelemetryPing("d", "s", "i", "t", 0.0, 0.0, 0.0f, 0L)
        assertEquals(0.0, ping.lat, 0.0)
        assertEquals(0.0, ping.lng, 0.0)
        assertEquals(0.0f, ping.accuracy)
        assertEquals(0L, ping.timestamp)
    }

    @Test
    fun telemetryPing_dataClassEquality() {
        val ping1 = TelemetryPing("d", "s", "i", "t", 10.0, 20.0, 3.0f, 100L)
        val ping2 = TelemetryPing("d", "s", "i", "t", 10.0, 20.0, 3.0f, 100L)
        assertEquals(ping1, ping2)
        assertEquals(ping1.hashCode(), ping2.hashCode())
    }

    @Test
    fun telemetryPing_dataClassInequality() {
        val ping1 = TelemetryPing("d1", "s", "i", "t", 10.0, 20.0, 3.0f, 100L)
        val ping2 = TelemetryPing("d2", "s", "i", "t", 10.0, 20.0, 3.0f, 100L)
        assertNotEquals(ping1, ping2)
    }

    @Test
    fun telemetryPing_copyProducesEqualObject() {
        val original = TelemetryPing("dev", "store", "item", "BACKGROUND", 44.0, 26.0, 10.0f, 12345L)
        val copy = original.copy()
        assertEquals(original, copy)
    }

    @Test
    fun telemetryPing_copyWithModifiedField() {
        val original = TelemetryPing("dev", "store", "item", "BACKGROUND", 44.0, 26.0, 10.0f, 12345L)
        val modified = original.copy(triggerType = "MANUAL")
        assertEquals("MANUAL", modified.triggerType)
        assertEquals(original.deviceId, modified.deviceId)
        assertEquals(original.storeId, modified.storeId)
        assertEquals(original.itemId, modified.itemId)
        assertNotEquals(original, modified)
    }

    @Test
    fun telemetryPing_boundaryLatitudeValues() {
        val northPole = TelemetryPing("d", "s", "i", "t", 90.0, 0.0, 1f, 0L)
        val southPole = TelemetryPing("d", "s", "i", "t", -90.0, 0.0, 1f, 0L)
        assertTrue(northPole.lat in -90.0..90.0)
        assertTrue(southPole.lat in -90.0..90.0)
    }

    @Test
    fun telemetryPing_boundaryLongitudeValues() {
        val eastBoundary = TelemetryPing("d", "s", "i", "t", 0.0, 180.0, 1f, 0L)
        val westBoundary = TelemetryPing("d", "s", "i", "t", 0.0, -180.0, 1f, 0L)
        assertTrue(eastBoundary.lng in -180.0..180.0)
        assertTrue(westBoundary.lng in -180.0..180.0)
    }

    @Test
    fun telemetryPing_triggerTypeValues() {
        val backgroundPing = TelemetryPing("d", "s", "i", "BACKGROUND", 0.0, 0.0, 0f, 0L)
        val manualPing = TelemetryPing("d", "s", "i", "MANUAL", 0.0, 0.0, 0f, 0L)
        val startupPing = TelemetryPing("d", "s", "i", "STARTUP_AUTO_SCAN", 0.0, 0.0, 0f, 0L)

        assertEquals("BACKGROUND", backgroundPing.triggerType)
        assertEquals("MANUAL", manualPing.triggerType)
        assertEquals("STARTUP_AUTO_SCAN", startupPing.triggerType)
    }
}