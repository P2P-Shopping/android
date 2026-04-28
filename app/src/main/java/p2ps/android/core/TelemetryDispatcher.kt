package p2ps.android.core

import android.util.Log
import p2ps.android.data.TelemetryManager
import p2ps.android.data.TelemetryPing
import p2ps.android.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TelemetryDispatcher(
    private val apiClient: ApiClient,
    private val telemetryManager: TelemetryManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "TelemetryDispatcher"

    /**
     * Dispatches a ping. Now a suspend function to integrate with modern Coroutines.
     */
    suspend fun dispatch(ping: TelemetryPing) {
        try {
            // Pas 1: Încercăm să trimitem prin API (suspend call)
            val responseSuccessful = apiClient.sendPing(ping)

            // Pas 2: Dacă a eșuat (false), salvăm local
            if (!responseSuccessful) {
                handleFallback(ping, "API reported failure")
            }
        } catch (e: Exception) {
            // Pas 3: Dacă a dat crash rețeaua, salvăm local
            handleFallback(ping, "Exception: ${e.message}")
        }
    }

    private fun handleFallback(ping: TelemetryPing, reason: String) {
        Log.w(TAG, "Task #40 Fallback: $reason. Saving to local storage via TelemetryManager.")
        telemetryManager.savePing(ping)
    }
}
