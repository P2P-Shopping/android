package p2ps.android

import android.util.Log

class ApiClient {
    private val TAG = "ApiClient"

    fun sendPing(dto: Map<String, Any>) {
        Log.i(TAG, "POST /api/telemetry/ping")

        // REZOLVARE GDPR: Logăm doar cheile sau statusul, nu datele brute (lat/lng)
        Log.d(TAG, "Payload structure valid. Keys present: ${dto.keys}")

        // TODO(#34): Implement actual Retrofit/OkHttp call here
        // Simulăm succesul pentru moment, dar marcăm clar că e placeholder
        val isSimulation = true
        if (isSimulation) {
            Log.i(TAG, "Simulation: Telemetry accepted (202 Accepted)")
        }
    }
}