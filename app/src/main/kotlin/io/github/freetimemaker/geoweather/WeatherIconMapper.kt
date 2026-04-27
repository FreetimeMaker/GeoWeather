package io.github.freetimemaker.geoweather


object WeatherIconMapper {
    fun getWeatherIcon(code: Int): Int {
        // Simplified for now, ignoring day/night for a moment to get things running
        return when (code) {
            0 -> R.drawable.google_clear_day
            1 -> R.drawable.google_mostly_clear_day
            2 -> R.drawable.google_partly_cloudy_day
            3 -> R.drawable.google_cloudy
            45, 48 -> R.drawable.google_fog
            51, 53, 55 -> R.drawable.google_drizzle
            61, 63, 65 -> R.drawable.google_rain_with_sunny_light
            71, 73, 75 -> R.drawable.google_snow_with_sunny_light
            else -> R.drawable.google_cloudy_with_sunny_light
        }
    }
}
