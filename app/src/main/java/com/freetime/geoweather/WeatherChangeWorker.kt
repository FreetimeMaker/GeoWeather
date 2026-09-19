package com.freetime.geoweather

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.R as SharedRes

class WeatherChangeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()
        val locations = repository.getAllLocationsSync()
            .filter { it.changeAlertsEnabled || it.notificationsEnabled }
        if (locations.isEmpty()) return Result.success()

        var retry = false
        locations.forEach { location ->
            try {
                val oldTemp = location.currentTemp
                val oldWind = location.currentWindSpeed
                val updated = repository.refreshLocationWeather(location.id) ?: location
                val newTemp = updated.currentTemp
                val newWind = updated.currentWindSpeed

                if (location.changeAlertsEnabled) {
                    val tempChanged = oldTemp != null && newTemp != null &&
                        kotlin.math.abs(newTemp - oldTemp) >= appSettings.tempThreshold.value
                    val windChanged = oldWind != null && newWind != null &&
                        kotlin.math.abs(newWind - oldWind) >= appSettings.windThreshold.value
                    if (tempChanged || windChanged) {
                        val message = applicationContext.getString(
                            SharedRes.string.temperature_change_msg,
                            repository.getDisplayTemp(location, appSettings.tempUnit.value),
                            repository.getDisplayTemp(updated, appSettings.tempUnit.value)
                        )
                        WeatherNotifications.show(
                            applicationContext,
                            200000 + (location.id % 100000).toInt(),
                            applicationContext.getString(SharedRes.string.app_name),
                            message,
                            alert = true
                        )
                    }
                }

                if (location.notificationsEnabled) {
                    val hourly = repository.getHourlyForecasts(updated)
                    val severe = hourly.take(6).firstOrNull {
                        it.code in 95..99 || it.code in 71..86 ||
                            (it.windGusts ?: 0.0) >= 70.0 ||
                            (it.precipitation ?: 0.0) >= 10.0
                    }
                    if (severe != null) {
                        val prefs = applicationContext.getSharedPreferences("weather_alert_dedupe", Context.MODE_PRIVATE)
                        val alertKey = "severe_${location.id}"
                        val signature = "${severe.time}:${severe.code}:${severe.windGusts?.toInt()}:${severe.precipitation}"
                        if (prefs.getString(alertKey, null) != signature) {
                            val reason = when {
                                severe.code in 95..99 -> WeatherCodes.getDescription(severe.code)
                                severe.code in 71..86 -> WeatherCodes.getDescription(severe.code)
                                (severe.windGusts ?: 0.0) >= 70.0 -> applicationContext.getString(SharedRes.string.strong_wind_gusts)
                                else -> applicationContext.getString(SharedRes.string.heavy_precipitation)
                            }
                            val message = applicationContext.getString(
                                SharedRes.string.extreme_weather_notification,
                                updated.name,
                                reason
                            )
                            WeatherNotifications.show(
                                applicationContext,
                                300000 + (location.id % 100000).toInt(),
                                applicationContext.getString(SharedRes.string.weather_alerts_title),
                                message,
                                alert = true
                            )
                            prefs.edit().putString(alertKey, signature).apply()
                        }
                    }

                    val rain = hourly.drop(1).take(3).firstOrNull {
                        it.precipProbability >= 60 && (it.rain ?: it.precipitation ?: 0.0) > 0.0
                    }
                    if (rain != null) {
                        val prefs = applicationContext.getSharedPreferences("weather_alert_dedupe", Context.MODE_PRIVATE)
                        val alertKey = "rain_${location.id}"
                        val signature = "${rain.time}:${rain.precipProbability}"
                        if (prefs.getString(alertKey, null) != signature) {
                            val message = applicationContext.getString(
                                SharedRes.string.rain_alert_notification,
                                rain.time,
                                updated.name,
                                rain.precipProbability
                            )
                            WeatherNotifications.show(
                                applicationContext,
                                400000 + (location.id % 100000).toInt(),
                                applicationContext.getString(SharedRes.string.next_rain_title),
                                message,
                                alert = true
                            )
                            prefs.edit().putString(alertKey, signature).apply()
                        }
                    }
                }
            } catch (_: Exception) {
                retry = true
            }
        }
        return if (retry) Result.retry() else Result.success()
    }
}
