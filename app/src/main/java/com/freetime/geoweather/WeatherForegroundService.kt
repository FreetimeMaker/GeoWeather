package com.freetime.geoweather

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.LocationEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class WeatherForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null
    private var notificationJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!DependencyManager.getAppSettings().persistentNotif.value ||
            !PersistentWeatherNotification.canPost(this)
        ) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        // Publish immediately; neither cached data nor a network request may delay foreground promotion.
        startForeground(NOTIFICATION_ID, createNotification(getString(R.string.widget_loading)))
        startUpdateLoop()
        return START_STICKY
    }

    private fun startUpdateLoop() {
        val repository = DependencyManager.getRepository()
        val settings = DependencyManager.getAppSettings()
        if (notificationJob?.isActive != true) {
            notificationJob = serviceScope.launch {
                combine(repository.getAllLocations(), settings.tempUnit, settings.windUnit) { locations, temp, wind ->
                    Triple(persistentWeatherLocation(locations), temp, wind)
                }.collect { (location, tempUnit, windUnit) ->
                    try {
                        publishNotification(notificationContent(location, tempUnit, windUnit))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to render persistent weather notification", e)
                    }
                }
            }
        }
        if (updateJob?.isActive == true) return
        updateJob = serviceScope.launch {
            while (isActive) {
                try {
                    // A saved location need not have been opened/selected before enabling this feature.
                    val location = persistentWeatherLocation(repository.getAllLocations().first())
                    if (location != null) repository.refreshLocationWeather(location.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh persistent weather; retaining cached content", e)
                }
                delay(30 * 60 * 1000L)
            }
        }
    }

    private suspend fun notificationContent(location: LocationEntity?, tempUnit: String, windUnit: String): String {
        if (location == null) return getString(R.string.persistent_notification_no_location)
        if (location.weatherData == null) return location.name + " · " + getString(R.string.error_loading_weather)
        val repository = DependencyManager.getRepository()
        val base = repository.getNotificationContent(location, tempUnit)
        val hourly = repository.getHourlyForecasts(location)
        val rain = hourly.take(6).maxOfOrNull { it.precipProbability } ?: 0
        val gust = hourly.take(6).maxOfOrNull { it.windGusts ?: it.windSpeed ?: 0.0 } ?: 0.0
        val summary = getString(R.string.persistent_weather_summary, base, rain, formatNotificationWind(gust, windUnit))
        val nextHour = hourly.firstOrNull() ?: return summary
        return summary + "\n" + getString(
            R.string.persistent_next_hour,
            formatNotificationTemp(nextHour.temp.toDouble(), tempUnit),
            nextHour.precipProbability
        )
    }

    private fun publishNotification(content: String) {
        if (PersistentWeatherNotification.canPost(this)) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, createNotification(content))
        } else {
            stopSelf()
        }
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, PersistentWeatherNotification.CHANNEL_ID)
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

    override fun onDestroy() {
        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun formatNotificationTemp(celsius: Double, unit: String): String =
        if (unit == "fahrenheit") ((celsius * 9.0 / 5.0) + 32.0).toInt().toString() + "°F"
        else celsius.toInt().toString() + "°C"

    private fun formatNotificationWind(kmh: Double, unit: String): String = when (unit) {
        "mph" -> ((kmh * 0.621371).toInt()).toString() + " mph"
        "ms" -> String.format(java.util.Locale.getDefault(), "%.1f m/s", kmh / 3.6)
        else -> kmh.toInt().toString() + " km/h"
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "WeatherForegroundSvc"
    }
}
