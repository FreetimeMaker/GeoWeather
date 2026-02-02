package com.freetime.geoweather.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val weatherData: String? = null,
    val lastUpdated: Long = 0
) {
    val currentTemp: String?
        get() {
            return try {
                weatherData?.let { data ->
                    val obj = org.json.JSONObject(data)
                    val current = obj.getJSONObject("current_weather")
                    current.getDouble("temperature").toString()
                }
            } catch (e: Exception) {
                null
            }
        }
    
    val currentWind: String?
        get() {
            return try {
                weatherData?.let { data ->
                    val obj = org.json.JSONObject(data)
                    val current = obj.getJSONObject("current_weather")
                    current.getDouble("windspeed").toString()
                }
            } catch (e: Exception) {
                null
            }
        }
}
