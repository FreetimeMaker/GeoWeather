package com.freetime.geoweather

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

abstract class BaseWeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetLocationPreferences.delete(context, it) }
        super.onDeleted(context, appWidgetIds)
    }
}

class WeatherWidgetReceiver : BaseWeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}

class CompactWeatherWidgetReceiver : BaseWeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWeatherWidget()
}

class ForecastWeatherWidgetReceiver : BaseWeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ForecastWeatherWidget()
}

class DetailedWeatherWidgetReceiver : BaseWeatherWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DetailedWeatherWidget()
}
