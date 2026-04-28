package p2ps.android.data

/**
 * Unified Data Class for Telemetry.
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
/**
 * Extension function to map TelemetryPing to TelemetryEntity for Room persistence.
 */
fun TelemetryPing.toEntity(): TelemetryEntity {
    return TelemetryEntity(
        deviceId = this.deviceId,
        storeId = this.storeId,
        itemId = this.itemId,
        triggerType = this.triggerType,
        latitude = this.lat,
        longitude = this.lng,
        accuracy = this.accuracy,
        timestamp = this.timestamp
    )
}
