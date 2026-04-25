package io.github.freetimemaker.geoweather.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
actual fun AppWebView(url: String, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    
    // In standard Compose Desktop, there is no built-in WebView.
    // We show a placeholder with a button to open in the system browser.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "WebView not available on Desktop.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { uriHandler.openUri(url) }) {
                Text("Open in Browser")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
