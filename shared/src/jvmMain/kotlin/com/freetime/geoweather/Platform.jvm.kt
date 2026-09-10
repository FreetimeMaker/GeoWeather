package com.freetime.geoweather

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    val uri = runCatching { URI(url) }.getOrNull() ?: return
    val osName = System.getProperty("os.name", "").lowercase()

    // java.awt.Desktop.browse() can report itself as supported on Linux but
    // still throw when no desktop integration/browser handler is available.
    // Prefer the platform launchers there and never let link opening crash the app.
    if (osName.contains("linux")) {
        if (runCatching { ProcessBuilder("xdg-open", url).start() }.isSuccess) return
        if (runCatching { ProcessBuilder("gio", "open", url).start() }.isSuccess) return
    }

    runCatching {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri)
            }
        }
    }
}

actual val isDesktop: Boolean = true

@Composable
actual fun rememberPaymentContext(): Any? = null

actual suspend fun getCurrentCoordinates(): Pair<Double, Double>? = null

actual fun copyToClipboard(text: String) {
    try {
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(
            java.awt.datatransfer.StringSelection(text), null
        )
    } catch (_: Exception) {
    }
}
