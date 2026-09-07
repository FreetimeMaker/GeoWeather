package com.freetime.geoweather.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["latitude", "longitude"], unique = true)
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val weatherData: String? = null,
    val lastUpdated: Long = 0,
    val notificationsEnabled: Boolean = false,
    val notificationTime: String = "08:00",
    val changeAlertsEnabled: Boolean = false,
    val changeAlertInterval: String = "3",
    val selected: Boolean = false,
    val isDefault: Boolean = false
) {
    val currentTemp: Double? get() {
        return try {
            weatherData?.let { data ->
                val json = Json.parseToJsonElement(data).jsonObject
                when {
                    "current_weather" in json -> json["current_weather"]?.jsonObject?.get("temperature")?.jsonPrimitive?.doubleOrNull
                    "current" in json -> json["current"]?.jsonObject?.get("temp_c")?.jsonPrimitive?.doubleOrNull
                    "timelines" in json -> {
                        val timelines = json["timelines"]?.jsonObject
                        if (timelines != null && "minutely" in timelines) {
                            timelines["minutely"]?.jsonArray?.get(0)?.jsonObject?.get("values")?.jsonObject?.get("temperature")?.jsonPrimitive?.doubleOrNull
                        } else {
                            timelines?.get("daily")?.jsonArray?.get(0)?.jsonObject?.get("values")?.jsonObject?.get("temperatureAvg")?.jsonPrimitive?.doubleOrNull
                        }
                    }
                    "currentConditions" in json -> json["currentConditions"]?.jsonObject?.get("temp")?.jsonPrimitive?.doubleOrNull
                    "temperature_2m" in json -> json["temperature_2m"]?.jsonPrimitive?.doubleOrNull
                    else -> null
                }
            }
        } catch (e: Exception) { null }
    }

    val currentWeatherCode: Int? get() {
        return try {
            weatherData?.let { data ->
                val json = Json.parseToJsonElement(data).jsonObject
                when {
                    "current_weather" in json -> {
                        val cw = json["current_weather"]?.jsonObject ?: return null
                        cw["weather_code"]?.jsonPrimitive?.intOrNull ?: cw["weathercode"]?.jsonPrimitive?.intOrNull
                    }
                    "current" in json -> {
                        val current = json["current"]?.jsonObject ?: return null
                        if ("condition" in current) current["condition"]?.jsonObject?.get("code")?.jsonPrimitive?.intOrNull
                        else current["weather_code"]?.jsonPrimitive?.intOrNull ?: current["weathercode"]?.jsonPrimitive?.intOrNull
                    }
                    "timelines" in json -> json["timelines"]?.jsonObject?.get("daily")?.jsonArray?.get(0)?.jsonObject?.get("values")?.jsonObject?.get("weatherCodeMax")?.jsonPrimitive?.intOrNull
                    "currentConditions" in json -> 0 // Visual crossing mapping needed
                    "weather_code" in json -> json["weather_code"]?.jsonPrimitive?.intOrNull
                    "weathercode" in json -> json["weathercode"]?.jsonPrimitive?.intOrNull
                    else -> null
                }
            }
        } catch (e: Exception) { null }
    }

    val isDay: Boolean get() {
        return try {
            weatherData?.let { data ->
                val json = Json.parseToJsonElement(data).jsonObject
                if ("current" in json) {
                    json["current"]?.jsonObject?.get("is_day")?.jsonPrimitive?.intOrNull == 1
                } else if ("current_weather" in json) {
                    json["current_weather"]?.jsonObject?.get("is_day")?.jsonPrimitive?.intOrNull == 1
                } else true
            } ?: true
        } catch (e: Exception) { true }
    }

    val provider: String get() {
        val data = weatherData ?: return "unknown"
        return when {
            "\"current_weather\":" in data -> "open_meteo"
            "\"current\":" in data -> "weatherapi"
            "\"timelines\":" in data -> "tomorrow.io"
            "\"currentConditions\":" in data -> "visualcrossing"
            else -> "open_meteo"
        }
    }
}
