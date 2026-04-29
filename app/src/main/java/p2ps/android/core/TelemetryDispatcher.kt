package p2ps.android.core

import android.content.Context
import android.util.Log
import p2ps.android.data.TelemetryDao
import p2ps.android.data.TelemetryPing
import p2ps.android.data.toEntity

class TelemetryDispatcher(
    private val telemetryDao: TelemetryDao,
    private val context: Context
) {
    private val TAG = "TelemetryDispatcher"

    /**
     * Dispatches a ping.
     * Adapted for the Offline-First architecture (Task #184).
     */
    suspend fun dispatch(ping: TelemetryPing) {
        try {
            Log.d(TAG, "Saving ping locally for batch sync: ${ping.itemId}")

            // Convert the network model to the database entity and save it safely offline
            val entity = ping.toEntity()
            telemetryDao.insertPing(entity)

            // Notify the WorkManager that new data is available
            // The sync job will remain pending until an active internet connection is detected
            scheduleTelemetrySync(context)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save ping locally: ${e.message}")
        }
    }
}