package com.freetime.geoweather.network

import com.freetime.geoweather.network.models.GeocodingResponse
import com.freetime.geoweather.network.models.WeatherResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class WeatherApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse {
        return client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", lat)
            parameter("longitude", lon)
            parameter("current_weather", true)
            parameter("hourly", "temperature_2m,weathercode,relativehumidity_2m,apparent_temperature")
            parameter("daily", "weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,windspeed_10m_max")
            parameter("timezone", "auto")
        }.body()
    }

    suspend fun getHistoricalWeather(lat: Double, lon: Double, startDate: String, endDate: String): WeatherResponse {
        return client.get("https://archive-api.open-meteo.com/v1/archive") {
            parameter("latitude", lat)
            parameter("longitude", lon)
            parameter("start_date", startDate)
            parameter("end_date", endDate)
            parameter("daily", "weathercode,temperature_2m_max,temperature_2m_min")
            parameter("timezone", "auto")
        }.body()
    }

    suspend fun searchLocations(query: String, language: String): GeocodingResponse {
        return client.get("https://geocoding-api.open-meteo.com/v1/search") {
            parameter("name", query)
            parameter("count", 20)
            parameter("language", language)
            parameter("format", "json")
        }.body()
    }

    suspend fun reverseGeocode(lat: Double, lon: Double, language: String): GeocodingResponse {
        // The API returns a single object or results array depending on the endpoint.
        // For simplicity, let's just handle the search for now or adjust models.
        return client.get("https://geocoding-api.open-meteo.com/v1/reverse") {
            parameter("latitude", lat)
            parameter("longitude", lon)
            parameter("language", language)
            parameter("format", "json")
        }.body()
    }
}
