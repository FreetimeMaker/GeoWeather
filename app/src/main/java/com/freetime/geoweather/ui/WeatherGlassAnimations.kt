package com.freetime.geoweather.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedWeatherGlass(code: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "weatherScene")
    val drift by transition.animateFloat(-28f, 28f, infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
    val pulse by transition.animateFloat(.9f, 1.1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val fall by transition.animateFloat(0f, 500f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "fall")
    val rotate by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "rotate")
    val wave by transition.animateFloat(0f, (PI * 2).toFloat(), infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "wave")
    val flash by transition.animateFloat(0f, 1f, infiniteRepeatable(keyframes {
        durationMillis = 4200
        0f at 0; 0f at 3100; 1f at 3150; .08f at 3230; .85f at 3310; 0f at 3420
    }), label = "flash")

    Box(modifier.height(170.dp).geoWeatherGlass(CircleShape, interactive = false)) {
        Canvas(Modifier.fillMaxSize()) {
            when (code) {
                0, 1 -> { // sun: real rays rotate, glass glow underneath
                    val r = 35.dp.toPx()
                    repeat(12) { i ->
                        val a = Math.toRadians((i * 30f + rotate).toDouble())
                        drawLine(
                            Color(0xFFFFC94A).copy(alpha = .75f),
                            Offset(center.x + cos(a).toFloat() * r * 1.35f, center.y + sin(a).toFloat() * r * 1.35f),
                            Offset(center.x + cos(a).toFloat() * r * 1.75f, center.y + sin(a).toFloat() * r * 1.75f),
                            3.dp.toPx()
                        )
                    }
                    drawCircle(Color(0xFFFFC94A).copy(alpha = .82f), r * pulse, center)
                    drawCircle(Color.White.copy(alpha = .16f), r * 1.7f * pulse, center)
                }
                2, 3 -> { // clouds physically drift over the glass
                    repeat(5) { i ->
                        val baseX = size.width * (.12f + i * .2f) + drift * if (i % 2 == 0) 1 else -.7f
                        val baseY = size.height * (.42f + (i % 2) * .15f)
                        drawCircle(Color.LightGray.copy(alpha = .72f), 25.dp.toPx(), Offset(baseX, baseY))
                        drawCircle(Color.White.copy(alpha = .62f), 19.dp.toPx(), Offset(baseX + 24f, baseY + 8f))
                    }
                }
                45, 48 -> { // fog bands
                    repeat(5) { i ->
                        val y = size.height * (.25f + i * .13f)
                        drawLine(Color.White.copy(alpha = .35f), Offset(-30f + drift, y), Offset(size.width + drift, y), 8.dp.toPx())
                    }
                }
                in 51..67, in 80..82 -> { // rain + splashes
                    repeat(14) { i ->
                        val x = size.width * (i + 1) / 15f + sin(i.toFloat()) * 12f
                        val y = (fall + i * 43f) % (size.height + 40f) - 20f
                        drawLine(Color(0xFF7CC7FF).copy(alpha = .75f), Offset(x, y), Offset(x - 8f, y + 25f), 2.5.dp.toPx())
                        if (y > size.height - 30f) drawCircle(Color(0xFF7CC7FF).copy(alpha = .35f), 7.dp.toPx(), Offset(x, size.height - 8f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
                in 71..77, in 85..86 -> { // snow flakes fall and sway
                    repeat(16) { i ->
                        val y = (fall * .55f + i * 37f) % (size.height + 30f) - 15f
                        val x = size.width * (i + 1) / 17f + sin(wave + i) * 18f
                        drawCircle(Color.White.copy(alpha = .82f), (2 + i % 3).dp.toPx(), Offset(x, y))
                    }
                }
                in 95..99 -> { // storm: rain plus irregular lightning
                    drawRect(Color.White.copy(alpha = flash * .48f))
                    repeat(10) { i ->
                        val x = size.width * (i + 1) / 11f
                        val y = (fall * 1.4f + i * 41f) % size.height
                        drawLine(Color(0xFF7CC7FF).copy(alpha = .65f), Offset(x, y), Offset(x - 10f, y + 28f), 3.dp.toPx())
                    }
                    if (flash > .1f) {
                        val p = Path().apply {
                            moveTo(center.x + 15f, 12f); lineTo(center.x - 12f, 65f)
                            lineTo(center.x + 9f, 65f); lineTo(center.x - 24f, 140f)
                        }
                        drawPath(p, Color(0xFFFFF59D).copy(alpha = .95f), style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.weatherFloatAnimation(): Modifier {
    val transition = rememberInfiniteTransition(label = "weatherFloat")
    val offset by transition.animateFloat(-5f, 5f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "offset")
    val scale by transition.animateFloat(.97f, 1.03f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
    return graphicsLayer { translationY = offset; scaleX = scale; scaleY = scale }
}
