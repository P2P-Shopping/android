package p2ps.android

import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.AppDatabase
import p2ps.android.data.DeviceIdManager
import p2ps.android.data.TelemetryPing
import java.util.UUID
import org.json.JSONObject
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import android.content.pm.PackageManager
import android.Manifest
import android.util.Base64
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
class WebViewActivity : ComponentActivity() {

    private lateinit var telemetryDispatcher: TelemetryDispatcher
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var webView: WebView
    private lateinit var jsBridge: P2PJsBridge
    private lateinit var cameraGalleryLauncher: ActivityResultLauncher<Intent>

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var progressBar: ProgressBar

    private var currentPhotoUri: Uri? = null
    private var currentCallbackId: String? = null

    companion object {
        private const val KEY_PHOTO_URI = "saved_photo_uri"
        private const val KEY_CALLBACK_ID = "saved_callback_id"
        private const val JS_CALLBACK_NULL = "javascript:window.onNativeImageReceived(null)"
        private const val IP_LOCALHOST = "127.0.0.1" // NOSONAR
        private const val IP_EMULATOR_HOST = "10.0.2.2" // NOSONAR
        private const val HOST_LOCALHOST = "localhost"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_PHOTO_URI, currentPhotoUri)
        outState.putString(KEY_CALLBACK_ID, currentCallbackId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            currentPhotoUri = savedInstanceState.getParcelable(KEY_PHOTO_URI)
            currentCallbackId = savedInstanceState.getString(KEY_CALLBACK_ID)
        }

        setupComponents()
        setupLaunchers()
        setupWebViewConfiguration()
        setupNavigationHandling()

