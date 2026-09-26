package com.freetime.geoweather

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.work.WorkManager
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.getRoomDatabase
import com.freetime.geoweather.data.LocationBackupStore
import com.russhwolf.settings.SharedPreferencesSettings
import java.util.concurrent.TimeUnit

class GeoWeatherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setAndroidContext(this)
        initDependencies()
        restorePersistentWeatherNotification()
        scheduleWeatherWork()
    }

    private fun initDependencies() {
        val database = getRoomDatabase(getDatabaseBuilder(this))
        val sharedPrefs = getSharedPreferences("geo_weather_prefs", MODE_PRIVATE)
        val settings = SharedPreferencesSettings(sharedPrefs)
        DependencyManager.initialize(database, settings)
    }

    private fun restorePersistentWeatherNotification() {
        if (DependencyManager.getAppSettings().persistentNotif.value) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, WeatherForegroundService::class.java)
            )
        }
    }

    private fun scheduleWeatherWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val changeWorkRequest = PeriodicWorkRequestBuilder<WeatherChangeWorker>(
            1, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        // Remove the old global worker. Daily notifications are scheduled per location.
        WorkManager.getInstance(this).cancelUniqueWork("WeatherDailyNotification")
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val database = DependencyManager.getDatabase()
            LocationBackupStore.restoreIfNeeded(this@GeoWeatherApp, database.locationDao())
            LocationBackupStore.sync(this@GeoWeatherApp, database.locationDao())
            DependencyManager.getRepository().getAllLocationsSync()
                .filter { it.notificationsEnabled }
                .forEach { WeatherNotificationScheduler.schedule(this@GeoWeatherApp, it) }
        }

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeatherChangeAlerts",
            ExistingPeriodicWorkPolicy.KEEP,
            changeWorkRequest
        )
    }
}
