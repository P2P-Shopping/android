package p2ps.android

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.TelemetryPing

/**
 * Manages interactions with physical hardware and coordinates telemetry dispatch.
 * Uses Kotlin Coroutines for safe, non-blocking asynchronous execution.
 */
class HardwareManager(
    private val telemetryDispatcher: TelemetryDispatcher,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val TAG = "HardwareManager"
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        Log.d(TAG, "Initializing Hardware SDK...")
        isInitialized = true
    }

    /**
     * Handles hardware triggers. Dispatches the ping on a background coroutine
     * to prevent NetworkOnMainThreadException and UI freezes.
     */
    fun handleHardwareTrigger(ping: TelemetryPing) {
        if (!isInitialized) {
            Log.e(TAG, "Trigger failed: Hardware not initialized.")
            return
        }

        Log.i(TAG, "Hardware Trigger Detected: ${ping.itemId}")
        
        // Lansăm o corutină pe un thread de fundal
        scope.launch {
            telemetryDispatcher.dispatch(ping)
        }
    }

    fun handleHardwareTrigger(storeId: String, itemId: String, triggerType: String = "BUTTON_PRESS") {
        Log.w(TAG, "Manual trigger received without location.")
    }
}
