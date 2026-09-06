package com.freetime.geoweather.domain

import com.russhwolf.settings.Settings

object UnitSettings {
    private val settings = Settings()

    var temperature: String
        get() = settings.getString("temperature_unit", "celsius")
        set(value) = settings.putString("temperature_unit", value)

    var wind: String
        get() = settings.getString("wind_unit", "kmh")
        set(value) = settings.putString("wind_unit", value)

    var pressure: String
        get() = settings.getString("pressure_unit", "hpa")
        set(value) = settings.putString("pressure_unit", value)

    fun temperature(value: Double): String {
        return if (temperature == "fahrenheit") {
            "${(value * 9 / 5 + 32).toInt()} °F"
        } else {
            "${value.toInt()} °C"
        }
    }

    fun wind(value: Double): String {
        return when (wind) {
            "mph" -> "${(value * 0.621371).toInt()} mph"
            "ms" -> "${(value / 3.6).toInt()} m/s"
            else -> "${value.toInt()} km/h"
        }
    }

    fun pressure(value: Double): String {
        return if (pressure == "mmhg") {
            "${(value * 0.750062).toInt()} mmHg"
        } else {
            "${value.toInt()} hPa"
        }
    }
}
