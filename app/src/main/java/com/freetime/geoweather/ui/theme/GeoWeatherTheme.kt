package com.freetime.geoweather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GeoBlue,
    onPrimary = White,
    secondary = GeoOrange,
    onSecondary = White,
    background = GeoBackground,
    onBackground = GeoText,
)

private val DarkColors = darkColorScheme(
    primary = GeoDarkBlue,
    onPrimary = White,
    secondary = GeoDarkOrange,
    onSecondary = White,
    background = GeoDarkBackground,
    onBackground = GeoDarkText,
)

@Composable
fun GeoWeatherTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = GeoTypography,
        shapes = GeoShapes,
        content = content
    )
}
