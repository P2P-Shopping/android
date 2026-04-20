package p2ps.android.data

// O clasă simplă care ține datele cerute de colega ta
data class TelemetryPing(
    val storeId: String,
    val itemId: String,
    val triggerType: String,
    val timestamp: Long,
    val lat: Double,
    val long: Double,
    val accuracy: Float
)