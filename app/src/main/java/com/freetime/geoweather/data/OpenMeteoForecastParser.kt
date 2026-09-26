package com.freetime.geoweather.data

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.*

internal object OpenMeteoForecastParser {
    fun hourly(data: String, nowMillis: Long = Clock.System.now().toEpochMilliseconds()): List<HourlyForecast> {
        val json = Json.parseToJsonElement(data).jsonObject
        val hourly = json["hourly"]?.jsonObject ?: return emptyList()
        val times = hourly["time"]?.jsonArray ?: return emptyList()
        val temps = hourly["temperature_2m"]?.jsonArray ?: return emptyList()
        val codes = hourly["weather_code"]?.jsonArray ?: hourly["weathercode"]?.jsonArray ?: return emptyList()
        val zone = runCatching { TimeZone.of(json["timezone"]?.jsonPrimitive?.content ?: "UTC") }.getOrDefault(TimeZone.UTC)
        val currentHourPrefix = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone).toString().take(13)
        val startIndex = times.indexOfFirst { it.jsonPrimitive.content.startsWith(currentHourPrefix) }.takeIf { it >= 0 } ?: 0
        val count = (minOf(times.size, temps.size, codes.size) - startIndex).coerceAtLeast(0)
        fun array(name: String) = hourly[name]?.jsonArray
        return List(count) { offset ->
            val i = startIndex + offset
            val time = times[i].jsonPrimitive.contentOrNull ?: return@List null
            val temp = temps[i].jsonPrimitive.doubleOrNull?.toInt() ?: return@List null
            val code = codes[i].jsonPrimitive.intOrNull ?: return@List null
            HourlyForecast(time, temp, code,
                array("precipitation_probability")?.getOrNull(i)?.jsonPrimitive?.intOrNull ?: 0,
                array("relative_humidity_2m")?.getOrNull(i)?.jsonPrimitive?.intOrNull,
                array("apparent_temperature")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("visibility")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull?.div(1000.0),
                array("pressure_msl")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("cloud_base")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("precipitation")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("rain")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("snowfall")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("wind_speed_10m")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("wind_gusts_10m")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("uv_index")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull)
        }.filterNotNull()
    }

    fun daily(data: String): List<DailyForecast> {
        val json = Json.parseToJsonElement(data).jsonObject
        val daily = json["daily"]?.jsonObject ?: return emptyList()
        val dates = daily["time"]?.jsonArray ?: return emptyList()
        val codes = daily["weather_code"]?.jsonArray ?: daily["weathercode"]?.jsonArray ?: return emptyList()
        val maxTemps = daily["temperature_2m_max"]?.jsonArray ?: return emptyList()
        val minTemps = daily["temperature_2m_min"]?.jsonArray ?: return emptyList()
        fun array(name: String) = daily[name]?.jsonArray
        return List(minOf(dates.size, codes.size, maxTemps.size, minTemps.size, 16)) { i ->
            val date = dates[i].jsonPrimitive.contentOrNull ?: return@List null
            val code = codes[i].jsonPrimitive.intOrNull ?: return@List null
            val max = maxTemps[i].jsonPrimitive.doubleOrNull?.toInt() ?: return@List null
            val min = minTemps[i].jsonPrimitive.doubleOrNull?.toInt() ?: return@List null
            DailyForecast(date, code, max, min,
                array("sunrise")?.getOrNull(i)?.jsonPrimitive?.content ?: "--",
                array("sunset")?.getOrNull(i)?.jsonPrimitive?.content ?: "--",
                array("precipitation_sum")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull ?: 0.0,
                array("precipitation_probability_max")?.getOrNull(i)?.jsonPrimitive?.intOrNull ?: 0,
                array("wind_speed_10m_max")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull ?: 0.0,
                array("apparent_temperature_max")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("apparent_temperature_min")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("daylight_duration")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("sunshine_duration")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("uv_index_max")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("rain_sum")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("snowfall_sum")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("precipitation_hours")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                array("wind_gusts_10m_max")?.getOrNull(i)?.jsonPrimitive?.doubleOrNull)
        }.filterNotNull()
    }
}
