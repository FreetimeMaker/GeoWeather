package com.freetime.geoweather

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.freetime.geoweather.data.DependencyManager
import geoweather.shared.generated.resources.*
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.getString

class WeatherForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        serviceScope.launch {
            val loadingMsg = getString(Res.string.widget_loading)
            startForeground(NOTIFICATION_ID, createNotification(loadingMsg))
            startUpdateLoop()
        }
        
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
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()
        
        val updatedLocation = repository.refreshSelectedLocationWeather()
        
        if (updatedLocation != null) {
            try {
                val content = repository.getNotificationContent(updatedLocation, appSettings.tempUnit.value)
                
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification(content))
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    private suspend fun createNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val appName = getString(Res.string.app_name)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(appName)
            .setContentText(content)
            .setSmallIcon(R.mipmap.icon)
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