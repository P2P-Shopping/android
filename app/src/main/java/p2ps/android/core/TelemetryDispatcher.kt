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

    fun dispatch(ping: TelemetryPing) {
        scope.launch {
            try {
                val responseSuccessful = apiClient.sendPing(ping)
                if (!responseSuccessful) {
                    handleFallback(ping, "API reported failure")
                }
            } catch (e: Exception) {
                handleFallback(ping, "Exception: ${e.message}")
            }
        }
    }

    private fun handleFallback(ping: TelemetryPing, reason: String) {
        Log.w(TAG, "Task #40 Fallback: $reason. Saving to local storage via TelemetryManager.")
        telemetryManager.savePing(ping)
    }
}