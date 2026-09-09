package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.freetime.geoweather.openUrl
import geoweather.shared.generated.resources.Res
import geoweather.shared.generated.resources.opening_default_browser
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.opening_default_browser))
    }
    
    LaunchedEffect(url) {
        openUrl(url)
    }
}
