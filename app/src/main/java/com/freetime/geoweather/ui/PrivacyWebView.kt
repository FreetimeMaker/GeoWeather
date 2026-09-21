package com.freetime.geoweather.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.freetime.browser.FreetimeBrowser

class PrivacyWebView(context: Context, attrs: AttributeSet? = null) : WebView(context, attrs) {
    var disablePrivateView: Boolean = false
        set(value) {
            field = value
            setupPrivacySettings()
        }

    var openLinksInExternalBrowser: Boolean = false

    init {
        setupPrivacySettings()
        clearAllData()
        webViewClient = PrivacyClient(context)
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
        CookieManager.getInstance().apply {
            setAcceptCookie(disablePrivateView)
            setAcceptThirdPartyCookies(this@PrivacyWebView, disablePrivateView)
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

    private inner class PrivacyClient(private val context: Context) : WebViewClient() {
        private val blockedHosts = listOf(
            "google-analytics.com",
            "doubleclick.net",
            "facebook.net",
            "googletagmanager.com",
            "adservice.google.com",
            "ads.yahoo.com",
            "bing.com/track"
        )

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url?.toString() ?: return false
            if (openLinksInExternalBrowser) {
                FreetimeBrowser.openExternal(context, url)
                return true
            }
            return false
        }

        @SuppressLint("NewApi")
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val url = request?.url ?: return super.shouldInterceptRequest(view, request)
            val host = url.host ?: return super.shouldInterceptRequest(view, request)
            if (!disablePrivateView && blockedHosts.any { host.contains(it) }) {
                return WebResourceResponse("text/plain", "utf-8", null)
            }
            val response = super.shouldInterceptRequest(view, request)
            if (!disablePrivateView) response?.responseHeaders?.remove("Set-Cookie")
            return response
        }
    }
}
