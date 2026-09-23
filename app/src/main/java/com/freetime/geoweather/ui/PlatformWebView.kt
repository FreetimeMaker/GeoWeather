package com.freetime.geoweather.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun PlatformWebView(url: String, modifier: Modifier, html: String? = null) {
    AndroidView(
        factory = { context ->
            PrivacyWebView(context).apply {
                settings.javaScriptEnabled = true
                if (html != null) loadDataWithBaseURL("https://www.rainviewer.com", html, "text/html", "UTF-8", null) else loadUrl(url)
            }
        },
        update = { webView ->
            if (html != null) webView.loadDataWithBaseURL("https://www.rainviewer.com", html, "text/html", "UTF-8", null)
            else if (webView.url != url) webView.loadUrl(url)
        },
        modifier = modifier
    )
}
