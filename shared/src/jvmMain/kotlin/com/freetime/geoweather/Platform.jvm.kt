package com.freetime.geoweather

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    }
}

@Composable
actual fun rememberPaymentContext(): Any? = null
