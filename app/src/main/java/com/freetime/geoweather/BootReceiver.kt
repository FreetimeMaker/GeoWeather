package com.freetime.geoweather

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.freetime.geoweather.data.DependencyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appSettings = DependencyManager.getAppSettings()
                if (appSettings.persistentNotif.value) {
                    val serviceIntent = Intent(context, WeatherForegroundService::class.java)
                    context.startForegroundService(serviceIntent)
                }

                WeatherWidget().updateAll(context)
                CompactWeatherWidget().updateAll(context)
                ForecastWeatherWidget().updateAll(context)
                DetailedWeatherWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
