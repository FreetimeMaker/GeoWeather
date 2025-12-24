package com.freetime.geoweather.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name ASC")
    LiveData<List<LocationEntity>> getAllLocations();

    @Insert
    void insertLocation(LocationEntity location);

    @Delete
    void deleteLocation(LocationEntity location);
}