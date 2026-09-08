package com.freetime.geoweather.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformWebView(url: String, modifier: Modifier = Modifier)
