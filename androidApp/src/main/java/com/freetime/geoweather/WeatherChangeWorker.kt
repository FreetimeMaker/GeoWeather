package com.freetime.geoweather

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freetime.geoweather.data.DependencyManager
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.getString

class WeatherChangeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()

        val location = repository.getSelectedLocation() ?: return Result.success()
        
        try {
            val oldTemp = location.currentTemp
            
            val updatedLocation = repository.refreshSelectedLocationWeather() ?: location
            val newTemp = updatedLocation.currentTemp
            
            if (oldTemp != null && newTemp != null && Math.abs(newTemp - oldTemp) >= 2.0) {
                val oldTempStr = repository.getDisplayTemp(location, appSettings.tempUnit.value)
                val newTempStr = repository.getDisplayTemp(updatedLocation, appSettings.tempUnit.value)
                
                val msg = getString(Res.string.temperature_change_msg, oldTempStr, newTempStr)
                // In a real app, you'd trigger a notification here
            }
            
        } catch (e: Exception) {
            return Result.retry()
        }
        
        return Result.success()
    }
}
