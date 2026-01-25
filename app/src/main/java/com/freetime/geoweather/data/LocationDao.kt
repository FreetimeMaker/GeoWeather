package com.freetime.geoweather.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name ASC")
    fun getAllLocations(): LiveData<List<LocationEntity>>

    @Query("SELECT COUNT(*) FROM locations")
    fun getCount(): Int

    @Query("SELECT * FROM locations WHERE latitude = :lat AND longitude = :lon LIMIT 1")
    fun findByCoordinates(lat: Double, lon: Double): LocationEntity?

    @Insert
    fun insertLocation(location: LocationEntity)

    @Update
    fun updateLocation(location: LocationEntity)

    @Delete
    fun deleteLocation(location: LocationEntity)
}
