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
                        obj.getJSONObject("current").getJSONObject("condition").getInt("code")
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
                        obj.getJSONObject("current").getInt("is_day") == 1
                    } else true
                } ?: true
            } catch (e: Exception) {
                true
            }
        }

    val provider: String
        get() {
            return if (weatherData?.contains("\"current\":") == true) "weatherapi" else "open_meteo"
        }

}
