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

        val location = repository.getSelectedLocation() ?: return Result.success()

        try {
            if (!location.changeAlertsEnabled) return Result.success()\n            val oldTemp = location.currentTemp\n            val oldWind = location.currentWindSpeed
            val updatedLocation = repository.refreshSelectedLocationWeather() ?: location
            val newTemp = updatedLocation.currentTemp\n            val newWind = updatedLocation.currentWindSpeed\n            val tempChanged = oldTemp != null && newTemp != null && kotlin.math.abs(newTemp - oldTemp) >= appSettings.tempThreshold.value\n            val windChanged = oldWind != null && newWind != null && newWind - oldWind >= appSettings.windThreshold.value

            if (tempChanged || windChanged) {
                val oldTempStr = repository.getDisplayTemp(location, appSettings.tempUnit.value)
                val newTempStr = repository.getDisplayTemp(updatedLocation, appSettings.tempUnit.value)
                val message = applicationContext.getString(
                    SharedRes.string.temperature_change_msg,
                    oldTempStr,
                    newTempStr
                )
                WeatherNotifications.show(
                    applicationContext,
                    2002,
                    applicationContext.getString(SharedRes.string.app_name),
                    message,
                    alert = true
                )
            }
        } catch (e: Exception) {
            return Result.retry()
        }

        return Result.success()
    }
}
