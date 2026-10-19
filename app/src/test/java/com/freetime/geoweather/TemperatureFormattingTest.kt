package com.freetime.geoweather

import com.freetime.geoweather.ui.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TemperatureFormattingTest {
    @Test fun absoluteTemperatures() {
        assertEquals("32°F", formatTemp(0.0, "fahrenheit"))
        assertEquals("212°F", formatTemp(100.0, "fahrenheit"))
        assertEquals("-40°F", formatTemp(-40.0, "fahrenheit"))
        assertEquals("20°C", formatTemp(20.9, "celsius"))
        assertEquals("20°C", formatTemp(20.9, "unknown"))
    }

    @Test fun differencesHaveNoOffset() {
        assertEquals(0.0, temperatureDeltaValue(0.0, "fahrenheit"))
        assertEquals(9.0, temperatureDeltaValue(5.0, "fahrenheit"))
        assertEquals(-9.0, temperatureDeltaValue(-5.0, "fahrenheit"))
        assertEquals(5.0, temperatureDeltaValue(5.0, "celsius"))
    }

    @Test fun decimalFormattingUsesCurrentLocale() {
        val locale = java.util.Locale.getDefault()
        assertEquals(String.format(locale, "%.1f°F", 68.9), formatTempDecimal(20.5, "fahrenheit"))
        assertEquals(String.format(locale, "%+.1f°F", -9.0), formatTempDelta(-5.0, "fahrenheit", signed = true))
        assertEquals(String.format(locale, "%.1f°F", 9.0), formatTempDelta(5.0, "fahrenheit"))
        assertEquals(String.format(locale, "%.1f°C", 2.5), formatTempDelta(2.5, "celsius"))
    }
}
