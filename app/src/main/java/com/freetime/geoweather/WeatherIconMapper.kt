package com.freetime.geoweather

import android.text.TextUtils
import java.time.ZoneId
import java.time.ZonedDateTime

object WeatherIconMapper {
    private var sunriseTime: ZonedDateTime? = null
    private var sunsetTime: ZonedDateTime? = null

    fun setSunTimes(sunriseIso: String?, sunsetIso: String?) {
        try {
            if (!TextUtils.isEmpty(sunriseIso)) {
                sunriseTime = ZonedDateTime.parse(sunriseIso + ":00Z")
                    .withZoneSameInstant(ZoneId.systemDefault())
            }
            if (!TextUtils.isEmpty(sunsetIso)) {
                sunsetTime = ZonedDateTime.parse(sunsetIso + ":00Z")
                    .withZoneSameInstant(ZoneId.systemDefault())
            }
        } catch (e: Exception) {
            sunriseTime = null
            sunsetTime = null
        }
    }

    private fun isDaytime(): Boolean {
        if (sunriseTime == null || sunsetTime == null) return true
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        return now.isAfter(sunriseTime) && now.isBefore(sunsetTime)
    }

    fun getIcon(code: Int, provider: String = "open_meteo", isDay: Boolean = true): Int {
        return if (provider.lowercase() == "weatherapi") {
            getWeatherApiIcon(code, isDay)
        } else {
            getWeatherIcon(code, isDay)
        }
    }

    fun getWeatherIcon(code: Int, isDay: Boolean = isDaytime()): Int {
        return when (code) {
            0 -> if (isDay) R.drawable.google_clear_day else R.drawable.google_clear_night
            1 -> if (isDay) R.drawable.google_mostly_clear_day else R.drawable.google_mostly_clear_night
            2 -> if (isDay) R.drawable.google_partly_cloudy_day else R.drawable.google_partly_cloudy_night
            3 -> R.drawable.google_cloudy
            45, 48 -> R.drawable.google_fog
            51, 53, 55 -> R.drawable.google_drizzle
            56, 57 -> R.drawable.icy
            61, 63, 65 -> if (isDay) R.drawable.google_rain_with_sunny_light else R.drawable.google_rain_with_sunny_dark
            66, 67 -> R.drawable.icy
            71, 73, 75 -> if (isDay) R.drawable.google_snow_with_sunny_light else R.drawable.google_snow_with_sunny_dark
            77 -> R.drawable.flurries
            80, 81, 82 -> if (isDay) R.drawable.scattered_showers_day else R.drawable.scattered_showers_night
            85, 86 -> if (isDay) R.drawable.scattered_snow_showers_day else R.drawable.scattered_snow_showers_night
            95 -> R.drawable.isolated_scattered_thunderstorms_day
            96, 99 -> R.drawable.isolated_scattered_thunderstorms_day
            else -> if (isDay) R.drawable.google_cloudy_with_sunny_light else R.drawable.google_cloudy_with_sunny_dark
        }
    }

    /**
     * Map WeatherAPI condition codes to app icons
     */
    fun getWeatherApiIcon(code: Int, isDay: Boolean = true): Int {
        return when (code) {
            1000 -> if (isDay) R.drawable.google_clear_day else R.drawable.google_clear_night
            1003 -> if (isDay) R.drawable.google_mostly_clear_day else R.drawable.google_mostly_clear_night
            1006 -> if (isDay) R.drawable.google_partly_cloudy_day else R.drawable.google_partly_cloudy_night
            1009 -> R.drawable.google_cloudy
            1030, 1135, 1147 -> R.drawable.google_fog
            1063, 1180, 1183, 1186, 1189, 1192, 1195 -> if (isDay) R.drawable.google_rain_with_sunny_light else R.drawable.google_rain_with_sunny_dark
            1066, 1210, 1213, 1216, 1219, 1222, 1225 -> if (isDay) R.drawable.google_snow_with_sunny_light else R.drawable.google_snow_with_sunny_dark
            1069, 1204, 1207, 1072, 1150, 1153, 1168, 1171 -> R.drawable.google_drizzle
            1087, 1273, 1276, 1279, 1282 -> R.drawable.isolated_scattered_thunderstorms_day
            1114, 1117 -> R.drawable.blowing_snow
            1237, 1261, 1264 -> R.drawable.flurries
            1240, 1243, 1246 -> if (isDay) R.drawable.scattered_showers_day else R.drawable.scattered_showers_night
            1249, 1252 -> if (isDay) R.drawable.scattered_showers_day else R.drawable.scattered_showers_night
            1255, 1258 -> if (isDay) R.drawable.scattered_snow_showers_day else R.drawable.scattered_snow_showers_night
            else -> if (isDay) R.drawable.google_cloudy_with_sunny_light else R.drawable.google_cloudy_with_sunny_dark
        }
    }
}
