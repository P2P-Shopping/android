package p2ps.android

import android.util.Log
import p2ps.android.data.TelemetryPing
import kotlinx.coroutines.*

/**
 * Manages interactions with physical hardware and coordinates telemetry dispatch.
 */
class HardwareManager(
    private val externalScope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val apiClient: ApiClient = ApiClient() // Îl mutăm aici în constructor
) {
    private val TAG = "HardwareManager"
    private var isInitialized = false

    // 1. Initializing the SDK
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
     * Handles the hardware trigger by dispatching the ping to the backend.
     * Task #149
     * Note: Location is now passed from the caller to ensure freshness and permissions.
     */
    fun handleHardwareTrigger(ping: TelemetryPing) {
        if (!isInitialized) {
            Log.e(TAG, "Trigger failed: Hardware not initialized.")
            return
        }

        Log.i(TAG, "Hardware Trigger Detected for item: ${ping.itemId}")
        val scope = externalScope ?: CoroutineScope(dispatcher)

        scope.launch {
            try {
                apiClient.sendPing(ping)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send ping from hardware trigger", e)
            }
        }
    }

    // Deprecated raw trigger for backward compatibility or direct simulation without location
    fun handleHardwareTrigger(storeId: String, itemId: String, triggerType: String = "BUTTON_PRESS") {
        Log.w(TAG, "Manual trigger received without location. Pings should be created via MainActivity.")
    }
}