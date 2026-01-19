package com.freetime.geoweather.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name ASC")
    LiveData<List<LocationEntity>> getAllLocations();

    @Query("SELECT COUNT(*) FROM locations")
    int getCount();

    @Query("SELECT * FROM locations WHERE latitude = :lat AND longitude = :lon LIMIT 1")
    LocationEntity findByCoordinates(double lat, double lon);

    @Insert
    void insertLocation(LocationEntity location);

    @Update
    void updateLocation(LocationEntity location);

    @Delete
    void deleteLocation(LocationEntity location);
}
