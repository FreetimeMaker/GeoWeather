package com.freetime.geoweather.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.freetime.geoweather.data.LocationDao;
import com.freetime.geoweather.data.LocationDatabase;
import com.freetime.geoweather.data.LocationEntity;

import java.util.List;

public class LocationsViewModel extends AndroidViewModel {

    private final LocationDao locationDao;
    public final LiveData<List<LocationEntity>> locations;

    public LocationsViewModel(Application application) {
        super(application);
        LocationDatabase db = LocationDatabase.getDatabase(application);
        locationDao = db.locationDao();
        locations = locationDao.getAllLocations();
    }

    public void addLocation(String name, double latitude, double longitude) {
        LocationDatabase.databaseWriteExecutor.execute(() -> {
            LocationEntity newLocation = new LocationEntity(name, latitude, longitude);
            locationDao.insertLocation(newLocation);
        });
    }

    public void deleteLocation(LocationEntity location) {
        LocationDatabase.databaseWriteExecutor.execute(() -> {
            locationDao.deleteLocation(location);
        });
    }
}