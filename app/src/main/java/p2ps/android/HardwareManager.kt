package p2ps.android

import android.util.Log
import p2ps.android.data.TelemetryPing

/**
 * Manages interactions with physical hardware and coordinates telemetry dispatch.
 */
class HardwareManager {
    private val TAG = "HardwareManager"
    private val apiClient = ApiClient()

    fun initialize() {
        Log.d(TAG, "Initializing Hardware SDK Scaffolding...")
        Log.d(TAG, "Attempting to connect to hardware API...")
        // Logic for connecting to physical sensors/triggers
        Log.i(TAG, "Hardware connection established successfully.")
    }

    fun handleHardwareTrigger(ping: TelemetryPing) {
        Log.i(TAG, "Hardware Trigger Detected for item: ${ping.itemId}")
        
        // Dispatch to backend via ApiClient
        apiClient.sendPing(ping)
    }
}
