package com.freetime.geoweather.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import kotlin.math.sin

@Composable
fun AnimatedWeatherGlass(code: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "weatherGlass")
    val drift by transition.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val fall by transition.animateFloat(
        initialValue = -20f, targetValue = 120f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "fall"
    )

    Box(
        modifier = modifier
            .height(150.dp)
            .geoWeatherGlass(CircleShape, interactive = false)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (code) {
                0, 1 -> { // clear / mostly clear: glowing glass sun
                    drawCircle(Color.White.copy(alpha = 0.20f), radius = 38.dp.toPx() * pulse, center = center)
                    drawCircle(Color.White.copy(alpha = 0.10f), radius = 58.dp.toPx() * pulse, center = center)
                }
                2, 3, 45, 48 -> { // clouds / fog
                    repeat(4) { i ->
                        val x = size.width * (0.18f + i * 0.22f) + drift * (if (i % 2 == 0) 1 else -1)
                        val y = size.height * (0.38f + (i % 2) * 0.16f)
                        drawCircle(Color.White.copy(alpha = 0.16f), 30.dp.toPx(), Offset(x, y))
                    }
                }
                in 51..67, in 80..82 -> { // drizzle / rain
                    repeat(8) { i ->
                        val x = size.width * (i + 1) / 9f + drift
                        val y = (fall + i * 24f) % size.height
                        drawLine(Color.White.copy(alpha = 0.34f), Offset(x, y), Offset(x - 7f, y + 20f), 3.dp.toPx())
                    }
                }
                in 71..77, in 85..86 -> { // snow
                    repeat(10) { i ->
                        val x = size.width * (i + 1) / 11f + sin((fall + i * 19f) / 35f) * 12f
                        val y = (fall + i * 31f) % size.height
                        drawCircle(Color.White.copy(alpha = 0.42f), 3.dp.toPx(), Offset(x, y))
                    }
                }
                in 95..99 -> { // thunderstorm
                    val flash = if (pulse > 1f) 0.30f else 0.08f
                    drawRect(Color.White.copy(alpha = flash))
                    val cx = center.x
                    drawLine(Color.White.copy(alpha = 0.65f), Offset(cx, 25f), Offset(cx - 15f, 65f), 5.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.65f), Offset(cx - 15f, 65f), Offset(cx + 5f, 65f), 5.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.65f), Offset(cx + 5f, 65f), Offset(cx - 20f, 115f), 5.dp.toPx())
                }
            }
        }
    }
}

@Composable
fun Modifier.weatherFloatAnimation(): Modifier {
    val transition = rememberInfiniteTransition(label = "weatherFloat")
    val offset by transition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "offset"
    )
    val scale by transition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    return graphicsLayer {
        translationY = offset
        scaleX = scale
        scaleY = scale
    }
}