        webView.loadUrl(BuildConfig.DASHBOARD_URL)
    }
    fun setPendingTransaction(callbackId: String?, uri: Uri?) {
        this.currentCallbackId = callbackId
        this.currentPhotoUri = uri
    }

    private fun setupComponents() {
        val database = AppDatabase.getDatabase(this)
        telemetryDispatcher = TelemetryDispatcher(database.telemetryDao(), this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        webView = WebView(this)
        jsBridge = P2PJsBridge(this)

        webView.addJavascriptInterface(jsBridge, "P2PBridge")
        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }

        val rootLayout = FrameLayout(this)
        rootLayout.addView(webView, FrameLayout.LayoutParams(-1, -1))
        rootLayout.addView(progressBar, FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER))
        setContentView(rootLayout)

        this.intent.putExtra("pb_ref", progressBar.id)
    }

    private fun setupLaunchers() {
        cameraGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val dataUri: Uri? = result.data?.data ?: currentPhotoUri
                handleCaptureResult(dataUri, result.data)
            } else {
                handleCaptureResult(null, result.data)
                webView.evaluateJavascript(JS_CALLBACK_NULL, null)            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            if (cameraGranted) {
                jsBridge.openNativeCamera(currentCallbackId)
            } else {
                webView.evaluateJavascript(JS_CALLBACK_NULL, null)
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleCaptureResult(dataUri: Uri?, intentData: Intent?) {
        if (dataUri != null) {
            processAndSendImage(dataUri)
        } else {
            val bitmap = intentData?.extras?.get("data") as? android.graphics.Bitmap
            if (bitmap != null) {
                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                webView.post {
                    webView.evaluateJavascript("javascript:window.onNativeImageReceived('$base64String')", null)
                }
            } else {
                webView.post {
                    webView.evaluateJavascript(JS_CALLBACK_NULL, null)
                }
            }
        }
    }

    private fun setupWebViewConfiguration() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = if (BuildConfig.DEBUG) WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                Log.d("WebViewJS", "${message?.message()}")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (::progressBar.isInitialized) {
                    progressBar.visibility = View.GONE
                }
                injectAutoPingScript(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (isExternalUrl(url)) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        return true
                    } catch (e: Exception) { return false }
                }
                return false
            }
        }
    }

    private fun isExternalUrl(url: String): Boolean {
        val host = Uri.parse(url).host ?: return true
        val authorizedHost = Uri.parse(BuildConfig.DASHBOARD_URL).host ?: return true
        val internalHosts = setOf(authorizedHost, HOST_LOCALHOST, IP_LOCALHOST, IP_EMULATOR_HOST)
        return host !in internalHosts
            }

    private fun setupNavigationHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun injectAutoPingScript(view: WebView?) {
        val js = """
            (function() {
                document.addEventListener('change', function(e) {
                    var target = e.target;
                    if (target.type === 'checkbox' && target.checked) {
                        var itemContainer = target.closest('li') || target.closest('[data-id]');
                        var itemId = itemContainer ? (itemContainer.getAttribute('data-id') || itemContainer.id) : null;
                        if (!itemId) {
                            var nameEl = itemContainer ? itemContainer.querySelector('span') : null;
                            itemId = nameEl ? nameEl.innerText.trim() : 'ui_item_' + Date.now();
                        }
                        var storeId = 'Lidl_Vite_Physical';
                        if (window.AndroidInterface) {
                            window.AndroidInterface.postTelemetry(storeId, itemId, 'WEB_UI_CHECKOFF');
                        }
                    }
                }, true);
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }
    fun isAuthorized(): Boolean {
        val url = webView.url ?: return false
        if (url.startsWith("file:///android_asset/")) return true

        val host = Uri.parse(url).host ?: return false

        val authorizedHost = Uri.parse(BuildConfig.DASHBOARD_URL).host
        if (host == authorizedHost) return true

        if (BuildConfig.DEBUG) {
            val debugHosts = listOf(HOST_LOCALHOST, IP_LOCALHOST, IP_EMULATOR_HOST)
            return debugHosts.contains(host)
        }

        return false
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun postTelemetry(storeId: String, itemId: String, triggerType: String) {
            runOnUiThread {
                if (isAuthorized()) {
                    sendPing(storeId, itemId, triggerType)
                }
            }
        }

        @JavascriptInterface
        fun getDeviceId(): String = if (isAuthorized()) DeviceIdManager.getDeviceId(this@WebViewActivity) else ""

        @JavascriptInterface
        fun getPlatform(): String = "android"
    }

    private fun sendPing(storeId: String, itemId: String, triggerType: String) {
        val deviceId = DeviceIdManager.getDeviceId(this)
        val cts = CancellationTokenSource()

        val timeoutJob = lifecycleScope.launch {
            kotlinx.coroutines.delay(10000) // 10s timeout
            if (!cts.token.isCancellationRequested) cts.cancel()
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnCompleteListener { timeoutJob.cancel() }
                .addOnSuccessListener { location ->
                    if (location != null) {
                        dispatchPing(deviceId, storeId, itemId, triggerType, location.latitude, location.longitude, location.accuracy)
                    } else {
                        tryLastLocationFallback(deviceId, storeId, itemId, triggerType)
                    }
                }
                .addOnFailureListener { tryLastLocationFallback(deviceId, storeId, itemId, triggerType) }
                .addOnCanceledListener { tryLastLocationFallback(deviceId, storeId, itemId, triggerType) }
        } catch (e: SecurityException) {
            timeoutJob.cancel()
            dispatchPing(deviceId, storeId, itemId, triggerType, 0.0, 0.0, 0f)
        }
    }

    private fun tryLastLocationFallback(deviceId: String, storeId: String, itemId: String, triggerType: String) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { lastLoc ->
                    val lat = lastLoc?.latitude ?: 0.0
                    val lng = lastLoc?.longitude ?: 0.0
                    val acc = lastLoc?.accuracy ?: 0f
                    dispatchPing(deviceId, storeId, itemId, triggerType, lat, lng, acc)
                }
                .addOnFailureListener { dispatchPing(deviceId, storeId, itemId, triggerType, 0.0, 0.0, 0f) }
        } catch (e: SecurityException) {
            dispatchPing(deviceId, storeId, itemId, triggerType, 0.0, 0.0, 0f)
        }
    }

    private fun dispatchPing(deviceId: String, storeId: String, itemId: String, triggerType: String, lat: Double, lng: Double, acc: Float) {
        if (itemId.isBlank()) return
        val ping = TelemetryPing(deviceId, storeId, itemId, triggerType, lat, lng, acc, System.currentTimeMillis(), UUID.randomUUID().toString())
        lifecycleScope.launch {
            telemetryDispatcher.dispatch(ping)
            Log.i("WebViewActivity", "SUCCESS: Telemetry saved for $itemId")
        }
    }
    private fun processAndSendImage(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.use { it.readBytes() }

                if (bytes != null) {
                    val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        webView.evaluateJavascript("javascript:window.onNativeImageReceived('$base64String')", null)
                    }
                    Log.d("P2P_Telemetry", "Image sent successfully")
                } else {
                    throw Exception("Failed to read bytes from URI")
                }
            } catch (e: Exception) {
                Log.e("P2P_Telemetry", "Error processing URI: ${e.message}")

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    webView.evaluateJavascript(JS_CALLBACK_NULL, null)
                }
            }
        }
    }
    fun launchNativePicker(intent: Intent) {
        cameraGalleryLauncher.launch(intent)
    }

    fun requestCameraPermissions(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("P2PBridge")
        webView.removeJavascriptInterface("AndroidInterface")

        webView.stopLoading()
        webView.loadUrl("about:blank")
        webView.destroy()

        super.onDestroy()
    }

}
