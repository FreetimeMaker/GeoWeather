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
    val windDelta: Double,
    val recordedAt: Long = System.currentTimeMillis()
) {
    val hasMeaningfulChange: Boolean
        get() = kotlin.math.abs(maxTempDelta) >= 1 ||
            kotlin.math.abs(minTempDelta) >= 1 ||
            kotlin.math.abs(rainProbabilityDelta) >= 10 ||
            kotlin.math.abs(windDelta) >= 5.0
}

private data class ForecastSnapshotDay(
    val date: String,
    val maxTemp: Int,
    val minTemp: Int,
    val rainProbability: Int,
    val windMax: Double
)

object ForecastChangeHistory {
    private const val PREFS = "forecast_change_history"

    fun compareAndStore(context: Context, locationKey: String, current: List<DailyForecast>): List<ForecastChange> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val snapshotKey = locationKey + "_snapshot"
        val changesKey = locationKey + "_changes"
        val currentSnapshot = current.take(16).map {
            ForecastSnapshotDay(it.date, it.maxTemp, it.minTemp, it.precipProbMax, it.windMax)
        }
        val encodedCurrent = encodeSnapshot(currentSnapshot)
        val encodedPrevious = prefs.getString(snapshotKey, null)

        if (encodedPrevious == encodedCurrent) {
            return parseChanges(prefs.getString(changesKey, null))
        }

        val previous = parseSnapshot(encodedPrevious)
        val changes = currentSnapshot.mapNotNull { now ->
            val old = previous.firstOrNull { it.date == now.date } ?: return@mapNotNull null
            ForecastChange(
                date = now.date,
                maxTempDelta = now.maxTemp - old.maxTemp,
                minTempDelta = now.minTemp - old.minTemp,
                rainProbabilityDelta = now.rainProbability - old.rainProbability,
                windDelta = now.windMax - old.windMax
            )
        }.filter { it.hasMeaningfulChange }

        val history = (changes + parseChanges(prefs.getString(changesKey, null)))
            .sortedByDescending { it.recordedAt }
            .take(80)
        prefs.edit()
            .putString(snapshotKey, encodedCurrent)
            .putString(changesKey, encodeChanges(history))
            .apply()
        return history
    }

    private fun encodeSnapshot(days: List<ForecastSnapshotDay>): String = JSONArray().apply {
        days.forEach { day -> put(JSONObject().apply {
            put("date", day.date); put("max", day.maxTemp); put("min", day.minTemp)
            put("rain", day.rainProbability); put("wind", day.windMax)
        }) }
    }.toString()

    private fun parseSnapshot(raw: String?): List<ForecastSnapshotDay> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(ForecastSnapshotDay(o.getString("date"), o.getInt("max"), o.getInt("min"), o.optInt("rain"), o.optDouble("wind")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeChanges(changes: List<ForecastChange>): String = JSONArray().apply {
        changes.forEach { change -> put(JSONObject().apply {
            put("date", change.date); put("max", change.maxTempDelta); put("min", change.minTempDelta)
            put("rain", change.rainProbabilityDelta); put("wind", change.windDelta); put("at", change.recordedAt)
        }) }
    }.toString()

    private fun parseChanges(raw: String?): List<ForecastChange> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(ForecastChange(o.getString("date"), o.getInt("max"), o.getInt("min"), o.getInt("rain"), o.getDouble("wind"), o.optLong("at", 0L)))
                }
            }
        }.getOrDefault(emptyList())
    }
}
