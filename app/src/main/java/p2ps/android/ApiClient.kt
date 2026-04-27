package p2ps.android

import android.util.Log
import p2ps.android.data.TelemetryPing
import kotlin.random.Random

class ApiClient {
    private val TAG = "ApiClient"
    suspend fun sendPing(ping: TelemetryPing): Boolean {
        Log.i(TAG, "POST /api/telemetry/ping")

        // TODO(task-40): replace with real HTTP call (Retrofit)
        val isSuccess = simulateNetworkSuccess()

        if (!isSuccess) {
            Log.e(TAG, "Network failure: simulation of failed request")
            return false
        }

        Log.i(TAG, "Simulation: Telemetry accepted (202 Accepted)")
        return true
    }
    private fun simulateNetworkSuccess(): Boolean {
        return Random.nextFloat() > 0.2f
    }
}