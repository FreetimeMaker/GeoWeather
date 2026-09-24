package com.freetime.geoweather

import android.content.Context

object WidgetLocationPreferences {
    private const val PREFS = "weather_widget_preferences"
    private const val LOCATION_PREFIX = "location_"

    fun saveLocationId(context: Context, appWidgetId: Int, locationId: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(LOCATION_PREFIX + appWidgetId, locationId)
            .apply()
    }

    fun getLocationId(context: Context, appWidgetId: Int): Long? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = LOCATION_PREFIX + appWidgetId
        return if (prefs.contains(key)) prefs.getLong(key, -1L).takeIf { it >= 0L } else null
    }

    fun delete(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(LOCATION_PREFIX + appWidgetId)
            .apply()
    }
}
