package com.freetime.geoweather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GeoTypography,
        shapes = GeoShapes,
        content = content
    )
}
