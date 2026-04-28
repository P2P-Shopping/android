package p2ps.android.data

/**
 * Data wrapper for batch telemetry uploads, matching the server-side TelemetryBatchDTO.
 */
data class TelemetryBatch(
    val pings: List<TelemetryPing>
)
