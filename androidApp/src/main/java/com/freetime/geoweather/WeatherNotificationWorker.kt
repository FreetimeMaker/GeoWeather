package com.freetime.geoweather

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freetime.geoweather.data.DependencyManager
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.getString

class WeatherNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()

        val location = repository.getSelectedLocation() ?: return Result.success()
        
        try {
            val updatedLocation = repository.refreshSelectedLocationWeather() ?: location
            
            val tempStr = repository.getDisplayTemp(updatedLocation, appSettings.tempUnit.value)
            val code = updatedLocation.currentWeatherCode ?: 0
            val description = WeatherCodes.getDescription(code)
            
            val content = getString(
                Res.string.WeatherNotificationTXT,
                updatedLocation.name,
                tempStr,
                description
            )
            
            // Show notification logic...
        } catch (e: Exception) {
            return Result.retry()
        }
        
        return Result.success()
    }
}
