package com.freetime.geoweather

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.freetime.geoweather.data.MockLocationDao
import com.freetime.geoweather.network.WeatherApi
import com.freetime.geoweather.ui.App
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val locationDao = MockLocationDao()
    val api = WeatherApi()
    val settingsManager = SettingsManager()
    ComposeViewport(document.body!!) {
        App(locationDao, api, settingsManager)
    }
}
