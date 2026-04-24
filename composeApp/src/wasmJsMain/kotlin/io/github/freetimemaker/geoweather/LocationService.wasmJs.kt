package io.github.freetimemaker.geoweather

import io.github.freetimemaker.geoweather.data.LocationEntity

class WasmLocationService : LocationService {
    override suspend fun getCurrentLocation(): LocationEntity? {
        // In a real Wasm app, we would use window.navigator.geolocation
        // For now, let's return null to indicate it's not implemented yet
        return null
    }
}

actual fun createLocationService(): LocationService = WasmLocationService()
