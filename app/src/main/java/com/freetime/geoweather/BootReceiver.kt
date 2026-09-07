package com.freetime.geoweather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPreferences = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
            val persistentNotif = sharedPreferences.getBoolean("persistent_notif", false)
            if (persistentNotif) {
                val serviceIntent = Intent(context, WeatherForegroundService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
