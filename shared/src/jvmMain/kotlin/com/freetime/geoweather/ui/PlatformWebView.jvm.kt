package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.freetime.geoweather.openUrl

@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Opening in default browser...")
    }
    
    LaunchedEffect(url) {
        openUrl(url)
    }
}
