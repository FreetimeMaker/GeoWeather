package com.freetime.geoweather

import android.content.Context
import com.freetime.geoweather.data.DailyForecast
import org.json.JSONArray
import org.json.JSONObject

data class ForecastChange(
    val date: String,
    val maxTempDelta: Int,
    val minTempDelta: Int,
    val rainProbabilityDelta: Int,
    val windDelta: Double
) {
    val hasMeaningfulChange: Boolean
        get() = kotlin.math.abs(maxTempDelta) >= 1 ||
            kotlin.math.abs(minTempDelta) >= 1 ||
            kotlin.math.abs(rainProbabilityDelta) >= 10 ||
            kotlin.math.abs(windDelta) >= 5.0
}

object ForecastChangeHistory {
    private const val PREFS = "forecast_change_history"

    fun compareAndStore(context: Context, locationKey: String, current: List<DailyForecast>): List<ForecastChange> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = parse(prefs.getString(locationKey, null))
        val changes = current.mapNotNull { now ->
            val old = previous.firstOrNull { it.date == now.date } ?: return@mapNotNull null
            ForecastChange(
                date = now.date,
                maxTempDelta = now.maxTemp - old.maxTemp,
                minTempDelta = now.minTemp - old.minTemp,
                rainProbabilityDelta = now.precipProbMax - old.precipProbMax,
                windDelta = now.windMax - old.windMax
            )
        }.filter { it.hasMeaningfulChange }
        prefs.edit().putString(locationKey, encode(current)).apply()
        return changes
    }

    private fun encode(days: List<DailyForecast>): String {
        val array = JSONArray()
        days.take(16).forEach { day ->
            array.put(JSONObject().apply {
                put("date", day.date)
                put("max", day.maxTemp)
                put("min", day.minTemp)
                put("rain", day.precipProbMax)
                put("wind", day.windMax)
            })
        }
        return array.toString()
    }

    private fun parse(raw: String?): List<DailyForecast> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        DailyForecast(
                            date = o.getString("date"),
                            code = 0,
                            maxTemp = o.getInt("max"),
                            minTemp = o.getInt("min"),
                            precipProbMax = o.optInt("rain"),
                            windMax = o.optDouble("wind")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
