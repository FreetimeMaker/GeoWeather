package com.freetime.geoweather.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.freetime.browser.BrowserMode
import com.freetime.browser.BrowserOptions
import com.freetime.browser.FreetimeBrowserScreen

@Composable
fun WebScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    FreetimeBrowserScreen(
        initialUrl = url,
        modifier = Modifier,
        options = BrowserOptions(mode = BrowserMode.IN_APP),
        title = title,
        javaScriptEnabled = true,
        onClose = onBack,
    )
}
