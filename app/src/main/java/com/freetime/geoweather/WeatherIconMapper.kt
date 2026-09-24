package com.freetime.geoweather

import androidx.annotation.DrawableRes
import com.freetime.geoweather.R as Res
import kotlinx.datetime.*

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
        return now > sunrise && now < sunset
    }

    @DrawableRes
    fun getWeatherIcon(code: Int, theme: String = "google"): Int =
        getWeatherIcon(code, isDaytime(), theme)

    @DrawableRes
    fun getWeatherIcon(code: Int, isDay: Boolean, theme: String = "google"): Int {
        return when (code) {
            0 -> if (isDay) Res.drawable.google_clear_day else Res.drawable.google_clear_night
            1 -> if (isDay) Res.drawable.google_mostly_clear_day else Res.drawable.google_mostly_clear_night
            2 -> if (isDay) Res.drawable.google_partly_cloudy_day else Res.drawable.google_partly_cloudy_night
            3 -> Res.drawable.google_cloudy
            45, 48 -> Res.drawable.google_fog
            51, 53, 55 -> Res.drawable.google_drizzle
            56, 57 -> Res.drawable.google_drizzle
            61, 63, 65, 66, 67, 80, 81, 82 ->
                if (isDay) Res.drawable.google_rain_with_sunny_light else Res.drawable.google_rain_with_sunny_dark
            71, 73, 75, 77, 85, 86 ->
                if (isDay) Res.drawable.google_snow_with_sunny_light else Res.drawable.google_snow_with_sunny_dark
            95, 96, 99 ->
                if (isDay) Res.drawable.google_rain_with_sunny_light else Res.drawable.google_rain_with_sunny_dark
            else -> if (isDay) Res.drawable.google_cloudy_with_sunny_light else Res.drawable.google_cloudy_with_sunny_dark
        }
    }
}
