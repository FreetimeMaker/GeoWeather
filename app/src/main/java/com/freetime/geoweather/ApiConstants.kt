package com.freetime.geoweather

object ApiConstants {
    // Open-Meteo APIs (Fallbacks)
    const val OPEN_METEO_FORECAST = "https://api.open-meteo.com/v1/forecast"
    const val OPEN_METEO_GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
    const val OPEN_METEO_REVERSE_GEOCODING = "https://geocoding-api.open-meteo.com/v1/reverse"
    const val OPEN_METEO_ARCHIVE = "https://archive-api.open-meteo.com/v1/archive"
    const val OPEN_METEO_AIR_QUALITY = "https://air-quality-api.open-meteo.com/v1/air-quality"
    
    // WeatherAPI
    const val WEATHER_API_FORECAST = "https://api.weatherapi.com/v1/forecast.json"
    const val WEATHER_API_CURRENT = "https://api.weatherapi.com/v1/current.json"
    
    // Tomorrow.io
    const val TOMORROW_IO_API = "https://api.tomorrow.io/v4/weather/forecast"
    
    // Visual Crossing
    const val VISUAL_CROSSING_API = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline"
    
    // OpenWeatherMap
    const val OPEN_WEATHER_MAP_FORECAST = "https://api.openweathermap.org/data/2.5/forecast"
    const val OPEN_WEATHER_MAP_CURRENT = "https://api.openweathermap.org/data/2.5/weather"
    
    // QWeather (Moon data and astronomical information)
    const val QWEATHER_MOON = "https://devapi.qweather.com/v7/astronomy/moon"
    const val QWEATHER_SUN = "https://devapi.qweather.com/v7/astronomy/sun"

    fun getAirQualityUrl(lat: Double, lon: Double): String {
        return "$OPEN_METEO_AIR_QUALITY?latitude=$lat&longitude=$lon&hourly=pm10,pm2_5&timezone=auto"
    }
}
