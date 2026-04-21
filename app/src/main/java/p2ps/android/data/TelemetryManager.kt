package p2ps.android.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.UUID

/**
 * Persists telemetry data locally using SharedPreferences.
 * Ensures data is not lost when the device is offline.
 */
class TelemetryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("telemetry_prefs", Context.MODE_PRIVATE)

    fun savePing(ping: TelemetryPing) {
        val editor = prefs.edit()

        val dataString = JSONObject()
            .put("deviceId", ping.deviceId)
            .put("storeId", ping.storeId)
            .put("itemId", ping.itemId)
            .put("triggerType", ping.triggerType)
            .put("timestamp", ping.timestamp)
            .put("lat", ping.lat)
            .put("lng", ping.lng)
            .put("accuracy", ping.accuracy)
            .toString()

        val uniqueKey = "ping_${ping.timestamp}_${UUID.randomUUID()}"

        editor.putString(uniqueKey, dataString)
        editor.apply()
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
                            accuracy = json.getDouble("accuracy").toFloat()
                        )
                    )
                } catch (e: Exception) {
                    // Skip malformed entries
                }
            }
        }
        return pings
    }
}
