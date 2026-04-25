package com.freetime.geoweather

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.freetime.geoweather.data.MockLocationDao
import com.freetime.geoweather.network.WeatherApi
import com.freetime.geoweather.ui.App

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val locationDao = MockLocationDao()
    val api = WeatherApi()
    val settingsManager = SettingsManager()
    ComposeViewport("ComposeTarget") {
        App(locationDao, api, settingsManager, createLocationService())
    }
}
