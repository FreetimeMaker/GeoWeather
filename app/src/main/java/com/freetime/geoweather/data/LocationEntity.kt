package com.freetime.geoweather.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["latitude", "longitude"], unique = true)
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    val currentTemp: Double?
        get() {
            return try {
                weatherData?.let { data ->
                    val obj = org.json.JSONObject(data)
                    if (obj.has("current_weather")) {
                        obj.getJSONObject("current_weather").getDouble("temperature")
                    } else if (obj.has("current")) {
                        obj.getJSONObject("current").getDouble("temp_c")
                    } else if (obj.has("temperature_2m")) {
                        // For some open-meteo variants
                        obj.getDouble("temperature_2m")
                    } else if (obj.has("timelines")) {
                        // Tomorrow.io logic
                        obj.getJSONObject("timelines").getJSONArray("daily").getJSONObject(0).getJSONObject("values").getDouble("temperatureAvg")
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }
    
    val currentWeatherCode: Int?
        get() {
            return try {
                weatherData?.let { data ->
                    val obj = org.json.JSONObject(data)
                    if (obj.has("current_weather")) {
                        obj.getJSONObject("current_weather").getInt("weathercode")
                    } else if (obj.has("current")) {
                        val current = obj.getJSONObject("current")
                        if (current.has("condition")) current.getJSONObject("condition").getInt("code")
                        else current.optInt("weathercode", 0)
                    } else if (obj.has("weathercode")) {
                        obj.getInt("weathercode")
                    } else if (obj.has("timelines")) {
                        obj.getJSONObject("timelines").getJSONArray("daily").getJSONObject(0).getJSONObject("values").getInt("weatherCodeMax")
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }

    val isDay: Boolean
        get() {
            return try {
                weatherData?.let { data ->
                    val obj = org.json.JSONObject(data)
                    if (obj.has("current")) {
                        obj.getJSONObject("current").optInt("is_day", 1) == 1
                    } else if (obj.has("current_weather")) {
                        obj.getJSONObject("current_weather").optInt("is_day", 1) == 1
                    } else true
                } ?: true
            } catch (e: Exception) {
                true
            }
        }

    val provider: String
        get() {
            val data = weatherData ?: return "unknown"
            return when {
                data.contains("\"current_weather\":") -> "open_meteo"
                data.contains("\"current\":") -> "weatherapi"
                data.contains("\"timelines\":") -> "tomorrow.io"
                data.contains("\"currentConditions\":") -> "visualcrossing"
                else -> "open_meteo"
            }
        }

}
