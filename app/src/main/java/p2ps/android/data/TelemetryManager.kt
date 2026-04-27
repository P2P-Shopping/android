package p2ps.android.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Persists telemetry data locally using Room Database.
 * Ensures data is not lost when the device is offline.
 */
class TelemetryManager(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val telemetryDao = db.telemetryDao()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun savePing(ping: TelemetryPing) {
        val entity = TelemetryEntity(
            deviceId = ping.deviceId,
            storeId = ping.storeId,
            itemId = ping.itemId,
            triggerType = ping.triggerType,
            latitude = ping.lat,
            longitude = ping.lng,
            accuracy = ping.accuracy,
            timestamp = ping.timestamp
        )

        scope.launch {
            telemetryDao.insertPing(entity)
            android.util.Log.d("TelemetryManager", "Ping cached offline in Room DB: ${ping.timestamp}")
        }
    }

    suspend fun getAllStoredPings(): List<TelemetryEntity> {
        return telemetryDao.getAllPings()
    }
}