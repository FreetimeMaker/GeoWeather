package com.freetime.geoweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freetime.geoweather.data.WeatherRepository
import com.freetime.geoweather.data.WeatherApiClient
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.getRoomDatabase

class WeatherNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = getRoomDatabase(getDatabaseBuilder(applicationContext))
        val repository = WeatherRepository(
            locationDao = database.locationDao(),
            historyDao = database.weatherHistoryDao(),
            apiClient = WeatherApiClient()
        )

        val location = repository.getSelectedLocation() ?: return Result.success()
        
        // For background workers, we use the basic 1-day forecast unless we implement
        // a local cache for the subscription tier.
        repository.updateWeather(location, 1)
        
        // Notification logic would go here
        location.currentTemp?.let { temperature ->
            showNotification(location.name, temperature)
        }
        
        return Result.success()
    }

    private fun showNotification(locationName: String, temperature: Double) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "weather_updates"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Weather updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, channelId)
        } else {
            Notification.Builder(applicationContext)
        }
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Weather in $locationName")
            .setContentText("Current temperature: $temperature°")
            .setAutoCancel(true)
            .build()

        manager.notify(locationName.hashCode(), notification)
    }
}
