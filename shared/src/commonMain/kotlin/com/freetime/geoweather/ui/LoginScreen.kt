package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.auth.AuthManager
import com.freetime.geoweather.getPlatform
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import geoweather.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreen(authManager: AuthManager) {
    val platform = remember { getPlatform() }
    var webViewUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Android: Embedded WebView
    // Desktop: System Browser (due to JCEF binary download issues/crashes)
    val useEmbeddedWebView = platform.name.contains("Android")

    if (webViewUrl != null && useEmbeddedWebView) {
        val state = rememberWebViewState(webViewUrl!!)
        
        LaunchedEffect(state.lastLoadedUrl) {
            val url = state.lastLoadedUrl ?: return@LaunchedEffect
            if (url.contains("geoweather://login-callback")) {
                authManager.handleRedirect(url)
                webViewUrl = null
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            WebView(state = state, modifier = Modifier.fillMaxSize())
            
            FilledIconButton(
                onClick = { webViewUrl = null },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(Res.string.login_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            ElevatedButton(
                onClick = {
                    val url = authManager.getGitHubLoginUrl()
                    if (useEmbeddedWebView) webViewUrl = url else platform.openUrl(url)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(Res.string.login_github))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ElevatedButton(
                onClick = {
                    val url = authManager.getGitLabLoginUrl()
                    if (useEmbeddedWebView) webViewUrl = url else platform.openUrl(url)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(Res.string.login_gitlab))
            }
        }
    }
}
