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
import kotlinx.coroutines.launch
import p2ps.android.core.TelemetryDispatcher
import p2ps.android.data.AppDatabase
import p2ps.android.data.DeviceIdManager
import p2ps.android.data.TelemetryPing
import java.util.UUID

class WebViewActivity : ComponentActivity() {

    private lateinit var telemetryDispatcher: TelemetryDispatcher
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(this)
        telemetryDispatcher = TelemetryDispatcher(database.telemetryDao(), this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Permite inspectarea WebView din Chrome (chrome://inspect)
        WebView.setWebContentsDebuggingEnabled(true)

        val webView = WebView(this)
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
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                Log.d("WebViewJS", "${message?.message()} -- From line ${message?.lineNumber()} of ${message?.sourceId()}")
                return true
            }
        }
        
        val internalDomain = Uri.parse(BuildConfig.DASHBOARD_URL).host ?: "127.0.0.1"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                Log.i("WebViewActivity", "Page finished loading. Injecting hook...")
                injectAutoPingScript(view)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (!url.contains(internalDomain) && !url.contains("localhost") && !url.contains("127.0.0.1")) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        return true
                    } catch (e: Exception) {
                        return false
                    }
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

        webView.loadUrl(BuildConfig.DASHBOARD_URL)
    }

    private fun injectAutoPingScript(view: WebView?) {
        val js = """
            (function() {
                console.log('P2P: Initializing interaction monitor...');
                
                document.addEventListener('change', function(e) {
                    var target = e.target;
                    if (target.type === 'checkbox' && target.checked) {
                        var itemContainer = target.closest('li') || target.closest('[data-id]');
                        var itemId = null;
                        
                        if (itemContainer) {
                            itemId = itemContainer.getAttribute('data-id') || itemContainer.id;
                            if (!itemId) {
                                var nameEl = itemContainer.querySelector('span');
                                itemId = nameEl ? nameEl.innerText.trim() : null;
                            }
                        }
                        
                        itemId = itemId || 'ui_item_' + Date.now();
                        
                        var storeId = 'Lidl_Vite_Physical';
                        console.log('P2P: Check-off detected for ' + itemId);
                        if (window.AndroidInterface && typeof window.AndroidInterface.postTelemetry === 'function') {
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
            Log.i("WebViewActivity", "BRIDGE: Received ping from JS for $itemId")
            runOnUiThread {
                sendPing(storeId, itemId, triggerType)
            }
        }

        @JavascriptInterface
        fun getDeviceId(): String = DeviceIdManager.getDeviceId(this@WebViewActivity)

        @JavascriptInterface
        fun getPlatform(): String = "android"
    }

    private fun sendPing(storeId: String, itemId: String, triggerType: String) {
        val deviceId = DeviceIdManager.getDeviceId(this)
        
        // Încercăm să obținem locația curentă cu prioritate mare
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    val finalLocation = location ?: null // Putem adăuga un fallback la lastLocation aici
                    
                    if (finalLocation != null) {
                        dispatchPing(deviceId, storeId, itemId, triggerType, finalLocation.latitude, finalLocation.longitude, finalLocation.accuracy)
                    } else {
                        // Fallback: Încercăm ultima locație cunoscută dacă cea curentă e null
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                dispatchPing(deviceId, storeId, itemId, triggerType, lastLoc.latitude, lastLoc.longitude, lastLoc.accuracy)
                            } else {
                                Log.w("WebViewActivity", "Location unavailable, sending ping with 0,0")
                                dispatchPing(deviceId, storeId, itemId, triggerType, 0.0, 0.0, 0f)
                            }
                        }
                    }
                }
        } catch (e: SecurityException) {
            Log.e("WebViewActivity", "Location permission missing during ping", e)
        }
    }

    private fun dispatchPing(deviceId: String, storeId: String, itemId: String, triggerType: String, lat: Double, lng: Double, acc: Float) {
        if (itemId.isBlank()) {
            Log.w("WebViewActivity", "ABORT: itemId is blank, skipping telemetry")
            return
        }

        val ping = TelemetryPing(
            deviceId = deviceId,
            storeId = storeId,
            itemId = itemId,
            triggerType = triggerType,
            lat = lat,
            lng = lng,
            accuracyMeters = acc,
            timestamp = System.currentTimeMillis(),
            pingId = UUID.randomUUID().toString()
        )

        lifecycleScope.launch {
            telemetryDispatcher.dispatch(ping)
            Log.i("WebViewActivity", "SUCCESS: Ping saved to DB for $itemId")
            Toast.makeText(this@WebViewActivity, "Ping înregistrat: $itemId", Toast.LENGTH_SHORT).show()
        }
    }
}
