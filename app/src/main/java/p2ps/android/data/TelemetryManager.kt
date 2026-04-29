package p2ps.android.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import p2ps.android.core.scheduleTelemetrySync

/**
 * Persists telemetry data locally using Room Database.
 * Ensures data is not lost when the device is offline.
 */
class TelemetryManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val telemetryDao = db.telemetryDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Saves a telemetry ping to the local Room database.
     */
    fun savePing(ping: TelemetryPing) {
        val entity = TelemetryEntity(
            deviceId = ping.deviceId,
            storeId = ping.storeId,
            itemId = ping.itemId,
            triggerType = ping.triggerType,
            latitude = ping.lat,
            longitude = ping.lng,
            accuracy = ping.accuracyMeters,
            timestamp = ping.timestamp,
            pingId = ping.pingId
        )

        scope.launch {
            try {
                telemetryDao.insertPing(entity)
                Log.d("TelemetryManager", "Ping cached offline: ${ping.timestamp}")
                
                // Trigger sync automatically
                scheduleTelemetrySync(context)
            } catch (e: Exception) {
                Log.e("TelemetryManager", "Failed to insert ping into Room", e)
            }
        }
    }

    /**
     * Retrieves all stored pings from the database.
     */
    suspend fun getAllStoredPings(): List<TelemetryEntity> {
        return telemetryDao.getAllPings()
    }

    /**
     * Deletes a list of pings from the database.
     */
    suspend fun deletePings(pings: List<TelemetryEntity>) {
        telemetryDao.deletePings(pings)
    }

    /**
     * Clears all cached pings.
     */
    suspend fun clearCache() {
        telemetryDao.clearCache()
    }
}
