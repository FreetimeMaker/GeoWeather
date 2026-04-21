package com.freetime.geoweather

import geoweather.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

object WeatherIconMapper {
    fun getWeatherIcon(code: Int): DrawableResource {
        // Simplified for now, ignoring day/night for a moment to get things running
        return when (code) {
            0 -> Res.drawable.google_clear_day
            1 -> Res.drawable.google_mostly_clear_day
            2 -> Res.drawable.google_partly_cloudy_day
            3 -> Res.drawable.google_cloudy
            45, 48 -> Res.drawable.google_fog
            51, 53, 55 -> Res.drawable.google_drizzle
            61, 63, 65 -> Res.drawable.google_rain_with_sunny_light
            71, 73, 75 -> Res.drawable.google_snow_with_sunny_light
            else -> Res.drawable.google_cloudy_with_sunny_light
        }
    }
}
