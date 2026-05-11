package p2ps.android

import org.junit.Test
import org.junit.Assert.*
import p2ps.android.data.TelemetryEntity
import p2ps.android.data.TelemetryPing
import p2ps.android.data.toEntity

class TelemetryDataTest {

    private fun ping(
        deviceId: String = "dev",
        storeId: String = "store",
        itemId: String = "item",
        triggerType: String = "BACKGROUND",
        lat: Double = 44.4268,
        lng: Double = 26.1025,
        accuracyMeters: Float = 10.0f,
        timestamp: Long = 1700000000000L,
        pingId: String = "ping-001"
    ) = TelemetryPing(deviceId, storeId, itemId, triggerType, lat, lng, accuracyMeters, timestamp, pingId)

    @Test
    fun telemetryPing_isCorrect() {
        val currentTime = System.currentTimeMillis()
        val p = ping(deviceId = "test_dev", lat = 44.4268, triggerType = "BACKGROUND", timestamp = currentTime, accuracyMeters = 10.0f)
        assertEquals("test_dev", p.deviceId)
        assertEquals(44.4268, p.lat, 0.0001)
        assertEquals("BACKGROUND", p.triggerType)
        assertEquals(currentTime, p.timestamp)
        assertEquals(10.0f, p.accuracyMeters)
    }

    @Test
    fun telemetryPing_coordinatesValidation() {
        val p = ping(lat = 90.0, lng = 180.0)
        assertTrue(p.lat in -90.0..90.0)
        assertTrue(p.lng in -180.0..180.0)
    }

    @Test
    fun telemetryEntity_defaultId_isZero() {
        val entity = TelemetryEntity(deviceId = "dev", storeId = "store", itemId = "item",
            triggerType = "BACKGROUND", latitude = 44.0, longitude = 26.0,
            accuracy = 5f, timestamp = 1000L, pingId = "p1")
        assertEquals(0, entity.id)
    }

    @Test
    fun telemetryEntity_customId_isPreserved() {
        val entity = TelemetryEntity(id = 42, deviceId = "dev", storeId = "store", itemId = "item",
            triggerType = "BACKGROUND", latitude = 44.0, longitude = 26.0,
            accuracy = 5f, timestamp = 1000L, pingId = "p1")
        assertEquals(42, entity.id)
    }

    @Test
    fun telemetryPing_to_telemetryEntity_fieldMapping() {
        val p = ping(deviceId = "mapped_dev", storeId = "mapped_store", itemId = "mapped_item",
            triggerType = "SCAN", lat = 48.8566, lng = 2.3522, accuracyMeters = 7.3f,
            timestamp = 999888777L, pingId = "ping-xyz")
        val entity = p.toEntity()
        assertEquals(p.deviceId, entity.deviceId)
        assertEquals(p.storeId, entity.storeId)
        assertEquals(p.itemId, entity.itemId)
        assertEquals(p.triggerType, entity.triggerType)
        assertEquals(p.lat, entity.latitude, 0.0001)
        assertEquals(p.lng, entity.longitude, 0.0001)
        assertEquals(p.accuracyMeters, entity.accuracy, 0.0001f)
        assertEquals(p.timestamp, entity.timestamp)
    }

    @Test
    fun telemetryPing_dataClassEquality() {
        val p1 = ping()
        val p2 = ping()
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test
    fun telemetryPing_dataClassInequality() {
        val p1 = ping(deviceId = "d1")
        val p2 = ping(deviceId = "d2")
        assertNotEquals(p1, p2)
    }

    @Test
    fun telemetryPing_copyWithModifiedField() {
        val original = ping(triggerType = "BACKGROUND")
        val modified = original.copy(triggerType = "MANUAL")
        assertEquals("MANUAL", modified.triggerType)
        assertNotEquals(original, modified)
    }

    @Test
    fun telemetryPing_negativeCoordinates() {
        val p = ping(lat = -33.8688, lng = -70.6693)
        assertTrue(p.lat in -90.0..90.0)
        assertTrue(p.lng in -180.0..180.0)
    }

    @Test
    fun telemetryPing_zeroCoordinates() {
        val p = ping(lat = 0.0, lng = 0.0, accuracyMeters = 0.0f, timestamp = 0L)
        assertEquals(0.0, p.lat, 0.0)
        assertEquals(0.0, p.lng, 0.0)
        assertEquals(0.0f, p.accuracyMeters)
        assertEquals(0L, p.timestamp)
    }

    @Test
    fun telemetryPing_triggerTypeValues() {
        assertEquals("BACKGROUND", ping(triggerType = "BACKGROUND").triggerType)
        assertEquals("MANUAL", ping(triggerType = "MANUAL").triggerType)
        assertEquals("STARTUP_AUTO_SCAN", ping(triggerType = "STARTUP_AUTO_SCAN").triggerType)
    }

    @Test
    fun telemetryPing_pingId_isPreserved() {
        val p = ping(pingId = "unique-id-123")
        assertEquals("unique-id-123", p.pingId)
    }
}