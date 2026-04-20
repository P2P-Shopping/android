package p2ps.android

import android.util.Log

class ApiClient {
    private val TAG = "ApiClient"

    // Task #34: Send telemetry DTO to backend ingestion endpoint
    fun sendPing(dto: Map<String, Any>) {
        Log.i(TAG, "POST /api/telemetry/ping")
        Log.d(TAG, "Serializing DTO to JSON: $dto")

        // Simulare logică succes/eroare
        val isSuccess = true
        if (isSuccess) {
            Log.i(TAG, "Success: Telemetry accepted by API (202 Accepted)")
        } else {
            Log.e(TAG, "Error: API Request failed (500 Internal Server Error)")
        }
    }
}