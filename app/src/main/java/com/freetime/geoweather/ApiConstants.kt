package com.freetime.geoweather

object ApiConstants {
    const val BASE_URL = "https://geo-weather-api.vercel.app"
    
    // Open-Meteo APIs
    const val OPEN_METEO_FORECAST = "https://api.open-meteo.com/v1/forecast"
    const val OPEN_METEO_GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
    const val OPEN_METEO_REVERSE_GEOCODING = "https://geocoding-api.open-meteo.com/v1/reverse"
    const val OPEN_METEO_ARCHIVE = "https://archive-api.open-meteo.com/v1/archive"
    const val OPEN_METEO_AIR_QUALITY = "https://air-quality-api.open-meteo.com/v1/air-quality"
    
    // WeatherAPI
    const val WEATHER_API_FORECAST = "https://api.weatherapi.com/v1/forecast.json"
    const val WEATHER_API_CURRENT = "https://api.weatherapi.com/v1/current.json"
    
    // QWeather
    const val QWEATHER_MOON = "https://devapi.qweather.com/v7/astronomy/moon"
}
