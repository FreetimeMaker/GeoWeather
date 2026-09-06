package com.freetime.geoweather

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.res.painterResource
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.getRoomDatabase

fun main() = application {
    val database = getRoomDatabase(getDatabaseBuilder())

    Window(
        onCloseRequest = ::exitApplication,
        title = "GeoWeather",
        icon = painterResource("icon.png")
    ) {
        WeatherApp(database)
    }
}
