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
        return try {
            // Coordinate input like "40.71, -74.00" -> reverse geocoding
            val coordinateMatch = coordinateRegex.matchEntire(query.trim())
            if (coordinateMatch != null) {
                val lat = coordinateMatch.groupValues[1].toDouble()
                val lon = coordinateMatch.groupValues[2].toDouble()
                return reverseGeocode(lat, lon)
            }

            val body = client.get(ApiConstants.OPEN_METEO_GEOCODING) {
                parameter("name", query)
                parameter("count", 20)
                parameter("language", "en")
                parameter("format", "json")
            }.bodyAsText()
            parseGeocodingResults(body)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): List<City> {
        return try {
            val body = client.get(ApiConstants.OPEN_METEO_REVERSE_GEOCODING) {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("language", "en")
                parameter("format", "json")
            }.bodyAsText()
            parseGeocodingResults(body)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseGeocodingResults(body: String): List<City> {
        val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(body).jsonObject
        // /v1/search returns {"results": [...]}, /v1/reverse a single object
        val results = json["results"]?.jsonArray
            ?: if (json.containsKey("name")) buildJsonArray { add(json) } else return emptyList()

        return results.map {
            val obj = it.jsonObject
            val name = obj["name"]?.jsonPrimitive?.content ?: "Unknown"
            val admin1 = obj["admin1"]?.jsonPrimitive?.content
            val country = obj["country"]?.jsonPrimitive?.content
            val displayName = buildString {
                append(name)
                if (admin1 != null) append(", $admin1")
                if (country != null) append(", $country")
            }
            City(
                name = displayName,
                latitude = obj["latitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                longitude = obj["longitude"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                country = country,
                admin1 = admin1
            )
        }
    }

    companion object {
        private val coordinateRegex = Regex("""^(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)$""")
    }
}
