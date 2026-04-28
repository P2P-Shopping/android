package p2ps.android.data

import com.google.gson.annotations.SerializedName

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
    @SerializedName("accuracyMeters")
    val accuracy: Float,
    val timestamp: Long
)