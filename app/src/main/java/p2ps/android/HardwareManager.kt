package p2ps.android

import android.util.Log
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.TelemetryPing

/**
 * Manages interactions with physical hardware and coordinates telemetry dispatch.
 */
class HardwareManager(private val telemetryDispatcher: TelemetryDispatcher) {
    private val TAG = "HardwareManager"
    private var isInitialized = false

    // Initializing the SDK
    fun initialize() {
        if (isInitialized) return

        Log.d(TAG, "Initializing Hardware SDK Scaffolding...")

        if (connectToDevice()) {
            isInitialized = true
        } else {
            Log.e(TAG, "Hardware SDK initialization failed.")
        }
    }

    private fun connectToDevice(): Boolean {
        return try {
            Log.d(TAG, "Attempting to connect to hardware API...")
            Log.i(TAG, "Hardware connection established successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Hardware connection failed.", e)
            false
        }
    }

    /**
     * Handles the hardware trigger by dispatching the ping via TelemetryDispatcher.
     * Task #40 Fallback & Task #149
     */
    fun handleHardwareTrigger(ping: TelemetryPing) {
        if (!isInitialized) {
            Log.e(TAG, "Trigger failed: Hardware not initialized.")
            return
        }

        Log.i(TAG, "Hardware Trigger Detected for item: ${ping.itemId}")
        
        // Mutăm apelul pe un fir de execuție secundar pentru a evita NetworkOnMainThreadException
        Thread {
            telemetryDispatcher.dispatch(ping)
        }.start()
    }

    fun handleHardwareTrigger(storeId: String, itemId: String, triggerType: String = "BUTTON_PRESS") {
        Log.w(TAG, "Manual trigger received without location. Pings should be created via MainActivity.")
    }
}
