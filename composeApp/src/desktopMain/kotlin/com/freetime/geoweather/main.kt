package com.freetime.geoweather

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.freetime.geoweather.data.MockLocationDao
import com.freetime.geoweather.network.WeatherApi
import androidx.compose.ui.res.painterResource
import com.freetime.geoweather.ui.App

fun main() = application {
    val locationDao = MockLocationDao()
    val api = WeatherApi()
    val settingsManager = SettingsManager()
    val locationService = createLocationService()
    Window(
        onCloseRequest = ::exitApplication,
        title = "GeoWeather",
        icon = painterResource("icon.webp")
    ) {
        App(locationDao, api, settingsManager, locationService)
    }
}
