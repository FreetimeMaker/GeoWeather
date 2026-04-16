package com.freetime.geoweather

import com.freetime.geoweather.data.LocationEntity
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class IpApiResponse(val lat: Double, val lon: Double, val city: String)

class DesktopLocationService : LocationService {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    override suspend fun getCurrentLocation(): LocationEntity? {
        return try {
            val response: IpApiResponse = client.get("http://ip-api.com/json").body()
            LocationEntity(name = response.city, latitude = response.lat, longitude = response.lon)
        } catch (e: Exception) {
            null
        }
    }
}

actual fun createLocationService(): LocationService = DesktopLocationService()
