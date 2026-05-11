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

class WebViewActivity : ComponentActivity() {

    private lateinit var telemetryDispatcher: TelemetryDispatcher
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        telemetryDispatcher = TelemetryDispatcher(database.telemetryDao(), this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView = WebView(this)
        val progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }

        val rootLayout = FrameLayout(this)
        rootLayout.addView(webView, FrameLayout.LayoutParams(-1, -1))
        rootLayout.addView(progressBar, FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER))
        setContentView(rootLayout)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = if (BuildConfig.DEBUG) {
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
        }

        // Folosim interfața clasică dar cu securitate sporită și stabilitate asincronă
        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                Log.d("WebViewJS", "${message?.message()}")
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                injectAutoPingScript(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val authorizedHost = Uri.parse(BuildConfig.DASHBOARD_URL).host
                if (authorizedHost != null && !url.contains(authorizedHost) && 
                    !url.contains("localhost") && !url.contains("127.0.0.1") && !url.contains("10.0.2.2")) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        return true
                    } catch (e: Exception) { return false }
                }
                return false
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })


        // Check if launched from a proximity notification with a deep link
        val deepLink = intent?.getStringExtra("deepLink")
        val urlToLoad = deepLink ?: BuildConfig.DASHBOARD_URL
        webView.loadUrl(urlToLoad)
    }

    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onPause() { super.onPause(); webView.onPause() }
    override fun onDestroy() {
        webView.apply {
            removeJavascriptInterface("AndroidInterface")
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val deepLink = intent.getStringExtra("deepLink")
        if (!deepLink.isNullOrBlank()) {
            webView.loadUrl(deepLink)
        }
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

    inner class WebAppInterface {
        @JavascriptInterface
        fun postTelemetry(storeId: String, itemId: String, triggerType: String) {
            // Validarea originii se face acum pe thread-ul principal pentru siguranță
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

        private fun isAuthorized(): Boolean {
            val url = webView.url ?: return false
            val host = Uri.parse(url).host ?: return false
            val authorizedHost = Uri.parse(BuildConfig.DASHBOARD_URL).host
            return host == authorizedHost || host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2"
        }
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
}
