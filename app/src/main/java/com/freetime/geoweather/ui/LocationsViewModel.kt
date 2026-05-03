package com.freetime.geoweather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.GeocodingRepository
import com.freetime.geoweather.data.LocationDao
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationsViewModel(application: Application) : AndroidViewModel(application) {

    private val locationDao: LocationDao = LocationDatabase.getDatabase(application).locationDao()
    private val geocodingRepository = GeocodingRepository()
    
    val locations: LiveData<List<LocationEntity>> = locationDao.getAllLocations()

    fun addLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.deselectAllLocations()
            val newLocation = LocationEntity(name = name, latitude = latitude, longitude = longitude, selected = true)
            locationDao.insertLocation(newLocation)
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.deleteLocation(location)
        }
    }

    fun setDefaultLocation(location: LocationEntity, isDefault: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isDefault) {
                locationDao.clearDefaultLocation()
            }
            locationDao.updateLocation(location.copy(isDefault = isDefault))
        }
    }

    fun selectLocation(location: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.deselectAllLocations()
            locationDao.updateLocation(location.copy(selected = true))
        }
    }

    suspend fun search(query: String): List<Triple<String, Double, Double>> {
        return withContext(Dispatchers.IO) {
            geocodingRepository.searchLocations(query)
        }
    }
}