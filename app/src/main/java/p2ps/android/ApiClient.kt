package p2ps.android

import android.util.Log
import p2ps.android.data.TelemetryPing

class ApiClient {
    private val TAG = "ApiClient"

    fun sendPing(ping: TelemetryPing) {
        Log.i(TAG, "POST /api/telemetry/ping")

        // GDPR Compliance: Log metadata only, no raw PII/Location in production logs
        Log.d(TAG, "Dispatching ping: deviceId=${ping.deviceId}, trigger=${ping.triggerType}")

        // TODO(#34): Implement actual Retrofit/OkHttp call here
        val isSimulation = true
        if (isSimulation) {
            Log.i(TAG, "Simulation: Telemetry accepted (202 Accepted)")
        }
    }
}
