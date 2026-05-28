package com.freetime.geoweather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.freetime.geoweather.data.LocationDao
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity

class LocationsViewModel(application: Application) : AndroidViewModel(application) {

    private val locationDao: LocationDao = LocationDatabase.getDatabase(application).locationDao()
    val locations: LiveData<List<LocationEntity>> = locationDao.getAllLocations()

    fun addLocation(name: String, latitude: Double, longitude: Double) {
        LocationDatabase.databaseWriteExecutor.execute {
            val newLocation = LocationEntity(name = name, latitude = latitude, longitude = longitude)
            locationDao.insertLocation(newLocation)
        }
    }

    fun deleteLocation(location: LocationEntity) {
        LocationDatabase.databaseWriteExecutor.execute {
            locationDao.deleteLocation(location)
        }
    }
}