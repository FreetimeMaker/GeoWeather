package com.freetime.geoweather.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            PrivacyWebView(context).apply {
                settings.javaScriptEnabled = true
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}
