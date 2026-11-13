package com.freetime.geoweather

import android.app.Application

import android.util.Log
import com.freetime.notifications.FreetimeNotifications

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.work.WorkManager
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.getRoomDatabase
import com.freetime.geoweather.data.LocationBackupStore
import com.russhwolf.settings.SharedPreferencesSettings
import java.util.concurrent.TimeUnit

class GeoWeatherApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationsInitialization: Deferred<Unit>
    private val notificationsInitializationMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        setAndroidContext(this)
        initDependencies()
        initializeFreetimeNotifications()

        scheduleWeatherWork()
    }

    private fun initializeFreetimeNotifications() {
        notificationsInitialization = applicationScope.async {
            try {
                FreetimeNotifications.initialize(
                    this@GeoWeatherApp,
                    freetimeNotificationConfiguration(
                        smallIcon = R.drawable.ic_notification,
                        updatesChannelName = getString(R.string.freetime_notifications_updates),
                        changelogsChannelName = getString(R.string.freetime_notifications_changelogs),
                        developerMessagesChannelName = getString(R.string.freetime_notifications_developer_messages)
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("GeoWeatherApp", "Failed to initialize Freetime notifications", e)
                throw e
            }
        }
    }

    suspend fun awaitNotificationsInitialized() {
        notificationsInitializationMutex.withLock {
            if (notificationsInitialization.isCancelled) initializeFreetimeNotifications()
            notificationsInitialization.await()
        }
    }

    private fun initDependencies() {
        val database = getRoomDatabase(getDatabaseBuilder(this))
        val sharedPrefs = getSharedPreferences("geo_weather_prefs", MODE_PRIVATE)
        val settings = SharedPreferencesSettings(sharedPrefs)
        DependencyManager.initialize(this, database, settings)
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
        applicationScope.launch {
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
