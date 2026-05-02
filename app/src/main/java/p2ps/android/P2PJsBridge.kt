package p2ps.android

import android.content.Context
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.content.edit
import java.util.UUID

class P2PJsBridge(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "p2ps_device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }


    @JavascriptInterface
    fun getDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Return cached ID if already stored
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }

        // Resolve ANDROID_ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        // Use ANDROID_ID if valid, otherwise generate a UUID
        val deviceId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            androidId
        } else {
            UUID.randomUUID().toString()
        }

        // Persist so it survives across restarts
        prefs.edit { putString(KEY_DEVICE_ID, deviceId) }

        return deviceId
    }

    @JavascriptInterface
    fun getPlatform(): String = "android"
}