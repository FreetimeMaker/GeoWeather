package com.freetime.geoweather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.GeocodingRepository
import com.freetime.geoweather.AuthManager
import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.api.ApiClient
import com.freetime.geoweather.api.LocationSyncRequest
import com.freetime.geoweather.data.LocationDao
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationsViewModel(application: Application) : AndroidViewModel(application) {

    private val locationDao: LocationDao = LocationDatabase.getDatabase(application).locationDao()
    private val geocodingRepository = GeocodingRepository()
    private val authManager = AuthManager.getInstance(application)
    private val apiClient = ApiClient(ApiConstants.BASE_URL)
    
    val locations: LiveData<List<LocationEntity>> = locationDao.getAllLocations()

    fun addLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.deselectAllLocations()
            val newLocation = LocationEntity(name = name, latitude = latitude, longitude = longitude, selected = true)
            locationDao.insertLocation(newLocation)
            syncWithCloud()
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.deleteLocation(location)
            syncWithCloud()
        }
    }

    fun setDefaultLocation(location: LocationEntity, isDefault: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isDefault) {
                locationDao.clearDefaultLocation()
            }
            locationDao.updateLocation(location.copy(isDefault = isDefault))
            syncWithCloud()
        }
    }

    fun selectLocation(location: LocationEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.deselectAllLocations()
            locationDao.updateLocation(location.copy(selected = true))
            syncWithCloud()
        }
    }

    fun syncWithCloud() {
        if (!authManager.isAuthenticated) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentLocations = locationDao.getAllLocationsSync()
            val token = authManager.getAccessToken()
            
            val syncList = currentLocations.map { 
                LocationSyncRequest(
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    notificationsEnabled = it.notificationsEnabled,
                    notificationTime = it.notificationTime,
                    changeAlertsEnabled = it.changeAlertsEnabled,
                    changeAlertInterval = it.changeAlertInterval,
                    isDefault = it.isDefault
                )
            }
            
            apiClient.syncLocations(syncList, token)
        }
    }

    suspend fun search(query: String): List<Triple<String, Double, Double>> {
        return withContext(Dispatchers.IO) {
            geocodingRepository.searchLocations(query)
        }
    }
}