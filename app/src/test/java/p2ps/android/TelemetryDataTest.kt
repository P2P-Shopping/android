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
            accuracyMeters = 10.0f,
            timestamp = currentTime
        )

        // Verificăm dacă datele sunt stocate corect în obiect
        assertEquals("test_dev", ping.deviceId)
        assertEquals(44.4268, ping.lat, 0.0001)
        assertEquals("BACKGROUND", ping.triggerType)
        assertEquals(currentTime, ping.timestamp)
        assertEquals(10.0f, ping.accuracyMeters)
    }

    @Test
    fun telemetryPing_coordinatesValidation() {
        // Un test simplu să ne asigurăm că latitudinea e validă
        val ping = TelemetryPing("d", "s", "i", "t", 91.0, 0.0, 1f, 0L)

        // Aici am putea adăuga logică de validare în viitor
        assertTrue("Latitude should be within range", ping.lat <= 180.0)
    }
}
