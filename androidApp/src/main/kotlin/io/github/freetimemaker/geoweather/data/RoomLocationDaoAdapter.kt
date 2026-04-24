package io.github.freetimemaker.geoweather.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomLocationDaoAdapter(
    private val dao: LocationRoomDao
) : io.github.freetimemaker.geoweather.data.LocationDao {

    override fun getAllLocations(): Flow<List<LocationEntity>> {
        return dao.getAllLocationsFlow().map { list ->
            list.map { it.toLocationEntity() }
        }
    }

    override suspend fun insertLocation(location: LocationEntity) {
        dao.insertLocation(location.toRoomEntity())
    }

    override suspend fun deleteLocation(location: LocationEntity) {
        dao.deleteLocation(location.toRoomEntity())
    }

    override suspend fun updateLocation(location: LocationEntity) {
        dao.updateLocation(location.toRoomEntity())
    }

    override suspend fun deselectAllLocations() {
        dao.deselectAllLocations()
    }

    override suspend fun clearDefaultLocation() {
        dao.clearDefaultLocation()
    }

    override suspend fun getSelectedLocation(): LocationEntity? {
        return dao.getSelectedLocation()?.toLocationEntity()
    }
}

private fun LocationEntity.toRoomEntity() = LocationRoomEntity(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    selected = selected,
    isDefault = isDefault
)
