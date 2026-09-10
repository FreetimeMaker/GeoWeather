package com.freetime.geoweather

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.getRoomDatabase
import com.russhwolf.settings.PreferencesSettings
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.prefs.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

fun main() = application {
    val database = remember { getRoomDatabase(getDatabaseBuilder()) }
    val prefs = remember { Preferences.userRoot().node("com.freetime.geoweather") }
    val settings = remember { PreferencesSettings(prefs) }
    val initialized = remember {
        DependencyManager.initialize(database, settings)
        true
    }
    val trayState = rememberTrayState()
    val repository = remember(initialized) { DependencyManager.getRepository() }
    val appSettings = remember(initialized) { DependencyManager.getAppSettings() }
    val notifiedKeys = remember { mutableSetOf<String>() }

    // The tray is only used as the native desktop notification host. GeoWeather's
    // Android-only persistent weather notification is intentionally not mirrored here.
    Tray(
        state = trayState,
        icon = painterResource("icon.png"),
        tooltip = "GeoWeather"
    )

    LaunchedEffect(repository, appSettings) {
        while (true) {
            runCatching {
                val now = LocalTime.now()
                val minuteKey = now.format(DateTimeFormatter.ofPattern("HH:mm"))
                val todayKey = java.time.LocalDate.now().toString()
                val locations = repository.getAllLocations().first()

                locations
                    .asSequence()
                    .filter(LocationEntity::notificationsEnabled)
                    .filter { it.notificationTime.matchesMinute(now) }
                    .forEach { location ->
                        val notificationKey = "$todayKey:${location.id}:$minuteKey"
                        if (notifiedKeys.add(notificationKey)) {
                            val updatedLocation = repository.refreshLocationWeather(location.id) ?: location
                            val content = repository.getNotificationContent(
                                updatedLocation,
                                appSettings.tempUnit.value
                            )
                            trayState.sendNotification(
                                Notification(
                                    title = "GeoWeather",
                                    message = content,
                                    type = Notification.Type.Info
                                )
                            )
                        }
                    }

                notifiedKeys.removeAll { !it.startsWith("$todayKey:") }
            }

            delay(30_000)
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "GeoWeather",
        icon = painterResource("icon.png")
    ) {
        WeatherApp(
            database = database,
            appSettings = appSettings
        )
    }
}

private fun String.matchesMinute(now: LocalTime): Boolean {
    return try {
        val configured = LocalTime.parse(this, DateTimeFormatter.ofPattern("HH:mm"))
        configured.hour == now.hour && configured.minute == now.minute
    } catch (_: DateTimeParseException) {
        false
    }
}
