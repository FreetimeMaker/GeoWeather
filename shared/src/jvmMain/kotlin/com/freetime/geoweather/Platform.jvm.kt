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

actual suspend fun getCurrentCoordinates(): Pair<Double, Double>? = null

actual fun applyAppLanguage(language: String) {
    // Desktop follows the system locale; choice is persisted only.
}

actual fun copyToClipboard(text: String) {
    try {
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(
            java.awt.datatransfer.StringSelection(text), null
        )
    } catch (_: Exception) {
    }
}
