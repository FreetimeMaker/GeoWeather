package com.freetime.geoweather.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun AnimatedWeatherGlass(code: Int, modifier: Modifier = Modifier, windSpeed: Double = 0.0, windDirection: Int = 0, intensity: Float = 1f, night: Boolean = false, moonAge: Double = currentMoonAge()) {
    val transition = rememberInfiniteTransition(label = "weatherScene")
    val windFactor = (1f + (windSpeed / 35.0).toFloat()).coerceIn(1f, 3f)
    val directionFactor = if (windDirection in 90..270) -1f else 1f
    val drift by transition.animateFloat(-28f * windFactor * directionFactor, 28f * windFactor * directionFactor, infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Reverse), label = "drift")
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
            if (night) {
                drawRect(Color(0xFF07162E).copy(alpha = .30f))
                repeat(18) { i -> drawCircle(Color.White.copy(alpha = .35f + (i % 3) * .12f), (1 + i % 2).dp.toPx(), Offset(size.width * ((i * 37 % 100) / 100f), size.height * ((i * 53 % 70) / 100f))) }
                drawCircle(Color(0xFFE9F2FF).copy(alpha = .75f), 18.dp.toPx(), Offset(size.width * .82f, size.height * .20f))
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            when (code) {
                0, 1 -> {
                    val r = 35.dp.toPx()
                    if (night) {
                        // At night the scene is moon-only. Do not draw the daytime sun
                        // or its rotating rays on top of the night sky.
                        drawCircle(Color(0xFFE9F2FF).copy(alpha = .20f), r * 1.65f * pulse, center)
                        val moonRadius = r * .82f * pulse
                        drawCircle(Color(0xFFF4F7FF).copy(alpha = .90f), moonRadius, center)
                        val phase = (moonAge / 29.53058867).coerceIn(0.0, 1.0)
                        val illumination = kotlin.math.abs(kotlin.math.cos(phase * PI * 2)).toFloat()
                        val waxing = phase < .5
                        val shadowRadius = moonRadius * (0.45f + illumination * .62f)
                        val shadowOffset = moonRadius * (1f - illumination) * if (waxing) -0.72f else 0.72f
                        drawCircle(
                            Color(0xFF07162E).copy(alpha = .88f),
                            shadowRadius,
                            Offset(center.x + shadowOffset, center.y)
                        )
                    } else {
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
                }
                2, 3 -> { // clouds physically drift over the glass
                    repeat(5) { i ->
                        val baseX = size.width * (.12f + i * .2f) + drift * if (i % 2 == 0) 1f else -0.7f
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
                    repeat((14 * intensity.coerceIn(.5f, 2f)).toInt()) { i ->
                        val x = size.width * (i + 1) / 15f + sin(i.toFloat()) * 12f
                        val y = (fall + i * 43f) % (size.height + 40f) - 20f
                        drawLine(Color(0xFF7CC7FF).copy(alpha = .75f), Offset(x.toFloat(), y.toFloat()), Offset(x - 8f, y + 25f), 2.5.dp.toPx())
                        if (y.toFloat() > size.height - 30f) drawCircle(Color(0xFF7CC7FF).copy(alpha = .35f), 7.dp.toPx(), Offset(x, size.height - 8f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }
                in 71..77, in 85..86 -> { // snow flakes fall and sway
                    repeat((16 * intensity.coerceIn(.5f, 2f)).toInt()) { i ->
                        val y = (fall * .55f + i * 37f) % (size.height + 30f) - 15f
                        val x = size.width * (i + 1) / 17f + sin(wave + i.toFloat()) * 18f
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


@Composable
fun FullScreenWeatherBackground(
    code: Int,
    windSpeed: Double = 0.0,
    windDirection: Int = 0,
    intensity: Float = 1f,
    night: Boolean = false,
    moonAge: Double = currentMoonAge(),
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "weatherParallax")
    val parallax by transition.animateFloat(
        initialValue = if (reducedMotion) 0f else -10f,
        targetValue = if (reducedMotion) 0f else 10f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "weatherParallaxOffset"
    )
    Box(modifier.fillMaxSize().graphicsLayer { translationX = parallax; translationY = parallax * .35f }) {
        AnimatedWeatherGlass(
            code = code,
            windSpeed = windSpeed,
            windDirection = windDirection,
            intensity = intensity,
            night = night,
            moonAge = moonAge,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = .48f; scaleX = 1.8f; scaleY = 4.8f }
        )
    }
}

@Composable
fun SevereWeatherPulse(modifier: Modifier = Modifier): Modifier {
    val t = rememberInfiniteTransition(label = "severe")
    val scale by t.animateFloat(.985f, 1.015f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "severeScale")
    return modifier.graphicsLayer { scaleX = scale; scaleY = scale }
}


private fun currentMoonAge(date: java.time.LocalDate = java.time.LocalDate.now()): Double {
    val knownNewMoon = java.time.LocalDate.of(2000, 1, 6)
    val days = java.time.temporal.ChronoUnit.DAYS.between(knownNewMoon, date).toDouble()
    return ((days % 29.53058867) + 29.53058867) % 29.53058867
}
