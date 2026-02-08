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

    fun getWeatherIcon(code: Int): Int {
        val isDay = isDaytime()

        return when (code) {
            0 -> if (isDay) R.drawable.google_clear_day else R.drawable.google_clear_night
            1 -> if (isDay) R.drawable.google_mostly_clear_day else R.drawable.google_mostly_clear_night
            2 -> if (isDay) R.drawable.google_partly_cloudy_day else R.drawable.google_partly_cloudy_night
            3 -> R.drawable.google_cloudy
            45, 48 -> R.drawable.google_fog
            51, 53, 55 -> R.drawable.google_drizzle
            61, 63, 65 -> if (isDay) R.drawable.google_rain_with_sunny_light else R.drawable.google_rain_with_sunny_dark
            71, 73, 75 -> if (isDay) R.drawable.google_snow_with_sunny_light else R.drawable.google_snow_with_sunny_dark
            else -> if (isDay) R.drawable.google_cloudy_with_sunny_light else R.drawable.google_cloudy_with_sunny_dark
        }
    }
}
