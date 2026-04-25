package com.freetime.geoweather

import com.freetime.geoweather.data.appContext

fun createLocationService(): LocationService = AndroidLocationService(appContext)
