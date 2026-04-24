package io.github.freetimemaker.geoweather

import io.github.freetimemaker.geoweather.data.appContext

fun createLocationService(): LocationService = AndroidLocationService(appContext)
