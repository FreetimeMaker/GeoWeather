package com.freetime.geoweather

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.R as SharedRes

class WeatherChangeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = DependencyManager.getRepository()
        val appSettings = DependencyManager.getAppSettings()
        val allLocations = repository.getAllLocationsSync()
        val connectivity = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivity.activeNetwork
        val capabilities = activeNetwork?.let(connectivity::getNetworkCapabilities)
        val onWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val refreshOfflinePacks = appSettings.offlinePacks.value &&
            !appSettings.dataSaver.value &&
            (!appSettings.offlinePacksWifiOnly.value || onWifi)
        val locations = allLocations.filter {
            it.changeAlertsEnabled || it.notificationsEnabled || (refreshOfflinePacks && it.offlinePackEnabled)
        }
        if (locations.isEmpty()) return Result.success()

        var retry = false
        locations.forEach { location ->
            try {
                val oldTemp = location.currentTemp
                val oldWind = location.currentWindSpeed
                val refreshStarted = System.currentTimeMillis()
                val updated = repository.refreshLocationWeather(location.id)
                currentCoroutineContext().ensureActive()
                val now = System.currentTimeMillis()
                if (updated == null || !RainAlertPolicy.isFreshRefresh(
                        location.lastUpdated, refreshStarted, updated.lastUpdated, now
                    )) {
                    retry = true
                    return@forEach
                }
                val newTemp = updated.currentTemp
                val newWind = updated.currentWindSpeed
                val tempThreshold = LocationAlertPreferences.tempThreshold(applicationContext, location.id, appSettings.tempThreshold.value)
                val windThreshold = LocationAlertPreferences.windThreshold(applicationContext, location.id, appSettings.windThreshold.value)
                val rainProbabilityThreshold = LocationAlertPreferences.rainProbability(applicationContext, location.id)
                val windGustThreshold = LocationAlertPreferences.windGustThreshold(applicationContext, location.id)
                val uvThreshold = LocationAlertPreferences.uvThreshold(applicationContext, location.id)
                val frostThreshold = LocationAlertPreferences.frostThreshold(applicationContext, location.id)

                if (location.changeAlertsEnabled) {
                    val tempChanged = oldTemp != null && newTemp != null &&
                        kotlin.math.abs(newTemp - oldTemp) >= tempThreshold
                    val windChanged = oldWind != null && newWind != null &&
                        kotlin.math.abs(newWind - oldWind) >= windThreshold
                    if (tempChanged || windChanged) {
                        val message = applicationContext.getString(
                            SharedRes.string.temperature_change_msg,
                            repository.getDisplayTemp(location, appSettings.tempUnit.value),
                            repository.getDisplayTemp(updated, appSettings.tempUnit.value)
                        )
                        WeatherNotifications.show(
                            applicationContext,
                            200000 + (location.id % 100000).toInt(),
                            applicationContext.getString(SharedRes.string.app_name),
                            message,
                            alert = true
                        )
                    }
                }

                if (location.notificationsEnabled) {
                    val hourly = repository.getHourlyForecasts(updated)
                    val severe = hourly.take(6).firstOrNull {
                        it.code in 95..99 || it.code in setOf(71, 73, 75, 77, 85, 86) ||
                            (appSettings.smartWindAlert.value && (it.windGusts ?: 0.0) >= windGustThreshold) ||
                            (appSettings.smartRainAlert.value && (it.precipitation ?: 0.0) >= 10.0)
                    }
                    if (severe != null) {
                        val prefs = applicationContext.getSharedPreferences("weather_alert_dedupe", Context.MODE_PRIVATE)
                        val alertKey = "severe_${location.id}"
                        val signature = "${severe.time}:${severe.code}:${severe.windGusts?.toInt()}:${severe.precipitation}"
                        if (prefs.getString(alertKey, null) != signature) {
                            val reason = when {
                                severe.code in 95..99 -> WeatherCodes.getDescription(severe.code)
                                severe.code in setOf(71, 73, 75, 77, 85, 86) -> WeatherCodes.getDescription(severe.code)
                                appSettings.smartWindAlert.value && (severe.windGusts ?: 0.0) >= windGustThreshold -> applicationContext.getString(SharedRes.string.strong_wind_gusts)
                                else -> applicationContext.getString(SharedRes.string.heavy_precipitation)
                            }
                            val message = applicationContext.getString(
                                SharedRes.string.extreme_weather_notification,
                                updated.name,
                                reason
                            )
                            WeatherNotifications.show(
                                applicationContext,
                                300000 + (location.id % 100000).toInt(),
                                applicationContext.getString(SharedRes.string.weather_alerts_title),
                                message,
                                alert = true
                            )
                            prefs.edit().putString(alertKey, signature).apply()
                        }
                    }

                    val rainPrefs = applicationContext.getSharedPreferences("weather_alert_dedupe", Context.MODE_PRIVATE)
                    val sentKey = "rain_sent_${location.id}"
                    val slotKey = "rain_slot_${location.id}"
                    val rainNow = System.currentTimeMillis()
                    val rain = if (appSettings.smartRainAlert.value) RainAlertPolicy.select(
                        slots = hourly.map { RainAlertPolicy.Slot(it.time, it.precipProbability, it.rain ?: it.precipitation ?: 0.0) },
                        timezone = repository.getWeatherTimeZoneId(updated),
                        now = rainNow,
                        fresh = RainAlertPolicy.isFreshRefresh(location.lastUpdated, refreshStarted, updated.lastUpdated, rainNow),
                        quietHours = appSettings.quietHours.value,
                        deviceHour = java.time.LocalTime.now().hour,
                        threshold = rainProbabilityThreshold,
                        lastSent = if (rainPrefs.contains(sentKey)) rainPrefs.getLong(sentKey, 0) else null,
                        lastSlot = if (rainPrefs.contains(slotKey)) rainPrefs.getLong(slotKey, 0) else null
                    ) else null
                    if (rain != null) {
                        val localStart = java.time.LocalDateTime.parse(rain.slot.time)
                        val hourFormat = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                        val message = applicationContext.getString(
                            SharedRes.string.rain_hourly_alert_message,
                            updated.name,
                            localStart.format(hourFormat),
                            localStart.plusHours(1).format(hourFormat),
                            rain.slot.probability
                        )
                        currentCoroutineContext().ensureActive()
                        if (WeatherNotifications.show(
                                applicationContext,
                                400000 + (location.id % 100000).toInt(),
                                applicationContext.getString(SharedRes.string.rain_hourly_alert_title),
                                message,
                                alert = true
                            )) {
                            rainPrefs.edit().putLong(sentKey, System.currentTimeMillis())
                                .putLong(slotKey, rain.startMillis).apply()
                        }
                    }

                    val smartPrefs = applicationContext.getSharedPreferences("weather_alert_dedupe", Context.MODE_PRIVATE)
                    val frost = if (appSettings.smartFrostAlert.value) hourly.take(12).firstOrNull { it.temp <= frostThreshold } else null
                    if (frost != null) {
                        val key = "frost_${location.id}"
                        val signature = frost.time + ":" + frost.temp
                        if (smartPrefs.getString(key, null) != signature) {
                            WeatherNotifications.show(
                                applicationContext,
                                500000 + (location.id % 100000).toInt(),
                                updated.name,
                                applicationContext.getString(
                                    SharedRes.string.frost_alert_notification,
                                    updated.name,
                                    frost.time,
                                    frost.temp
                                ),
                                alert = true
                            )
                            smartPrefs.edit().putString(key, signature).apply()
                        }
                    }
                    val highUv = if (appSettings.smartUvAlert.value) hourly.take(12).firstOrNull { (it.uvIndex ?: 0.0) >= uvThreshold } else null
                    if (highUv != null) {
                        val key = "uv_${location.id}"
                        val signature = highUv.time + ":" + highUv.uvIndex
                        if (smartPrefs.getString(key, null) != signature) {
                            WeatherNotifications.show(
                                applicationContext,
                                600000 + (location.id % 100000).toInt(),
                                updated.name,
                                applicationContext.getString(
                                    SharedRes.string.uv_alert_notification,
                                    updated.name,
                                    highUv.time,
                                    highUv.uvIndex?.toString() ?: "--"
                                ),
                                alert = true
                            )
                            smartPrefs.edit().putString(key, signature).apply()
                        }
                    }                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                retry = true
            }
        }
        return if (retry) Result.retry() else Result.success()
    }
}
