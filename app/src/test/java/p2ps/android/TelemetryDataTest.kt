package p2ps.android

import org.junit.Test
import org.junit.Assert.*
import p2ps.android.data.TelemetryEntity
import p2ps.android.data.TelemetryPing
import p2ps.android.data.toEntity

class TelemetryDataTest {

    // ── TelemetryPing ──────────────────────────────────────────────────────────

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

        assertEquals("test_dev", ping.deviceId)
        assertEquals(44.4268, ping.lat, 0.0001)
        assertEquals("BACKGROUND", ping.triggerType)
        assertEquals(currentTime, ping.timestamp)
        assertEquals(10.0f, ping.accuracyMeters)
    }

    @Test
    fun telemetryPing_coordinatesValidation() {
        val ping = TelemetryPing("d", "s", "i", "t", 90.0, 180.0, 1f, 0L)
        assertTrue("Latitude must be within [-90, 90]", ping.lat in -90.0..90.0)
        assertTrue("Longitude must be within [-180, 180]", ping.lng in -180.0..180.0)

    @Test
    fun telemetryEntity_defaultId_isZero() {
        val entity = TelemetryEntity(
            deviceId = "dev", storeId = "store", itemId = "item",
            triggerType = "BACKGROUND", latitude = 44.0, longitude = 26.0,
            accuracy = 5f, timestamp = 1000L
        )
        assertEquals(0, entity.id)
    }

    @Test
    fun telemetryEntity_customId_isPreserved() {
        val entity = TelemetryEntity(
            id = 42,
            deviceId = "dev", storeId = "store", itemId = "item",
            triggerType = "BACKGROUND", latitude = 44.0, longitude = 26.0,
            accuracy = 5f, timestamp = 1000L
        )
        assertEquals(42, entity.id)
    }

    @Test
    fun telemetryEntity_equality_sameValues() {
        val e1 = TelemetryEntity(1, "dev", "store", "item", "TYPE", 1.0, 2.0, 0.5f, 1000L)
        val e2 = TelemetryEntity(1, "dev", "store", "item", "TYPE", 1.0, 2.0, 0.5f, 1000L)
        assertEquals(e1, e2)
    }

    @Test
    fun telemetryEntity_equality_differentId() {
        val e1 = TelemetryEntity(1, "dev", "store", "item", "TYPE", 1.0, 2.0, 0.5f, 1000L)
        val e2 = TelemetryEntity(2, "dev", "store", "item", "TYPE", 1.0, 2.0, 0.5f, 1000L)
        assertNotEquals(e1, e2)
    }

    @Test
    fun telemetryEntity_usesLatitudeLongitude_notLatLng() {
        val entity = TelemetryEntity(
            deviceId = "dev", storeId = "store", itemId = "item",
            triggerType = "T", latitude = 55.7558, longitude = 37.6173,
            accuracy = 3f, timestamp = 500L
        )
        assertEquals(55.7558, entity.latitude, 0.0001)
        assertEquals(37.6173, entity.longitude, 0.0001)
    }

    // ── TelemetryPing → TelemetryEntity field mapping ─────────────────────────

    @Test
    fun telemetryPing_to_telemetryEntity_fieldMapping() {
        val ping = TelemetryPing(
            deviceId = "mapped_dev",
            storeId = "mapped_store",
            itemId = "mapped_item",
            triggerType = "SCAN",
            lat = 48.8566,
            lng = 2.3522,
            accuracy = 7.3f,
            timestamp = 999888777L
        )
        val entity = ping.toEntity()

        assertEquals(ping.deviceId, entity.deviceId)
        assertEquals(ping.storeId, entity.storeId)
        assertEquals(ping.itemId, entity.itemId)
        assertEquals(ping.triggerType, entity.triggerType)
        assertEquals(ping.lat, entity.latitude, 0.0001)
        assertEquals(ping.lng, entity.longitude, 0.0001)
        assertEquals(ping.accuracy, entity.accuracy, 0.0001f)
        assertEquals(ping.timestamp, entity.timestamp)
    }

    @Test
    fun telemetryPing_lat_mapsTo_entity_latitude_not_longitude() {
        val ping = TelemetryPing("d", "s", "i", "t", lat = 10.0, lng = 20.0, accuracy = 1f, timestamp = 0L)
        val entity = TelemetryEntity(
            deviceId = ping.deviceId, storeId = ping.storeId, itemId = ping.itemId,
            triggerType = ping.triggerType, latitude = ping.lat, longitude = ping.lng,
            accuracy = ping.accuracy, timestamp = ping.timestamp
        )
        assertEquals(10.0, entity.latitude, 0.0001)
        assertEquals(20.0, entity.longitude, 0.0001)
        // Ensure they're not swapped
        assertNotEquals(entity.latitude, entity.longitude, 0.0001)
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
            accuracyMeters = 5.0f,
            timestamp = 1700000000000L
        )

        assertEquals("dev_001", ping.deviceId)
        assertEquals("store_XYZ", ping.storeId)
        assertEquals("item_ABC", ping.itemId)
        assertEquals("MANUAL", ping.triggerType)
        assertEquals(48.8566, ping.lat, 0.0001)
        assertEquals(2.3522, ping.lng, 0.0001)
        assertEquals(5.0f, ping.accuracyMeters)
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
        assertEquals(0.0f, ping.accuracyMeters)
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