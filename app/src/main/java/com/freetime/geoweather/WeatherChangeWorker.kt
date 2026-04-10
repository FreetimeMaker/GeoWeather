package com.freetime.geoweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
            val db = LocationDatabase.getDatabase(applicationContext)
            
            // Only send change alerts for the selected location
            val selectedLocation = db.locationDao().getSelectedLocation()
            
            if (selectedLocation != null && selectedLocation.changeAlertsEnabled) {
                try {
                    // Fetch current weather data for the selected location
                    val currentWeather = fetchCurrentWeatherData(selectedLocation.latitude, selectedLocation.longitude)
                    val currentSnapshot = WeatherSnapshot(
                        temperature = currentWeather.getDouble("temperature"),
                        windSpeed = currentWeather.optDouble("windspeed", 0.0),
                        precipitation = currentWeather.optDouble("precipitation", 0.0),
                        weatherCode = currentWeather.getInt("weathercode"),
                        timestamp = System.currentTimeMillis()
                    )

                    // Get last saved weather snapshot for this location
                    val lastSnapshot = getLastWeatherSnapshot(applicationContext, selectedLocation.id)

                    if (lastSnapshot != null) {
                        val changes = detectWeatherChanges(applicationContext, lastSnapshot, currentSnapshot)
                        if (changes.isNotEmpty()) {
                            sendWeatherChangeAlert(applicationContext, selectedLocation.name, changes, selectedLocation.id)
                        }
                    }

                    // Save current snapshot for this location
                    saveWeatherSnapshot(applicationContext, currentSnapshot, selectedLocation.id)
                } catch (_: Exception) {
                    // Error handling
                }
            }
            
            Result.success()
        } catch (_: Exception) {
            // Log intentionally omitted
            Result.failure()
        }
    }

    private fun fetchCurrentWeatherData(lat: Double, lon: Double): JSONObject {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=precipitation_probability,windspeed_10m&timezone=auto"
        val response = URL(url).readText()
        val json = JSONObject(response)
        
        val currentWeather = if (json.has("current_weather")) json.getJSONObject("current_weather") else JSONObject()
        val hourly = if (json.has("hourly")) json.getJSONObject("hourly") else JSONObject()

        // Get current hour index (best-effort)
        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).format(Date())
        if (hourly.has("time")) {
            val times = hourly.getJSONArray("time")
            var currentIndex = 0
            for (i in 0 until times.length()) {
                if (times.getString(i).startsWith(currentTime.substring(0, 13))) {
                    currentIndex = i
                    break
                }
            }
            // Add precipitation and wind speed to current weather if available
            if (hourly.has("precipitation_probability")) {
                try {
                    currentWeather.put("precipitation", hourly.getJSONArray("precipitation_probability").getDouble(currentIndex))
                } catch (_: Exception) { /* ignore */ }
            }
            if (hourly.has("windspeed_10m")) {
                try {
                    currentWeather.put("windspeed", hourly.getJSONArray("windspeed_10m").getDouble(currentIndex))
                } catch (_: Exception) { /* ignore */ }
            }
        }

        return currentWeather
    }

    private fun getLastWeatherSnapshot(context: Context, locationId: Long): WeatherSnapshot? {
        return try {
            val sharedPreferences = context.getSharedPreferences("weather_change_$locationId", Context.MODE_PRIVATE)
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
        } catch (_: Exception) {
            null
        }
    }

    private fun saveWeatherSnapshot(context: Context, snapshot: WeatherSnapshot, locationId: Long) {
        val sharedPreferences = context.getSharedPreferences("weather_change_$locationId", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putFloat("last_temp", snapshot.temperature.toFloat())
            .putFloat("last_wind", snapshot.windSpeed.toFloat())
            .putFloat("last_precip", snapshot.precipitation.toFloat())
            .putInt("last_weather_code", snapshot.weatherCode)
            .putLong("last_weather_timestamp", snapshot.timestamp)
            .apply()
    }

    private fun detectWeatherChanges(context: Context, last: WeatherSnapshot, current: WeatherSnapshot): List<String> {
        val changes = mutableListOf<String>()
        val sharedPreferences = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
        val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"
        val windUnit = sharedPreferences.getString("wind_unit", "kmh") ?: "kmh"
        
        // Temperature change threshold: 5°C
        if (kotlin.math.abs(current.temperature - last.temperature) >= 5.0) {
            val lastTemp = if (tempUnit == "fahrenheit") (last.temperature * 9/5 + 32).toInt().toString() + "°F" else last.temperature.toInt().toString() + "°C"
            val currentTemp = if (tempUnit == "fahrenheit") (current.temperature * 9/5 + 32).toInt().toString() + "°F" else current.temperature.toInt().toString() + "°C"
            changes.add("Temperature: $lastTemp → $currentTemp")
        }
        
        // Wind speed increase threshold: 15 km/h
        if (current.windSpeed - last.windSpeed >= 15.0) {
            val displayWind = if (windUnit == "mph") (current.windSpeed * 0.621371).toInt().toString() + " mph" else current.windSpeed.toInt().toString() + " km/h"
            changes.add("Wind increased to $displayWind")
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
        changes: List<String>,
        locationId: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel (Android O+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "weather_change_alerts",
                "Weather Change Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for significant weather changes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for opening app with specific location
        val intent = Intent(context, WeatherDetailActivity::class.java).apply {
            putExtra("name", locationName)
            putExtra("lat", 0.0) // These will be fetched from DB in the activity
            putExtra("lon", 0.0)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            locationId.toInt() + 1000, // Use different offset to avoid conflicts
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
        
        // Use location ID + offset for unique notification ID
        notificationManager.notify(locationId.toInt() + 1000, notification)
    }
}
