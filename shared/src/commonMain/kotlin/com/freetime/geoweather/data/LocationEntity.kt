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

    val feelsLike: Double? get() = currentValue("apparent_temperature")
    val humidity: Double? get() = currentValue("relative_humidity_2m")
    val precipitation: Double? get() = currentValue("precipitation")
    val windSpeed: Double? get() = currentValue("wind_speed_10m")
    val windDirection: Double? get() = currentValue("wind_direction_10m")
    val pressure: Double? get() = currentValue("pressure_msl")
    val visibility: Double? get() = currentValue("visibility")?.div(1000.0)
    val cloudCover: Double? get() = currentValue("cloud_cover")
    val uvIndex: Double? get() = currentValue("uv_index")

    private fun currentValue(key: String): Double? {
        return try {
            weatherData?.let { data ->
                val json = Json.parseToJsonElement(data).jsonObject
                json["current"]?.jsonObject?.get(key)?.jsonPrimitive?.doubleOrNull
            }
        } catch (e: Exception) { null }
    }

    val dailyForecast: List<ForecastDay> get() {
        return try {
            weatherData?.let { data ->
                val json = Json.parseToJsonElement(data).jsonObject
                val daily = json["daily"]?.jsonObject ?: return emptyList()
                val times = daily["time"]?.jsonArray ?: return emptyList()
                val codes = daily["weathercode"]?.jsonArray ?: daily["weather_code"]?.jsonArray ?: return emptyList()
                val maxTemps = daily["temperature_2m_max"]?.jsonArray ?: return emptyList()
                val minTemps = daily["temperature_2m_min"]?.jsonArray ?: return emptyList()

                times.indices.map { i ->
                    ForecastDay(
                        date = times[i].jsonPrimitive.content,
                        weatherCode = codes[i].jsonPrimitive.intOrNull ?: 0,
                        maxTemp = maxTemps[i].jsonPrimitive.doubleOrNull ?: 0.0,
                        minTemp = minTemps[i].jsonPrimitive.doubleOrNull ?: 0.0
                    )
                }
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    val hourlyForecast: List<ForecastHour> get() {
        return try {
            weatherData?.let { data ->
                val hourly = Json.parseToJsonElement(data).jsonObject["hourly"]?.jsonObject
                    ?: return emptyList()
                val times = hourly["time"]?.jsonArray ?: return emptyList()
                val temperatures = hourly["temperature_2m"]?.jsonArray ?: return emptyList()
                val codes = hourly["weathercode"]?.jsonArray ?: return emptyList()
                val windSpeeds = hourly["windspeed_10m"]?.jsonArray
                val precipitationProbabilities = hourly["precipitation_probability"]?.jsonArray
                val uvIndexes = hourly["uv_index"]?.jsonArray
                val visibilities = hourly["visibility"]?.jsonArray

                times.indices.map { i ->
                    ForecastHour(
                        time = times[i].jsonPrimitive.content,
                        temperature = temperatures[i].jsonPrimitive.doubleOrNull ?: 0.0,
                        weatherCode = codes[i].jsonPrimitive.intOrNull ?: 0,
                        windSpeed = windSpeeds?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                        precipitationProbability = precipitationProbabilities?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                        uvIndex = uvIndexes?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                        visibility = visibilities?.getOrNull(i)?.jsonPrimitive?.doubleOrNull?.div(1000.0)
                    )
                }
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}

data class ForecastDay(
    val date: String,
    val weatherCode: Int,
    val maxTemp: Double,
    val minTemp: Double
)

data class ForecastHour(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double?,
    val precipitationProbability: Double?,
    val uvIndex: Double?,
    val visibility: Double?
)
