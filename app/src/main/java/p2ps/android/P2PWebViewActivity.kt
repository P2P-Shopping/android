package p2ps.android

import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

/**
 * WebView Activity that:
 *  - Registers [P2PJsBridge] so JS can call window.P2PBridge.*   (#217)
 *  - Injects the JWT token after each page load                   (#218)
 *
 * WebView este creat programatic — nu necesită layout XML.
 */
class P2PWebViewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "P2PWebViewActivity"
        private const val WEB_APP_URL = "https://your-web-app.example.com"
        private const val LOGIN_URL = "https://your-web-app.example.com/login"
    }

    private lateinit var webView: WebView
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crează WebView programatic — fără layout XML
        webView = WebView(this)
        setContentView(webView)

        tokenManager = TokenManager(this)

        configureWebView()
        webView.loadUrl(WEB_APP_URL)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(false)
        }

        // #217: Înregistrează JS bridge
        val bridge = P2PJsBridge(this)
        webView.addJavascriptInterface(bridge, "P2PBridge")

        // #218: Injectează token la fiecare page load
        webView.webViewClient = P2PWebViewClient()
    }

    inner class P2PWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest
        ): Boolean = false

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            when {
                url.startsWith(LOGIN_URL) -> {
                    Log.d(TAG, "Pe pagina de login, skip injection")
                }
                tokenManager.isTokenValid() -> {
                    injectToken(view, tokenManager.getToken()!!)
                }
                else -> {
                    Log.d(TAG, "Token lipsă sau expirat — redirect la login")
                    view.loadUrl(LOGIN_URL)
                }
            }
        }
    }

    private fun injectToken(view: WebView, token: String) {
        val safeToken = JSONObject.quote(token)

        val script = """
            (function() {
                try {
                    localStorage.setItem('authToken', ${'$'}safeToken);
                    window.dispatchEvent(
                        new CustomEvent('p2p:tokenReady', { detail: { token: ${'$'}safeToken } })
                    );
                } catch(e) {
                    console.error('[P2P] Token injection failed:', e);
                }
            })();
        """.trimIndent()

        view.evaluateJavascript(script) { result ->
            Log.d(TAG, "Token injection result: ${'$'}result")
        }
    }
}