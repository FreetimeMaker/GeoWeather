package com.freetime.geoweather

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.R as SharedRes

class WeatherNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_LOCATION_ID = "location_id"
    }

    override suspend fun doWork(): Result {
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()
        val locationId = inputData.getLong(KEY_LOCATION_ID, -1L)
        if (locationId < 0) return Result.success()

        val location = repository.getAllLocationsSync().firstOrNull { it.id == locationId }
            ?: return Result.success()
        if (!location.notificationsEnabled) return Result.success()

        return try {
            val updatedLocation = repository.refreshLocationWeather(location.id) ?: location
            val tempStr = repository.getDisplayTemp(updatedLocation, appSettings.tempUnit.value)
            val code = updatedLocation.currentWeatherCode ?: 0
            val description = WeatherCodes.getDescription(code)
            val hourly = repository.getHourlyForecasts(updatedLocation)
            val daily = repository.getDailyForecasts(updatedLocation).firstOrNull()
            val rainPeak = hourly.take(12).maxOfOrNull { it.precipProbability } ?: 0
            val windPeak = hourly.take(12).maxOfOrNull { it.windGusts ?: it.windSpeed ?: 0.0 } ?: 0.0
            val hour = java.time.LocalTime.now().hour
            val period = applicationContext.getString(
                if (hour < 12) SharedRes.string.briefing_morning else SharedRes.string.briefing_evening
            )
            val range = daily?.let { it.minTemp.toString() + "–" + it.maxTemp + "°" } ?: tempStr
            val message = applicationContext.getString(
                SharedRes.string.daily_weather_notification,
                period,
                updatedLocation.name,
                range,
                description,
                rainPeak,
                windPeak.toInt()
            )
            WeatherNotifications.show(
                applicationContext,
                2001 + (location.id % 100000).toInt(),
                applicationContext.getString(SharedRes.string.app_name),
                message
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
