package com.freetime.geoweather

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.getRoomDatabase
import com.russhwolf.settings.SharedPreferencesSettings
import java.util.concurrent.TimeUnit

class GeoWeatherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setAndroidContext(this)
        initDependencies()
        scheduleWeatherWork()
    }

    private fun initDependencies() {
        val database = getRoomDatabase(getDatabaseBuilder(this))
        val sharedPrefs = getSharedPreferences("geo_weather_prefs", MODE_PRIVATE)
        val settings = SharedPreferencesSettings(sharedPrefs)
        DependencyManager.initialize(database, settings)
    }

    private fun scheduleWeatherWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val dailyWorkRequest =
            PeriodicWorkRequestBuilder<WeatherNotificationWorker>(
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
