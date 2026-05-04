package p2ps.android

import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest

class WebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)

        val progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }

        val rootLayout = FrameLayout(this)

        rootLayout.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        rootLayout.addView(progressBar, FrameLayout.LayoutParams(
            150, 150, android.view.Gravity.CENTER
        ))

        setContentView(rootLayout)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val uri = request.url

                if (url.startsWith("mailto:") || url.startsWith("tel:")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    } catch (e: Exception) {
                    }
                    return true
                }

                val internalDomain = "10.0.2.2"

                return if (!url.contains(internalDomain)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    } catch (e: Exception) {
                    }
                    true
                } else {
                    false
                }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        webView.loadUrl(BuildConfig.DASHBOARD_URL)
    }
}