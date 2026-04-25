package com.freetime.geoweather

import com.freetime.geoweather.data.LocationEntity

interface LocationService {
    suspend fun getCurrentLocation(): LocationEntity?
}

expect fun createLocationService(): LocationService
