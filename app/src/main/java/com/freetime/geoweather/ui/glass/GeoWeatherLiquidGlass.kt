package com.freetime.geoweather.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import com.freetime.design.LocalFreetimeBackdrop
import com.freetime.design.freetimeBackdropSource
import com.freetime.design.freetimeGlass
import com.freetime.design.freetimeGlassCapsule
import com.freetime.design.rememberFreetimeBackdrop

/**
 * GeoWeather Liquid Glass root.
 *
 * The gradient belongs to the backdrop source itself, so glass surfaces sample
 * real visual content instead of a flat theme color. On Android 13+ the
 * Freetime backdrop applies blur, lens distortion and vibrancy to this layer.
 */
@Composable
fun GeoWeatherGlassRoot(content: @Composable () -> Unit) {
    val backdrop = rememberFreetimeBackdrop()
    val colors = listOf(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.11f),
        MaterialTheme.colorScheme.background
    )

    CompositionLocalProvider(LocalFreetimeBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .freetimeBackdropSource(backdrop)
                    .background(Brush.linearGradient(colors))
            )
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}

@Composable
fun Modifier.geoWeatherGlass(shape: Shape, interactive: Boolean = true): Modifier =
    freetimeGlass(shape = shape, interactive = interactive)

@Composable
fun Modifier.geoWeatherGlassCapsule(interactive: Boolean = true): Modifier =
    freetimeGlassCapsule(interactive = interactive)
