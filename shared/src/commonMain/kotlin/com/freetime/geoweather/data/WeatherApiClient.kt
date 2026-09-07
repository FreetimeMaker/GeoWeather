package com.freetime.geoweather.data

import com.freetime.geoweather.domain.City
import com.freetime.geoweather.ApiConstants
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class WeatherApiClient(
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }
) {
    suspend fun get(url: String, token: String? = null): String {
        val response = client.get(url) {
            header("User-Agent", "GeoWeatherApp")
            token?.let {
                header("Authorization", "Bearer $it")
            }
        }
        
        if (response.status.value !in 200..299) {
            throw Exception("HTTP Error ${response.status.value}: ${response.bodyAsText()}")
        }
        
        return response.bodyAsText()
    }

    suspend fun searchCity(query: String): List<City> {
        val url = ApiConstants.OPEN_METEO_GEOCODING + "?name=$query&count=10&language=en&format=json"
        val response = client.get(url)
        if (response.status.value !in 200..299) return emptyList()
        
        val body = response.bodyAsText()
        val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
        val results = json["results"]?.jsonArray ?: return emptyList()
        
        return results.map {
            val obj = it.jsonObject
            City(
                name = obj["name"]?.jsonPrimitive?.content ?: "Unknown",
                latitude = obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                longitude = obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                country = obj["country"]?.jsonPrimitive?.content,
                admin1 = obj["admin1"]?.jsonPrimitive?.content
            )
        }
    }
}
