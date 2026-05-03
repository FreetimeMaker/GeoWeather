package com.freetime.geoweather

import android.content.Context

object WeatherCodes {
    fun getDescription(code: Int, context: Context? = null, provider: String = "open_meteo"): String {
        if (provider.lowercase() == "weatherapi" && context != null) {
            return getWeatherApiDescription(code, context)
        }
        
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

    /**
     * Map WeatherAPI condition codes to Open-Meteo style descriptions
     */
    fun getWeatherApiDescription(code: Int, context: Context): String {
        return when (code) {
            1000 -> context.getString(R.string.wc_clear)
            1003, 1006 -> context.getString(R.string.wc_mainly_clear)
            1009 -> context.getString(R.string.wc_overcast)
            1030, 1135, 1147 -> context.getString(R.string.wc_fog)
            1063, 1180, 1183, 1186, 1189, 1192, 1195 -> context.getString(R.string.wc_rain)
            1066, 1210, 1213, 1216, 1219, 1222, 1225 -> context.getString(R.string.wc_snowfall)
            1069, 1204, 1207 -> context.getString(R.string.wc_freezing_rain)
            1072, 1150, 1153, 1168, 1171 -> context.getString(R.string.wc_drizzle)
            1087, 1273, 1276, 1279, 1282 -> context.getString(R.string.wc_thunderstorm)
            1114, 1117 -> context.getString(R.string.wc_snowfall)
            1237, 1261, 1264 -> context.getString(R.string.wc_snow_grains)
            1240, 1243, 1246 -> context.getString(R.string.wc_rain_showers)
            1249, 1252 -> context.getString(R.string.wc_rain_showers)
            1255, 1258 -> context.getString(R.string.wc_snow_showers)
            else -> context.getString(R.string.wc_unknown)
        }
    }
}
