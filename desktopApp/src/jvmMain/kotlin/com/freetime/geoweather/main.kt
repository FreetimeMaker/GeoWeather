package com.freetime.geoweather

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.getRoomDatabase
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

fun main() = application {
    val database = getRoomDatabase(getDatabaseBuilder())
    val prefs = Preferences.userRoot().node("com.freetime.geoweather")
    val settings = PreferencesSettings(prefs)
    DependencyManager.initialize(database, settings)
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "GeoWeather",
    ) {
        WeatherApp(
            database = DependencyManager.getDatabase(),
            appSettings = DependencyManager.getAppSettings()
        )
    }
}
