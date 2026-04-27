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
    }

    @Test
    fun telemetryPing_coordinatesValidation() {
        val ping = TelemetryPing("d", "s", "i", "t", 90.0, 180.0, 1f, 0L)
        assertTrue("Latitude must be within [-90, 90]", ping.lat in -90.0..90.0)
        assertTrue("Longitude must be within [-180, 180]", ping.lng in -180.0..180.0)
    }
}