package io.github.freetimemaker.geoweather

import io.github.freetimemaker.geoweather.data.LocationEntity

interface LocationService {
    suspend fun getCurrentLocation(): LocationEntity?
}

expect fun createLocationService(): LocationService
