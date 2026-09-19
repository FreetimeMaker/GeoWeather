package com.freetime.geoweather.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import me.free_time.design.FreetimeGlassRoot
import me.free_time.design.freetimeGlass
import me.free_time.design.freetimeGlassCapsule

/**
 * GeoWeather compatibility aliases backed by the shared Freetime Design library.
 * Keeping these names avoids duplicating the Liquid Glass implementation while
 * allowing existing screens to migrate without unnecessary churn.
 */
@Composable
fun GeoWeatherGlassRoot(content: @Composable () -> Unit) {
    FreetimeGlassRoot(content)
}

@Composable
fun Modifier.geoWeatherGlass(shape: Shape, interactive: Boolean = true): Modifier =
    freetimeGlass(shape = shape, interactive = interactive)

@Composable
fun Modifier.geoWeatherGlassCapsule(interactive: Boolean = true): Modifier =
    freetimeGlassCapsule(interactive = interactive)
