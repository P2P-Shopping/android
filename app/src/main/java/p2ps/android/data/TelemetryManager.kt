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

        val dataString = JSONObject()
            .put("deviceId", ping.deviceId)
            .put("storeId", ping.storeId)
            .put("itemId", ping.itemId)
            .put("triggerType", ping.triggerType)
            .put("timestamp", ping.timestamp)
            .put("lat", ping.lat)
            .put("lng", ping.lng)
            .put("accuracyMeters", ping.accuracyMeters)
            .toString()

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

    fun getAllStoredPings(): List<TelemetryPing> {
        val allEntries = prefs.all
        val pings = mutableListOf<TelemetryPing>()
        
        allEntries.forEach { (_, value) ->
            if (value is String) {
                try {
                    val json = JSONObject(value)
                    pings.add(
                        TelemetryPing(
                            deviceId = json.getString("deviceId"),
                            storeId = json.getString("storeId"),
                            itemId = json.getString("itemId"),
                            triggerType = json.getString("triggerType"),
                            timestamp = json.getLong("timestamp"),
                            lat = json.getDouble("lat"),
                            lng = json.getDouble("lng"),
                            accuracyMeters = json.getDouble("accuracyMeters").toFloat()
                        )
                    )
                } catch (e: Exception) {
                    // Skip malformed entries
                }
            }
        }
    }
}