package com.freetime.geoweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.LocationEntity

internal fun persistentWeatherLocation(locations: List<LocationEntity>): LocationEntity? =
    locations.firstOrNull { it.selected } ?: locations.firstOrNull()

object PersistentWeatherNotification {
    const val CHANNEL_ID = "persistent_weather"

    fun canPost(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.persistent_weather_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ))
        return NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun start(context: Context): Boolean {
        if (!canPost(context)) return false
        return try {
            ContextCompat.startForegroundService(context, Intent(context, WeatherForegroundService::class.java))
            true
        } catch (e: RuntimeException) {
            Log.e("PersistentWeather", "Could not start persistent weather service", e)
            false
        }
    }

    fun restoreIfEnabled(context: Context) {
        if (DependencyManager.getAppSettings().persistentNotif.value) start(context)
    }
}
