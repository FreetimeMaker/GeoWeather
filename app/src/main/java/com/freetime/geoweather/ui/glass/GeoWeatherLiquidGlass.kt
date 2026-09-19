package com.freetime.geoweather.ui.glass

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.layerBackdrop
import com.kyant.backdrop.rememberLayerBackdrop
import kotlinx.coroutines.launch

val LocalGeoWeatherBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun rememberGeoWeatherBackdrop(): Backdrop? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) rememberLayerBackdrop() else null

fun Modifier.geoWeatherBackdropSource(backdrop: Backdrop?): Modifier =
    if (backdrop != null) layerBackdrop(backdrop) else this

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
    backdrop: Backdrop?,
    shape: Shape,
    interactive: Boolean = true,
): Modifier {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.40f)
    if (backdrop == null) return clip(shape).background(surface.copy(alpha = 0.58f))

    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }

    val glass = drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(8.dp.toPx())
            lens(24.dp.toPx(), 24.dp.toPx())
        },
        layerBlock = {
            val scale = lerp(1f, 1.025f, press.value)
            scaleX = scale
            scaleY = scale
        },
        onDrawSurface = { drawRect(surface) }
    )
    if (!interactive) return glass

    return glass.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            scope.launch { press.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 340f)) }
            var pressed = true
            while (pressed) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                pressed = change?.pressed == true
            }
            scope.launch { press.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 300f)) }
        }
    }
}
