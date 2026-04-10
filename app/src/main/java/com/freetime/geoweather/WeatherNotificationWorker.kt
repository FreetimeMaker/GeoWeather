package com.freetime.geoweather

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freetime.geoweather.data.LocationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class WeatherNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "WeatherNotificationWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "doWork started")
            // Check notification permissions first
            if (!hasNotificationPermission()) {
                Log.d(TAG, "No notification permission")
                return@withContext Result.failure()
            }
            
            val db = LocationDatabase.getDatabase(applicationContext)
            
            // Only send notifications for the selected location
            val selectedLocation = db.locationDao().getSelectedLocation()
            
            if (selectedLocation != null && selectedLocation.notificationsEnabled) {
                try {
                    // Fetch current weather data for the selected location
                    val weatherData = fetchWeatherData(selectedLocation.latitude, selectedLocation.longitude)
                    val temp = weatherData.getDouble("temperature")
                    val weatherCode = weatherData.getInt("weathercode")
                    val weatherDescription = getWeatherDescription(weatherCode)

                    Log.d(TAG, "Preparing notification for ${selectedLocation.name}: $temp, $weatherDescription")

                    // Send notification for this location
                    sendWeatherNotification(
                        applicationContext, 
                        selectedLocation.name, 
                        selectedLocation.latitude,
                        selectedLocation.longitude,
                        temp, 
                        weatherDescription,
                        selectedLocation.id
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching weather for selected location", e)
                }
            } else {
                Log.d(TAG, "No selected location with notifications enabled")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork failed", e)
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun fetchWeatherData(lat: Double, lon: Double): JSONObject {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&timezone=auto"
        val response = URL(url).readText()
        val json = JSONObject(response)
        // Return the current_weather object (contains temperature, windspeed, weathercode, etc.)
        return if (json.has("current_weather")) json.getJSONObject("current_weather") else json
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Notifications are automatically granted on older versions
        }
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
        latitude: Double,
        longitude: Double,
        temperature: Double,
        weatherDescription: String,
        locationId: Long
    ) {
        if (!hasNotificationPermission()) {
            Log.d(TAG, "sendWeatherNotification: permission missing")
            return
        }
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sharedPreferences = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
        val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"
        
        // Create notification channel (only on Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "weather_notifications",
                "Weather Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily weather notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for opening app with specific location
        val intent = Intent(context, WeatherDetailActivity::class.java).apply {
            putExtra("name", locationName)
            putExtra("lat", latitude)
            putExtra("lon", longitude)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            locationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Format notification message
        val displayTemp = if (tempUnit == "fahrenheit") (temperature * 9/5 + 32).toInt() else temperature.toInt()
        val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
        val tempText = "$displayTemp$tempSuffix"
        val message = context.getString(R.string.WeatherNotificationTXT, locationName, tempText, weatherDescription)
        
        val notification = NotificationCompat.Builder(context, "weather_notifications")
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(context.getString(R.string.DailyWeatherUpdateTXT))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Use location ID for unique notification ID
        notificationManager.notify(locationId.toInt(), notification)
        Log.d(TAG, "Notification sent for $locationName (id=${locationId.toInt()})")
    }
}
