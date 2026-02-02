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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class WeatherSnapshot(
    val temperature: Double,
    val windSpeed: Double,
    val precipitation: Double,
    val weatherCode: Int,
    val timestamp: Long
)

class WeatherChangeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("weather_notifications", Context.MODE_PRIVATE)
            val changeAlertsEnabled = sharedPreferences.getBoolean("change_alerts_enabled", false)
            
            if (!changeAlertsEnabled) {
                return@withContext Result.success()
            }

            // Get saved location
            val locationName = sharedPreferences.getString("location_name", "Current Location") ?: "Current Location"
            val lat = sharedPreferences.getFloat("location_lat", 52.5200f).toDouble()
            val lon = sharedPreferences.getFloat("location_lon", 13.4050f).toDouble()

            // Fetch current weather data
            val currentWeather = fetchCurrentWeatherData(lat, lon)
            val currentSnapshot = WeatherSnapshot(
                temperature = currentWeather.getDouble("temp"),
                windSpeed = currentWeather.getDouble("windspeed"),
                precipitation = currentWeather.getDouble("precipitation"),
                weatherCode = currentWeather.getInt("weather_code"),
                timestamp = System.currentTimeMillis()
            )

            // Get last saved weather snapshot
            val lastSnapshot = getLastWeatherSnapshot(sharedPreferences)

            if (lastSnapshot != null) {
                val changes = detectWeatherChanges(lastSnapshot, currentSnapshot)
                if (changes.isNotEmpty()) {
                    sendWeatherChangeAlert(applicationContext, locationName, changes)
                }
            }

            // Save current snapshot
            saveWeatherSnapshot(sharedPreferences, currentSnapshot)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun fetchCurrentWeatherData(lat: Double, lon: Double): JSONObject {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=precipitation_probability,windspeed_10m&timezone=auto"
        val response = URL(url).readText()
        val json = JSONObject(response)
        
        val currentWeather = json.getJSONObject("current_weather")
        val hourly = json.getJSONObject("hourly")
        
        // Get current hour index
        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())
        val times = hourly.getJSONArray("time")
        var currentIndex = 0
        for (i in 0 until times.length()) {
            if (times.getString(i).startsWith(currentTime.substring(0, 13))) {
                currentIndex = i
                break
            }
        }
        
        // Add precipitation and wind speed to current weather
        currentWeather.put("precipitation", hourly.getJSONArray("precipitation_probability").getDouble(currentIndex))
        currentWeather.put("windspeed", hourly.getJSONArray("windspeed_10m").getDouble(currentIndex))
        
        return currentWeather
    }

    private fun getLastWeatherSnapshot(sharedPreferences: SharedPreferences): WeatherSnapshot? {
        return try {
            val temp = sharedPreferences.getFloat("last_temp", Float.NaN).toDouble()
            val wind = sharedPreferences.getFloat("last_wind", Float.NaN).toDouble()
            val precip = sharedPreferences.getFloat("last_precip", Float.NaN).toDouble()
            val code = sharedPreferences.getInt("last_weather_code", -1)
            val timestamp = sharedPreferences.getLong("last_weather_timestamp", 0)
            
            if (temp.isNaN() || wind.isNaN() || precip.isNaN() || code == -1 || timestamp == 0L) {
                null
            } else {
                WeatherSnapshot(temp, wind, precip, code, timestamp)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveWeatherSnapshot(sharedPreferences: SharedPreferences, snapshot: WeatherSnapshot) {
        sharedPreferences.edit()
            .putFloat("last_temp", snapshot.temperature.toFloat())
            .putFloat("last_wind", snapshot.windSpeed.toFloat())
            .putFloat("last_precip", snapshot.precipitation.toFloat())
            .putInt("last_weather_code", snapshot.weatherCode)
            .putLong("last_weather_timestamp", snapshot.timestamp)
            .apply()
    }

    private fun detectWeatherChanges(last: WeatherSnapshot, current: WeatherSnapshot): List<String> {
        val changes = mutableListOf<String>()
        
        // Temperature change threshold: 5°C
        if (kotlin.math.abs(current.temperature - last.temperature) >= 5.0) {
            changes.add("Temperature: ${last.temperature.toInt()}°C → ${current.temperature.toInt()}°C")
        }
        
        // Wind speed increase threshold: 15 km/h
        if (current.windSpeed - last.windSpeed >= 15.0) {
            changes.add("Wind increased to ${current.windSpeed.toInt()} km/h")
        }
        
        // Precipitation increase threshold: 30%
        if (current.precipitation - last.precipitation >= 30.0) {
            changes.add("Precipitation chance: ${last.precipitation.toInt()}% → ${current.precipitation.toInt()}%")
        }
        
        // Weather condition change
        if (last.weatherCode != current.weatherCode) {
            val lastDesc = getWeatherDescription(last.weatherCode)
            val currentDesc = getWeatherDescription(current.weatherCode)
            if (lastDesc != currentDesc) {
                changes.add("Condition: $lastDesc → $currentDesc")
            }
        }
        
        return changes
    }

    private fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Clear"
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

    private fun sendWeatherChangeAlert(
        context: Context,
        locationName: String,
        changes: List<String>
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel
        val channel = NotificationChannel(
            "weather_change_alerts",
            "Weather Change Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts for significant weather changes"
        }
        notificationManager.createNotificationChannel(channel)
        
        // Create intent for opening app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Format notification message
        val changeText = changes.joinToString("\n")
        val message = context.getString(R.string.WeatherChangeTXT) + " in $locationName:\n$changeText"
        
        val notification = NotificationCompat.Builder(context, "weather_change_alerts")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.WeatherChangeTXT))
            .setContentText("Weather changed in $locationName")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
