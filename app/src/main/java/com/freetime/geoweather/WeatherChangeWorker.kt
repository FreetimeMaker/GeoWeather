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
            val sharedPreferences = applicationContext.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
            
            val selectedLocation = db.locationDao().getSelectedLocation()
            
            if (selectedLocation != null && selectedLocation.changeAlertsEnabled) {
                try {
                    val currentWeather = fetchCurrentWeatherData(selectedLocation.latitude, selectedLocation.longitude)
                    val currentSnapshot = WeatherSnapshot(
                        temperature = currentWeather.getDouble("temperature"),
                        windSpeed = currentWeather.optDouble("windspeed", 0.0),
                        precipitation = currentWeather.optDouble("precipitation", 0.0),
                        weatherCode = currentWeather.getInt("weathercode"),
                        timestamp = System.currentTimeMillis()
                    )

                    val lastSnapshot = getLastWeatherSnapshot(applicationContext, selectedLocation.id)

                    if (lastSnapshot != null) {
                        val tempThreshold = sharedPreferences.getInt("notif_temp_threshold", 5).toDouble()
                        val windThreshold = sharedPreferences.getInt("notif_wind_threshold", 15).toDouble()
                        
                        val changes = detectWeatherChanges(applicationContext, lastSnapshot, currentSnapshot, tempThreshold, windThreshold)
                        if (changes.isNotEmpty()) {
                            sendWeatherChangeAlert(applicationContext, selectedLocation.name, changes, selectedLocation.id)
                        }
                    }

                    saveWeatherSnapshot(applicationContext, currentSnapshot, selectedLocation.id)
                } catch (_: Exception) {}
            }
            
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private fun fetchCurrentWeatherData(lat: Double, lon: Double): JSONObject {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=precipitation_probability,windspeed_10m&timezone=auto"
        val response = URL(url).readText()
        val json = JSONObject(response)
        
        val currentWeather = if (json.has("current_weather")) json.getJSONObject("current_weather") else JSONObject()
        val hourly = if (json.has("hourly")) json.getJSONObject("hourly") else JSONObject()

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
            if (hourly.has("precipitation_probability")) {
                try {
                    currentWeather.put("precipitation", hourly.getJSONArray("precipitation_probability").getDouble(currentIndex))
                } catch (_: Exception) {}
            }
            if (hourly.has("windspeed_10m")) {
                try {
                    currentWeather.put("windspeed", hourly.getJSONArray("windspeed_10m").getDouble(currentIndex))
                } catch (_: Exception) {}
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
            
            if (temp.isNaN() || wind.isNaN() || precip.isNaN() || code == -1 || timestamp == 0L) null
            else WeatherSnapshot(temp, wind, precip, code, timestamp)
        } catch (_: Exception) { null }
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

    private fun detectWeatherChanges(
        context: Context, 
        last: WeatherSnapshot, 
        current: WeatherSnapshot,
        tempThreshold: Double,
        windThreshold: Double
    ): List<String> {
        val changes = mutableListOf<String>()
        val sharedPreferences = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
        val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"
        val windUnit = sharedPreferences.getString("wind_unit", "kmh") ?: "kmh"
        
        if (kotlin.math.abs(current.temperature - last.temperature) >= tempThreshold) {
            val lastTemp = if (tempUnit == "fahrenheit") (last.temperature * 9/5 + 32).toInt().toString() + "°F" else last.temperature.toInt().toString() + "°C"
            val currentTemp = if (tempUnit == "fahrenheit") (current.temperature * 9/5 + 32).toInt().toString() + "°F" else current.temperature.toInt().toString() + "°C"
            changes.add(context.getString(R.string.temperature_change_msg, lastTemp, currentTemp))
        }
        
        if (current.windSpeed - last.windSpeed >= windThreshold) {
            val displayWind = if (windUnit == "mph") (current.windSpeed * 0.621371).toInt().toString() + " mph" else current.windSpeed.toInt().toString() + " km/h"
            changes.add(context.getString(R.string.wind_increase_msg, displayWind))
        }
        
        if (current.precipitation - last.precipitation >= 30.0) {
            changes.add(context.getString(R.string.precip_increase_msg, current.precipitation.toInt()))
        }
        
        if (last.weatherCode != current.weatherCode) {
            val lastDesc = WeatherCodes.getDescription(last.weatherCode, context)
            val currentDesc = WeatherCodes.getDescription(current.weatherCode, context)
            if (lastDesc != currentDesc) {
                changes.add(context.getString(R.string.condition_change_msg, lastDesc, currentDesc))
            }
        }
        
        return changes
    }

    private fun sendWeatherChangeAlert(
        context: Context,
        locationName: String,
        changes: List<String>,
        locationId: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "weather_change_alerts",
                "Weather Change Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, WeatherDetailActivity::class.java).apply {
            putExtra("name", locationName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            locationId.toInt() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val changeText = changes.joinToString("\n")
        val message = "$locationName:\n$changeText"
        
        val notification = NotificationCompat.Builder(context, "weather_change_alerts")
            .setSmallIcon(R.mipmap.icon)
            .setContentTitle(context.getString(R.string.WeatherChangeTXT))
            .setContentText(context.getString(R.string.weather_changed_in_city, locationName))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(locationId.toInt() + 1000, notification)
    }
}
