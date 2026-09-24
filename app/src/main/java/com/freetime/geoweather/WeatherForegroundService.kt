package com.freetime.geoweather

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.freetime.geoweather.data.DependencyManager
import kotlinx.coroutines.*

class WeatherForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        // Foreground services must publish their notification immediately after startup.
        startForeground(
            NOTIFICATION_ID,
            createNotification(getString(R.string.widget_loading))
        )
        startUpdateLoop()

        return START_STICKY
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                updateWeatherNotification()
                delay(30 * 60 * 1000)
            }
        }
    }

    private suspend fun updateWeatherNotification() {
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()
        val updatedLocation = repository.refreshSelectedLocationWeather()

        if (updatedLocation != null) {
            try {
                val base = repository.getNotificationContent(updatedLocation, appSettings.tempUnit.value)
                val hourly = repository.getHourlyForecasts(updatedLocation)
                val rain = hourly.take(6).maxOfOrNull { it.precipProbability } ?: 0
                val gust = hourly.take(6).maxOfOrNull { it.windGusts ?: it.windSpeed ?: 0.0 } ?: 0.0
                val windUnit = appSettings.windUnit.value
                val gustText = formatNotificationWind(gust, windUnit)
                val summary = getString(R.string.persistent_weather_summary, base, rain, gustText)
                val nextHour = hourly.firstOrNull()
                val content = if (nextHour != null) {
                    summary + "\n" + getString(
                        R.string.persistent_next_hour,
                        repository.getDisplayTemp(nextHour, appSettings.tempUnit.value),
                        nextHour.precipProbability
                    )
                } else summary
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification(content))
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to update persistent weather notification", e)
            }
        }
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.persistent_weather_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun formatNotificationWind(kmh: Double, unit: String): String = when (unit) {
        "mph" -> ((kmh * 0.621371).toInt()).toString() + " mph"
        "ms" -> String.format(java.util.Locale.getDefault(), "%.1f m/s", kmh / 3.6)
        else -> kmh.toInt().toString() + " km/h"
    }

    companion object {
        private const val CHANNEL_ID = "persistent_weather"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "WeatherForegroundSvc"
    }
}
