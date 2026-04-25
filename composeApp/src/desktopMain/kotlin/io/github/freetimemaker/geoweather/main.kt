package io.github.freetimemaker.geoweather

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.freetimemaker.geoweather.data.MockLocationDao
import io.github.freetimemaker.geoweather.network.WeatherApi
import androidx.compose.ui.res.painterResource
import io.github.freetimemaker.geoweather.ui.App

fun main(args: Array<String>) = application {
    println("Starting GeoWeather Desktop...")
    val locationDao = MockLocationDao()
    val api = WeatherApi()
    val settingsManager = SettingsManager()
    val locationService = createLocationService()
    
    println("Initializing Window...")
    Window(
        onCloseRequest = ::exitApplication,
        title = "GeoWeather"
    ) {
        App(locationDao, api, settingsManager, locationService)
    }
}
