package io.github.freetimemaker.geoweather

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.freetimemaker.geoweather.data.MockLocationDao
import io.github.freetimemaker.geoweather.network.WeatherApi
import io.github.freetimemaker.geoweather.ui.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val locationDao = MockLocationDao()
    val api = WeatherApi()
    val settingsManager = SettingsManager()
    ComposeViewport("ComposeTarget") {
        App(locationDao, api, settingsManager, createLocationService())
    }
}
