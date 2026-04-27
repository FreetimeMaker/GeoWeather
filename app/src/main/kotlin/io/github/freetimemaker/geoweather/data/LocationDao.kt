package io.github.freetimemaker.geoweather.data

import kotlinx.coroutines.flow.Flow

interface LocationDao {
    fun getAllLocations(): Flow<List<LocationEntity>>
    suspend fun insertLocation(location: LocationEntity)
    suspend fun deleteLocation(location: LocationEntity)
    suspend fun updateLocation(location: LocationEntity)
    suspend fun deselectAllLocations()
    suspend fun clearDefaultLocation()
    suspend fun getSelectedLocation(): LocationEntity?
}
