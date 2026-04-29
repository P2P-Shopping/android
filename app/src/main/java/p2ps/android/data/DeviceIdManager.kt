package p2ps.android.data

import android.content.Context
import java.util.UUID

object DeviceIdManager {
    private const val PREFS_NAME = "p2ps_prefs"
    private const val KEY_DEVICE_ID = "device_id_uuid"

    /**
     * Returns a unique UUID for this device. 
     * Generates a new one and saves it persistently on the first call.
     */
    fun getDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        
        return deviceId
    }
}
