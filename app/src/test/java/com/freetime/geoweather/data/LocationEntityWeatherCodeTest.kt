package com.freetime.geoweather.data

import kotlin.test.Test
import kotlin.test.assertEquals

class LocationEntityWeatherCodeTest {
    @Test
    fun currentOpenMeteoFieldsAreParsed() {
        val location = LocationEntity(
            name = "Test",
            latitude = 0.0,
            longitude = 0.0,
            weatherData = """{"current":{"temperature_2m":12.5,"weather_code":63,"relative_humidity_2m":81,"apparent_temperature":10.0,"pressure_msl":1012.4,"wind_speed_10m":14.2,"wind_direction_10m":220,"wind_gusts_10m":27.0,"is_day":1}}"""
        )
        assertEquals(12.5, location.currentTemp)
        assertEquals(63, location.currentWeatherCode)
        assertEquals(81, location.currentHumidity)
        assertEquals(10.0, location.currentFeelsLike)
        assertEquals(1012.4, location.currentPressure)
        assertEquals(14.2, location.currentWindSpeed)
        assertEquals(220, location.currentWindDirection)
        assertEquals(27.0, location.currentWindGusts)
        assertEquals(true, location.isDay)
    }

    @Test
    fun legacyOpenMeteoCurrentWeatherStillWorks() {
        val location = LocationEntity(
            name = "Legacy",
            latitude = 0.0,
            longitude = 0.0,
            weatherData = """{"current_weather":{"temperature":7.0,"weathercode":2,"is_day":0}}"""
        )
        assertEquals(7.0, location.currentTemp)
        assertEquals(2, location.currentWeatherCode)
        assertEquals(false, location.isDay)
    }
}
