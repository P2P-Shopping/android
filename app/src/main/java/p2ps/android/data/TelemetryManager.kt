package p2ps.android.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.*
import android.util.Log

/**
 * Persists telemetry data locally using Room Database.
 * Ensures data is not lost when the device is offline.
 */
class TelemetryManager(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val telemetryDao = db.telemetryDao()
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("TelemetryManager", "Coroutine failed: ${throwable.message}", throwable)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + errorHandler)
    fun savePing(ping: TelemetryPing) {
        val entity = ping.toEntity()

        scope.launch {
            try {
                telemetryDao.insertPing(entity)
                Log.d("TelemetryManager", "Ping cached offline: ${ping.timestamp}")
            } catch (e: Exception) {
                Log.e("TelemetryManager", "Failed to insert ping into Room", e)
            }
        }
    }

    suspend fun getAllStoredPings(): List<TelemetryEntity> {
        return telemetryDao.getAllPings()
    }

    suspend fun deletePings(pings: List<TelemetryEntity>) {
        try {
            telemetryDao.deletePings(pings)
            Log.d("TelemetryManager", "Cleared ${pings.size} sent pings from cache.")
        } catch (e: Exception) {
            Log.e("TelemetryManager", "Failed to delete pings from cache", e)
        }
    }
    suspend fun clearAllCache() {
        telemetryDao.clearCache()
    }
}