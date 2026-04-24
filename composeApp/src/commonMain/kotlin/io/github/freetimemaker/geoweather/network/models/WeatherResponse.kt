package io.github.freetimemaker.geoweather.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    @SerialName("current_weather")
    val currentWeather: CurrentWeather? = null,
    val hourly: HourlyData? = null,
    val daily: DailyData? = null
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    val time: String
)

@Serializable
data class HourlyData(
    val time: List<String>,
    @SerialName("temperature_2m")
    val temperature2m: List<Double>,
    val weathercode: List<Int>,
    @SerialName("relativehumidity_2m")
    val relativeHumidity2m: List<Int>? = null,
    @SerialName("apparent_temperature")
    val apparentTemperature: List<Double>? = null
)

@Serializable
data class DailyData(
    val time: List<String>,
    @SerialName("weathercode")
    val weathercode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max")
    val temperature2mMax: List<Double>,
    @SerialName("temperature_2m_min")
    val temperature2mMin: List<Double>,
    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),
    @SerialName("precipitation_probability_max")
    val precipitationProbabilityMax: List<Int>? = null,
    @SerialName("windspeed_10m_max")
    val windspeed10mMax: List<Double>? = null
)
