package com.freetime.geoweather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.freetime.geoweather.openUrl

@Composable
actual fun GeoWeatherTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    oledBlack: Boolean,
    content: @Composable () -> Unit
) {
    var colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    if (darkTheme && oledBlack) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black
        )
    }

    val desktopUriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            openUrl(uri)
        }
    }

    CompositionLocalProvider(LocalUriHandler provides desktopUriHandler) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GeoTypography,
            shapes = GeoShapes,
            content = content
        )
    }
}
