package com.freetime.geoweather.ui.glass

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as nativeLayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import kotlinx.coroutines.launch

val LocalGeoWeatherBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

@Composable
fun rememberGeoWeatherBackdrop(): LayerBackdrop? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) rememberLayerBackdrop { drawContent() } else null

fun Modifier.geoWeatherBackdropSource(backdrop: LayerBackdrop?): Modifier =
    if (backdrop == null) this else this.nativeLayerBackdrop(backdrop)

@Composable
fun GeoWeatherGlassRoot(content: @Composable () -> Unit) {
    val backdrop = rememberGeoWeatherBackdrop()
    CompositionLocalProvider(LocalGeoWeatherBackdrop provides backdrop) {
        Box(Modifier.fillMaxSize().geoWeatherBackdropSource(backdrop)) { content() }
    }
}

@Composable
fun Modifier.geoWeatherGlass(shape: Shape, interactive: Boolean = true): Modifier =
    geoWeatherLiquidGlass(LocalGeoWeatherBackdrop.current, shape, interactive)

@Composable
fun Modifier.geoWeatherLiquidGlass(
    backdrop: LayerBackdrop?,
    shape: Shape,
    interactive: Boolean = true,
): Modifier {
    val fallback = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    val glassScrim = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.Black else Color.White
    if (backdrop == null) return this.clip(shape).background(fallback).border(1.dp, outline, shape)

    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    val touch = remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }
    val glass = this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {},
        onDrawSurface = {
            drawRect(glassScrim.copy(alpha = 0.22f))
            val pressed = press.value
            if (pressed > 0f) {
                drawRect(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.18f * pressed), Color.Transparent),
                        center = touch.value.takeUnless { it == Offset.Zero } ?: Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 1.5f
                    ),
                    blendMode = BlendMode.Plus
                )
            }
        }
    ).graphicsLayer {
        val scale = lerp(1f, 1.025f, press.value)
        scaleX = scale
        scaleY = scale
    }
    if (!interactive) return glass
    return glass.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            touch.value = down.position
            scope.launch { press.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 340f)) }
            var pressed = true
            while (pressed) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                if (change == null) pressed = false else {
                    touch.value = change.position
                    pressed = change.pressed
                }
            }
            scope.launch { press.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 300f)) }
        }
    }
}
