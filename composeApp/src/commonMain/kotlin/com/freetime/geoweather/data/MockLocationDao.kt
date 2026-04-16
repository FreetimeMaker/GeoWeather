package com.freetime.geoweather.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockLocationDao : LocationDao {
    private val _locations = MutableStateFlow<List<LocationEntity>>(
        listOf(
            LocationEntity(id = 1, name = "London", latitude = 51.5074, longitude = -0.1278, selected = true),
            LocationEntity(id = 2, name = "New York", latitude = 40.7128, longitude = -74.0060)
        )
    )

    override fun getAllLocations(): Flow<List<LocationEntity>> = _locations

    override suspend fun getAllLocationsSync(): List<LocationEntity> = _locations.value

    override suspend fun getCount(): Int = _locations.value.size

    override suspend fun findByCoordinates(lat: Double, lon: Double): LocationEntity? =
        _locations.value.find { it.latitude == lat && it.longitude == lon }

    override suspend fun getSelectedLocation(): LocationEntity? =
        _locations.value.find { it.selected }

    override suspend fun deselectAllLocations() {
        _locations.value = _locations.value.map { it.copy(selected = false) }
    }

    override suspend fun getDefaultLocation(): LocationEntity? =
        _locations.value.find { it.isDefault }

    override suspend fun clearDefaultLocation() {
        _locations.value = _locations.value.map { it.copy(isDefault = false) }
    }

    override suspend fun insertLocation(location: LocationEntity) {
        _locations.value = _locations.value + location.copy(id = (_locations.value.maxOfOrNull { it.id } ?: 0) + 1)
    }

    override suspend fun updateLocation(location: LocationEntity) {
        _locations.value = _locations.value.map { if (it.id == location.id) location else it }
    }

    override suspend fun deleteLocation(location: LocationEntity) {
        _locations.value = _locations.value.filter { it.id != location.id }
    }
}
