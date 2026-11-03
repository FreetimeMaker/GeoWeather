package com.freetime.geoweather

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertTrue

class WidgetPaletteTest {
    @Test
    fun widgetTextHasReadableContrastForEveryWeatherCodeAndTimeOfDay() {
        for (isDay in listOf(true, false)) {
            for (code in 0..99) {
                val palette = widgetPalette(code, isDay)
                for (foreground in listOf(palette.primary, palette.secondary, palette.muted)) {
                    val ratio = contrastRatio(foreground, palette.background)
                    assertTrue(ratio >= 4.5f, "Contrast $ratio for weather code $code, isDay=$isDay")
                }
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (maxOf(firstLuminance, secondLuminance) + 0.05f) /
            (minOf(firstLuminance, secondLuminance) + 0.05f)
    }
}
