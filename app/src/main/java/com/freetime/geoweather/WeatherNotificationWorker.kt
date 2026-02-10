package com.freetime.geoweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freetime.geoweather.data.LocationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class WeatherNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = LocationDatabase.getDatabase(applicationContext)
            val locations = db.locationDao().getAllLocationsSync()
            
            for (location in locations) {
                if (location.notificationsEnabled) {
                    // Fetch current weather data for this location
                    val weatherData = fetchWeatherData(location.latitude, location.longitude)
                    val temp = weatherData.getDouble("temp")
                    val weatherCode = weatherData.getInt("weather_code")
                    val weatherDescription = getWeatherDescription(weatherCode)

                    // Send notification for this location
                    sendWeatherNotification(
                        applicationContext, 
                        location.name, 
                        temp, 
                        weatherDescription,
                        location.id
                    )
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun fetchWeatherData(lat: Double, lon: Double): JSONObject {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&timezone=auto"
        val response = URL(url).readText()
        val json = JSONObject(response)
        return json.getJSONObject("current_weather")
    }

    private fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Clear sky"
            1, 2, 3 -> "Partly cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown"
        }
    }

    private fun sendWeatherNotification(
        context: Context,
        locationName: String,
        temperature: Double,
        weatherDescription: String,
        locationId: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel
        val channel = NotificationChannel(
            "weather_notifications",
            "Weather Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily weather notifications"
        }
        notificationManager.createNotificationChannel(channel)
        
        // Create intent for opening app with specific location
        val intent = Intent(context, WeatherDetailActivity::class.java).apply {
            putExtra("name", locationName)
            putExtra("lat", 0.0) // These will be fetched from DB in the activity
            putExtra("lon", 0.0)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            locationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Format notification message
        val tempText = "${temperature.toInt()}°C"
        val message = context.getString(R.string.WeatherNotificationTXT, locationName, tempText, weatherDescription)
        
        val notification = NotificationCompat.Builder(context, "weather_notifications")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.DailyWeatherUpdateTXT))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Use location ID for unique notification ID
        notificationManager.notify(locationId.toInt(), notification)
    }
}
