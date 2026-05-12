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



    @JavascriptInterface
    fun getDeviceId(): String {
        val prefs = context.getSharedPreferences("p2ps_device_prefs", Context.MODE_PRIVATE)
        prefs.getString("device_id", null)?.let { return it }
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val deviceId = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") androidId else UUID.randomUUID().toString()
        prefs.edit { putString("device_id", deviceId) }
        return deviceId
    }

    @JavascriptInterface
    fun getPlatform(): String = "android"


    @JavascriptInterface
    fun openNativeCamera(callbackId: Any?) {
        val webActivity = context as? WebViewActivity ?: return
        val idString = callbackId?.toString()

        webActivity.runOnUiThread {

            val hasCameraPermission = ContextCompat.checkSelfPermission(
                webActivity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                dispatchCameraIntent(idString)
            } else {
                webActivity.setPendingTransaction(idString, null)
                webActivity.requestCameraPermissions(arrayOf(Manifest.permission.CAMERA))
            }
        }
    }


    private fun dispatchCameraIntent(callbackId: String?) {
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
                activity.setPendingTransaction(callbackId, uri)

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
