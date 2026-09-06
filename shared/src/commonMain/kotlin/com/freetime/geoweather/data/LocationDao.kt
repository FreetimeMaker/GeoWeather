package com.freetime.geoweather.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY name ASC")
    suspend fun getAllLocationsSync(): List<LocationEntity>

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getCount(): Int

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun findById(id: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE latitude = :lat AND longitude = :lon")
    suspend fun findByCoordinates(lat: Double, lon: Double): LocationEntity?

    @Query("SELECT * FROM locations WHERE selected = 1 LIMIT 1")
    suspend fun getSelectedLocation(): LocationEntity?

    @Query("UPDATE locations SET selected = 0")
    suspend fun deselectAllLocations()

    @Query("SELECT * FROM locations WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultLocation(): LocationEntity?

    @Query("UPDATE locations SET isDefault = 0")
    suspend fun clearDefaultLocation()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)
}
