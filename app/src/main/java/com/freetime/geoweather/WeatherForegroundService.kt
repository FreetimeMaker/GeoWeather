package com.freetime.geoweather

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.freetime.geoweather.data.LocationDatabase
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

class WeatherForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Loading..."))
        
        startUpdateLoop()
        
        return START_STICKY
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                updateWeatherNotification()
                delay(30 * 60 * 1000) // Update every 30 minutes
            }
        }
    }

    private suspend fun updateWeatherNotification() {
        val db = LocationDatabase.getDatabase(this)
        val location = db.locationDao().getSelectedLocation() ?: db.locationDao().getAllLocationsSync().firstOrNull()
        
        if (location != null) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current_weather=true&timezone=auto"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val current = json.getJSONObject("current_weather")
                val temp = current.getDouble("temperature")
                val code = if (current.has("weather_code")) current.getInt("weather_code") else current.getInt("weathercode")
                
                val sharedPreferences = getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
                val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"
                val displayTemp = if (tempUnit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()
                val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                
                val description = WeatherCodes.getDescription(code, this)
                val content = "${location.name}: $displayTemp$tempSuffix, $description"
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification(content))
            } catch (e: Exception) {
                // Ignore errors for now
            }
        }
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GeoWeather")
            .setContentText(content)
            .setSmallIcon(R.mipmap.icon)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Persistent Weather",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "persistent_weather"
        private const val NOTIFICATION_ID = 1001
    }
}
