package com.freetime.geoweather

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.freetime.geoweather.data.LocationEntity
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object WeatherNotificationScheduler {
    private const val PREFIX = "WeatherDailyNotification_"

    fun schedule(context: Context, location: LocationEntity) {
        if (!location.notificationsEnabled) {
            cancel(context, location.id)
            return
        }

        val time = runCatching { LocalTime.parse(location.notificationTime) }
            .getOrDefault(LocalTime.of(8, 0))
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = Duration.between(now, next).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putLong(WeatherNotificationWorker.KEY_LOCATION_ID, location.id)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName(location.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context, locationId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(locationId))
    }

    private fun workName(locationId: Long) = "$PREFIX$locationId"
}
