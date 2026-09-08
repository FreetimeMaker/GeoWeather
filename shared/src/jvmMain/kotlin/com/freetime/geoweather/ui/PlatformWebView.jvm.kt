package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.awt.Desktop
import java.net.URI

@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Opening radar in browser...")
    }
    
    LaunchedEffect(url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
