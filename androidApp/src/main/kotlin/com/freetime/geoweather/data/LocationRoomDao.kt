package com.freetime.geoweather.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationRoomDao {
    @Query("SELECT * FROM locations ORDER BY id")
    fun getAllLocationsFlow(): Flow<List<LocationRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationRoomEntity)

    @Delete
    suspend fun deleteLocation(location: LocationRoomEntity)

    @Update
    suspend fun updateLocation(location: LocationRoomEntity)

    @Query("UPDATE locations SET selected = 0")
    suspend fun deselectAllLocations()

    @Query("UPDATE locations SET isDefault = 0")
    suspend fun clearDefaultLocation()

    @Query("SELECT * FROM locations WHERE selected = 1 LIMIT 1")
    suspend fun getSelectedLocation(): LocationRoomEntity?
}
