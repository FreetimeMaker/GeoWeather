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
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
