package com.freetime.geoweather.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockLocationDao : LocationDao {
    private val _locations = MutableStateFlow<List<LocationEntity>>(emptyList())

    override fun getAllLocations(): Flow<List<LocationEntity>> = _locations

    override suspend fun insertLocation(location: LocationEntity) {
        val current = _locations.value
        val nextId = if (location.id != 0L) location.id else (current.maxOfOrNull { it.id } ?: 0L) + 1L
        _locations.value = current + location.copy(id = nextId)
    }

    override suspend fun deleteLocation(location: LocationEntity) {
        _locations.value = _locations.value.filterNot { it.id != 0L && it.id == location.id }
    }

    override suspend fun updateLocation(location: LocationEntity) {
        _locations.value = _locations.value.map {
            if (it.id != 0L && it.id == location.id) location else it
        }
    }

    override suspend fun deselectAllLocations() {
        _locations.value = _locations.value.map { it.copy(selected = false) }
    }

    override suspend fun clearDefaultLocation() {
        _locations.value = _locations.value.map { it.copy(isDefault = false) }
    }

    override suspend fun getSelectedLocation(): LocationEntity? {
        return _locations.value.firstOrNull { it.selected }
    }
}
