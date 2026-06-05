package com.freetime.geoweather
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
class PrivacyWebView(context: Context, attrs: AttributeSet? = null) :
    WebView(context, attrs) {
    var disablePrivateView: Boolean = false
    var openLinksInExternalBrowser: Boolean = false
    init {
        setupPrivacySettings()
        clearAllData()
        webViewClient = PrivacyClient(context, this)
    }
    @SuppressLint("NewApi")
    private fun setupPrivacySettings() {
        settings.apply {
            javaScriptEnabled = false
            domStorageEnabled = disablePrivateView
            allowFileAccess = false
            allowContentAccess = false
            userAgentString = "Mozilla/5.0"
        }
        if (!disablePrivateView) {
            CookieManager.getInstance().apply {
                setAcceptCookie(false)
                setAcceptThirdPartyCookies(this@PrivacyWebView, false)
            }
        } else {
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@PrivacyWebView, true)
            }
        }
    }
    private fun clearAllData() {
        clearCache(true)
        clearHistory()
        if (!disablePrivateView) {
            CookieManager.getInstance().apply {
                removeAllCookies(null)
                flush()
            }
        }
    }
    private inner class PrivacyClient(private val context: Context, private val webView: WebView) : WebViewClient() {
        private val blockedHosts = listOf(
            "google-analytics.com",
            "doubleclick.net",
            "facebook.net",
            "googletagmanager.com",
            "adservice.google.com",
            "ads.yahoo.com",
            "bing.com/track"
        )
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url?.toString() ?: return false
            // Open in external browser if setting is enabled
            if (openLinksInExternalBrowser) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
                return true
            }
            return false
        }
        @SuppressLint("NewApi")
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url ?: return super.shouldInterceptRequest(view, request)
            val host = url.host ?: return super.shouldInterceptRequest(view, request)
            // Block trackers only if private view is enabled
            if (!disablePrivateView && blockedHosts.any { host.contains(it) }) {
                return WebResourceResponse("text/plain", "utf-8", null)
            }
            // Set-Cookie Header entfernen (nur bei Private View)
            if (!disablePrivateView) {
                val response = super.shouldInterceptRequest(view, request)
                response?.responseHeaders?.remove("Set-Cookie")
                return response
            }
            return super.shouldInterceptRequest(view, request)
        }
    }
}
