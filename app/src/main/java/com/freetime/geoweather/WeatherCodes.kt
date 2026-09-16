package com.freetime.geoweather

import android.content.Context

object WeatherCodes {
    fun getDescription(code: Int, context: Context? = null): String {
        if (context == null) {
            return when (code) {
                0 -> "Clear sky"
                1, 2 -> "Mainly clear"
                3 -> "Overcast"
                45, 48 -> "Fog"
                51, 53, 55 -> "Drizzle"
                56, 57 -> "Freezing drizzle"
                61, 63, 65 -> "Rain"
                66, 67 -> "Freezing rain"
                71, 73, 75 -> "Snowfall"
                77 -> "Snow grains"
                80, 81, 82 -> "Rain showers"
                85, 86 -> "Snow showers"
                95 -> "Thunderstorm"
                96, 99 -> "Thunderstorm with hail"
                else -> "Unknown"
            }
        }

        return when (code) {
            0 -> context.getString(R.string.wc_clear)
            1, 2 -> context.getString(R.string.wc_mainly_clear)
            3 -> context.getString(R.string.wc_overcast)
            45, 48 -> context.getString(R.string.wc_fog)
            51, 53, 55 -> context.getString(R.string.wc_drizzle)
            56, 57 -> context.getString(R.string.wc_freezing_drizzle)
            61, 63, 65 -> context.getString(R.string.wc_rain)
            66, 67 -> context.getString(R.string.wc_freezing_rain)
            71, 73, 75 -> context.getString(R.string.wc_snowfall)
            77 -> context.getString(R.string.wc_snow_grains)
            80, 81, 82 -> context.getString(R.string.wc_rain_showers)
            85, 86 -> context.getString(R.string.wc_snow_showers)
            95 -> context.getString(R.string.wc_thunderstorm)
            96, 99 -> context.getString(R.string.wc_thunderstorm_hail)
            else -> context.getString(R.string.wc_unknown)
        }
    }
}
