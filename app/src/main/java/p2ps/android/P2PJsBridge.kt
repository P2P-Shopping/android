package p2ps.android

import android.content.Context
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.content.edit
import java.util.UUID
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import p2ps.android.WebViewActivity

class P2PJsBridge(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "p2ps_device_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        const val CAMERA_REQ_CODE = 101
        var lastCallbackId: String? = null
        const val PICK_IMAGE_REQ_CODE = 201
        var photoUri: android.net.Uri? = null
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


    @JavascriptInterface
    fun openNativeCamera(callbackId: Any?) {
        val webActivity = context as? WebViewActivity ?: return
        lastCallbackId = callbackId.toString()
        webActivity.runOnUiThread {

            val hasCameraPermission = ContextCompat.checkSelfPermission(
                webActivity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                Log.d("P2PBridge", "Permission already exists. Starting camera...")
                dispatchCameraIntent()
            } else {
                Log.d("P2PBridge", "No permission. Please give permission...")
                webActivity.requestCameraPermissions(arrayOf(Manifest.permission.CAMERA))
            }
        }
    }


    private fun dispatchCameraIntent() {
        val activity = context as? WebViewActivity ?: return
        activity.runOnUiThread {
            try {
                val storageDir = activity.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: return@runOnUiThread

                val photoFile = java.io.File.createTempFile(
                    "JPEG_${System.currentTimeMillis()}_",
                    ".jpg",
                    storageDir
                )
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    activity,
                    "p2ps.android.fileprovider",
                    photoFile
                )
                photoUri = uri

                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }

                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                val chooser = Intent.createChooser(galleryIntent, "Select image source")
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

                activity.launchNativePicker(chooser)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
