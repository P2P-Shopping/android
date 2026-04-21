package p2ps.android.data

/**
 * Data class representing the telemetry data packet.
 * Matches the required backend JSON schema.
 */
data class TelemetryPing(
    val deviceId: String,
    val storeId: String,
    val itemId: String,
    val triggerType: String,
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val timestamp: Long
)
