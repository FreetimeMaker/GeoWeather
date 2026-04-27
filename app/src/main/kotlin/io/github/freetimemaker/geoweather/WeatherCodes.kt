package io.github.freetimemaker.geoweather


object WeatherCodes {
    fun getDescriptionResource(code: Int): Int {
        return when (code) {
            0 -> R.string.wc_clear
            1, 2 -> R.string.wc_mainly_clear
            3 -> R.string.wc_overcast
            45, 48 -> R.string.wc_fog
            51, 53, 55 -> R.string.wc_drizzle
            56, 57 -> R.string.wc_freezing_drizzle
            61, 63, 65 -> R.string.wc_rain
            66, 67 -> R.string.wc_freezing_rain
            71, 73, 75 -> R.string.wc_snowfall
            77 -> R.string.wc_snow_grains
            80, 81, 82 -> R.string.wc_rain_showers
            85, 86 -> R.string.wc_snow_showers
            95 -> R.string.wc_thunderstorm
            96, 99 -> R.string.wc_thunderstorm_hail
            else -> R.string.wc_unknown
        }
    }
}
