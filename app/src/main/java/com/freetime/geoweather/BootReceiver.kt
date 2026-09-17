package com.freetime.geoweather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.freetime.geoweather.data.DependencyManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val appSettings = DependencyManager.getAppSettings()
            if (appSettings.persistentNotif.value) {
                val serviceIntent = Intent(context, WeatherForegroundService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
