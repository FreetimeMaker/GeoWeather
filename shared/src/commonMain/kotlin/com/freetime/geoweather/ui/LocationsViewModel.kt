package com.freetime.geoweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.data.LocationDao
import com.freetime.geoweather.data.LocationEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationsViewModel(private val locationDao: LocationDao) : ViewModel() {

    val locations: StateFlow<List<LocationEntity>> = locationDao.getAllLocationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val newLocation = LocationEntity(name = name, latitude = latitude, longitude = longitude)
            locationDao.insertLocation(newLocation)
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch {
            locationDao.deleteLocation(location)
        }
    }
}
