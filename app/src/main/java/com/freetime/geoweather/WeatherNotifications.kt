package com.freetime.geoweather

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object WeatherNotifications {
    const val CHANNEL_UPDATES = "weather_updates"
    const val CHANNEL_ALERTS = "weather_alerts"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_UPDATES, "Weather updates", NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ALERTS, "Weather alerts", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    fun show(context: Context, id: Int, title: String, message: String, alert: Boolean = false) {
        val settings = runCatching { com.freetime.geoweather.data.DependencyManager.getAppSettings() }.getOrNull()
        val hour = java.time.LocalTime.now().hour
        val quietNow = settings?.quietHours?.value == true && (hour >= 22 || hour < 7)
        if (quietNow) return
        val profile = settings?.notificationProfile?.value ?: "normal"
        if (!alert && profile == "outdoor") return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannels(context)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, if (alert) CHANNEL_ALERTS else CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (alert) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
