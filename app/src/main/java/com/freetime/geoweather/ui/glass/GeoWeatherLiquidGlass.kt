package com.freetime.geoweather.ui.glass

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.launch

val LocalGeoWeatherBackdrop = staticCompositionLocalOf<LiquidState?> { null }

@Composable
fun rememberGeoWeatherBackdrop(): LiquidState = rememberLiquidState()

fun Modifier.geoWeatherBackdropSource(backdrop: LiquidState?): Modifier =
    if (backdrop == null) this else this.liquefiable(backdrop)

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
    backdrop: LiquidState?,
    shape: Shape,
    interactive: Boolean = true,
): Modifier {
    val fallback = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    if (backdrop == null) return this.clip(shape).background(fallback).border(1.dp, outline, shape)

    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    val touch = remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }

    val glass = this
        .liquid(backdrop) {
            shape = shape
        }
        .graphicsLayer {
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
                if (change == null) {
                    pressed = false
                } else {
                    touch.value = change.position
                    pressed = change.pressed
                }
            }
            scope.launch { press.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 300f)) }
        }
    }
}
