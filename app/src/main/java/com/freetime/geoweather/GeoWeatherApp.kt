package com.freetime.geoweather

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class GeoWeatherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleWeatherWork()
    }

    private fun scheduleWeatherWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyWorkRequest = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(constraints).build()

        val changeWorkRequest = PeriodicWorkRequestBuilder<WeatherChangeWorker>(
            3, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherDailyNotification",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherChangeAlerts",
            ExistingPeriodicWorkPolicy.KEEP,
            changeWorkRequest
        )
    }
}
