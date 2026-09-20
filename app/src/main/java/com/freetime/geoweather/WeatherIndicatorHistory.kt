package com.freetime.geoweather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WeatherIndicatorRecord(
    val location: String,
    val label: String,
    val startedAt: Long,
    val endedAt: Long? = null
)

object WeatherIndicatorHistory {
    private const val PREFS = "weather_indicator_history"
    private const val KEY = "records"
    private const val MAX_RECORDS = 30

    fun update(context: Context, location: String, activeLabels: List<String>) {
        val now = System.currentTimeMillis()
        val records = load(context).toMutableList()
        val activeForLocation = records.filter { it.location == location && it.endedAt == null }

        activeLabels.filterNot { label -> activeForLocation.any { it.label == label } }.forEach { label ->
            records.add(0, WeatherIndicatorRecord(location, label, now))
        }

        activeForLocation.filterNot { it.label in activeLabels }.forEach { ended ->
            val index = records.indexOfFirst {
                it.location == ended.location && it.label == ended.label && it.startedAt == ended.startedAt
            }
            if (index >= 0) records[index] = ended.copy(endedAt = now)
        }
        save(context, records.sortedByDescending { it.startedAt }.take(MAX_RECORDS))
    }

    fun recent(context: Context, location: String): List<WeatherIndicatorRecord> =
        load(context).filter { it.location == location }.take(8)

    private fun load(context: Context): List<WeatherIndicatorRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        WeatherIndicatorRecord(
                            location = o.getString("location"),
                            label = o.getString("label"),
                            startedAt = o.getLong("startedAt"),
                            endedAt = if (o.has("endedAt")) o.getLong("endedAt") else null
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, records: List<WeatherIndicatorRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(JSONObject().apply {
                put("location", record.location)
                put("label", record.label)
                put("startedAt", record.startedAt)
                record.endedAt?.let { put("endedAt", it) }
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}
