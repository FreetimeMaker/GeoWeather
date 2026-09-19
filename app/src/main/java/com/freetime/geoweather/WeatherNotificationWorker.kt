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

    override suspend fun doWork(): Result {
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()

        val locations = repository.getAllLocationsSync().filter { it.notificationsEnabled }
        if (locations.isEmpty()) return Result.success()

        var shouldRetry = false
        locations.forEach { location ->
            try {
                val updatedLocation = repository.refreshLocationWeather(location.id) ?: location
                val tempStr = repository.getDisplayTemp(updatedLocation, appSettings.tempUnit.value)
                val code = updatedLocation.currentWeatherCode ?: 0
                val description = WeatherCodes.getDescription(code)
                val message = applicationContext.getString(
                    SharedRes.string.WeatherNotificationTXT,
                    updatedLocation.name,
                    tempStr,
                    description
                )
                WeatherNotifications.show(
                    applicationContext,
                    2001 + (location.id % 100000).toInt(),
                    applicationContext.getString(SharedRes.string.app_name),
                    message
                )
            } catch (_: Exception) {
                shouldRetry = true
            }
        }
        if (shouldRetry) return Result.retry()

        return Result.success()
    }
}
