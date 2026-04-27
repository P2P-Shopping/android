package p2ps.android

import android.util.Log
import p2ps.android.data.TelemetryPing

class ApiClient {
    private val TAG = "ApiClient"

    fun sendPing(ping: TelemetryPing): Boolean {
        Log.i(TAG, "POST /api/telemetry/ping")
        Log.d(TAG, "Dispatching ping: deviceId=${ping.deviceId}, trigger=${ping.triggerType}")


        val isSuccess = true

        if (!isSuccess) {
            Log.e(TAG, "Network failure: simulation of failed request")
            return false
        }

        Log.i(TAG, "Simulation: Telemetry accepted (202 Accepted)")
        return true

    }
}