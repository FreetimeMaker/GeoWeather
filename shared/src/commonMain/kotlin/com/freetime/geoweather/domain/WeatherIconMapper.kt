package com.freetime.geoweather.domain

import kotlinx.datetime.*
import kotlin.time.Clock

object WeatherIconMapper {
    private var sunriseTime: LocalDateTime? = null
    private var sunsetTime: LocalDateTime? = null

    fun setSunTimes(sunrise: String, sunset: String) {
        try {
            sunriseTime = Instant.parse(sunrise).toLocalDateTime(TimeZone.currentSystemDefault())
            sunsetTime = Instant.parse(sunset).toLocalDateTime(TimeZone.currentSystemDefault())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isDaytime(): Boolean {
        val sunrise = sunriseTime ?: return true
        val sunset = sunsetTime ?: return true
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // Simplified comparison for demo
        return now > sunrise && now < sunset
    }

    fun getWeatherIconName(code: Int, isDay: Boolean = isDaytime()): String {
        return when (code) {
            0 -> if (isDay) "google_clear_day" else "google_clear_night"
            1 -> if (isDay) "google_mostly_clear_day" else "google_mostly_clear_night"
            2 -> if (isDay) "google_partly_cloudy_day" else "google_partly_cloudy_night"
            3 -> "google_cloudy"
            45, 48 -> "google_fog"
            51, 53, 55 -> "google_drizzle"
            61, 63, 65 -> if (isDay) "google_rain_with_sunny_light" else "google_rain_with_sunny_dark"
            71, 73, 75 -> if (isDay) "google_snow_with_sunny_light" else "google_snow_with_sunny_dark"
            else -> if (isDay) "google_cloudy_with_sunny_light" else "google_cloudy_with_sunny_dark"
        }
    }

    fun getWeatherEmoji(code: Int, isDay: Boolean = isDaytime()): String {
        return when (code) {
            0 -> if (isDay) "☀" else "☾"
            1, 2 -> if (isDay) "🌤" else "☾"
            3 -> "☁"
            45, 48 -> "🌫"
            51, 53, 55, 56, 57 -> "🌦"
            61, 63, 65, 80, 81, 82 -> "🌧"
            66, 67 -> "🌧"
            71, 73, 75, 77, 85, 86 -> "🌨"
            95, 96, 99 -> "⛈"
            else -> "☁"
        }
    }
}
