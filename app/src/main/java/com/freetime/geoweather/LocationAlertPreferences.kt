package com.freetime.geoweather

import android.content.Context

object LocationAlertPreferences {
    private const val PREFS = "location_alert_preferences"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun key(locationId: Long, name: String) = locationId.toString() + "_" + name

    fun tempThreshold(context: Context, locationId: Long, fallback: Int): Int =
        prefs(context).getInt(key(locationId, "temp_threshold"), fallback)

    fun windThreshold(context: Context, locationId: Long, fallback: Int): Int =
        prefs(context).getInt(key(locationId, "wind_threshold"), fallback)

    fun rainProbability(context: Context, locationId: Long, fallback: Int = 60): Int =
        prefs(context).getInt(key(locationId, "rain_probability"), fallback)

    fun windGustThreshold(context: Context, locationId: Long, fallback: Int = 70): Int =
        prefs(context).getInt(key(locationId, "wind_gust_threshold"), fallback)

    fun uvThreshold(context: Context, locationId: Long, fallback: Int = 6): Int =
        prefs(context).getInt(key(locationId, "uv_threshold"), fallback)

    fun frostThreshold(context: Context, locationId: Long, fallback: Int = 0): Int =
        prefs(context).getInt(key(locationId, "frost_threshold"), fallback)

    fun setTempThreshold(context: Context, locationId: Long, value: Int) =
        prefs(context).edit().putInt(key(locationId, "temp_threshold"), value).apply()

    fun setWindThreshold(context: Context, locationId: Long, value: Int) =
        prefs(context).edit().putInt(key(locationId, "wind_threshold"), value).apply()

    fun setRainProbability(context: Context, locationId: Long, value: Int) =
        prefs(context).edit().putInt(key(locationId, "rain_probability"), value).apply()

    fun setWindGustThreshold(context: Context, locationId: Long, value: Int) =
        prefs(context).edit().putInt(key(locationId, "wind_gust_threshold"), value).apply()

    fun setUvThreshold(context: Context, locationId: Long, value: Int) =
        prefs(context).edit().putInt(key(locationId, "uv_threshold"), value).apply()

    fun setFrostThreshold(context: Context, locationId: Long, value: Int) =
        prefs(context).edit().putInt(key(locationId, "frost_threshold"), value).apply()

    fun clear(context: Context, locationId: Long) {
        val editor = prefs(context).edit()
        listOf("temp_threshold", "wind_threshold", "rain_probability", "wind_gust_threshold", "uv_threshold", "frost_threshold")
            .forEach { editor.remove(key(locationId, it)) }
        editor.apply()
    }
}
