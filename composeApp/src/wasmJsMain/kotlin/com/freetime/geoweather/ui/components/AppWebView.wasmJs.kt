package com.freetime.geoweather.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlView

@Composable
actual fun AppWebView(url: String, modifier: Modifier) {
    HtmlView(
        modifier = modifier,
        factory = {
            val iframe = document.createElement("iframe") as HTMLIFrameElement
            iframe.src = url
            iframe.style.width = "100%"
            iframe.style.height = "100%"
            iframe.style.border = "none"
            iframe
        }
    )
}
