package io.github.freetimemaker.geoweather.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun platformColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    return getFallbackColorScheme(darkTheme)
}
