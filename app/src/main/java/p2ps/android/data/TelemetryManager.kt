package p2ps.android.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.UUID

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
}