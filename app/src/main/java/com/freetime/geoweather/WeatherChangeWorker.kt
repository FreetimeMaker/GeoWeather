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
    val provider: String,
    val timestamp: Long
)

class WeatherChangeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sharedPreferences = applicationContext.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
            val requireLogin = sharedPreferences.getBoolean("require_login", false)
            val authManager = AuthManager.getInstance(applicationContext)

            if (requireLogin && !authManager.isAuthenticated) {
                return@withContext Result.success()
            }

            val db = LocationDatabase.getDatabase(applicationContext)
            
            val selectedLocation = db.locationDao().getSelectedLocation()
            
            if (selectedLocation != null && selectedLocation.changeAlertsEnabled) {
                try {
                    val weatherRepository = WeatherRepository(applicationContext)
                    val result = weatherRepository.getWeatherData(selectedLocation.latitude, selectedLocation.longitude, 1)
                    
                    if (result is WeatherRepository.WeatherDataResult.Success) {
                        val currentSnapshot = WeatherSnapshot(
                            temperature = result.temp,
                            windSpeed = result.windSpeed,
                            precipitation = result.precipitation,
                            weatherCode = result.weatherCode,
                            provider = result.provider,
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
                    }
                } catch (_: Exception) {}
            }
            
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private fun getLastWeatherSnapshot(context: Context, locationId: Long): WeatherSnapshot? {
        return try {
            val sharedPreferences = context.getSharedPreferences("weather_change_$locationId", Context.MODE_PRIVATE)
            val temp = sharedPreferences.getFloat("last_temp", Float.NaN).toDouble()
            val wind = sharedPreferences.getFloat("last_wind", Float.NaN).toDouble()
            val precip = sharedPreferences.getFloat("last_precip", Float.NaN).toDouble()
            val code = sharedPreferences.getInt("last_weather_code", -1)
            val provider = sharedPreferences.getString("last_weather_provider", "open_meteo") ?: "open_meteo"
            val timestamp = sharedPreferences.getLong("last_weather_timestamp", 0)
            
            if (temp.isNaN() || wind.isNaN() || precip.isNaN() || code == -1 || timestamp == 0L) null
            else WeatherSnapshot(temp, wind, precip, code, provider, timestamp)
        } catch (_: Exception) { null }
    }

    private fun saveWeatherSnapshot(context: Context, snapshot: WeatherSnapshot, locationId: Long) {
        val sharedPreferences = context.getSharedPreferences("weather_change_$locationId", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putFloat("last_temp", snapshot.temperature.toFloat())
            .putFloat("last_wind", snapshot.windSpeed.toFloat())
            .putFloat("last_precip", snapshot.precipitation.toFloat())
            .putInt("last_weather_code", snapshot.weatherCode)
            .putString("last_weather_provider", snapshot.provider)
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
        
        if (last.weatherCode != current.weatherCode || last.provider != current.provider) {
            val lastDesc = WeatherCodes.getDescription(last.weatherCode, context, last.provider)
            val currentDesc = WeatherCodes.getDescription(current.weatherCode, context, current.provider)
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
