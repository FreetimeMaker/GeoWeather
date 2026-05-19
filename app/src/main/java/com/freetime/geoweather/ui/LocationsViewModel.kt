package com.freetime.geoweather.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.GeocodingRepository
import com.freetime.geoweather.AuthManager
import com.freetime.geoweather.WeatherRepository
import com.freetime.geoweather.api.LocationSyncRequest
import com.freetime.geoweather.data.LocationDao
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocationsViewModel(application: Application) : AndroidViewModel(application) {

    private val locationDao: LocationDao = LocationDatabase.getDatabase(application).locationDao()
    private val appContext = application
    private val geocodingRepository = GeocodingRepository()
    private val authManager = AuthManager.getInstance(application)
    private val weatherRepository = WeatherRepository(application)

    val locations: LiveData<List<LocationEntity>> = locationDao.getAllLocations()

    fun refreshAllWeathers() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentLocations = locationDao.getAllLocationsSync()
            currentLocations.forEach { loc ->
                val result = weatherRepository.getWeatherData(loc.latitude, loc.longitude, 1, appContext)
                if (result is WeatherRepository.WeatherDataResult.Success) {
                    locationDao.updateLocation(loc.copy(
                        weatherData = result.rawJson,
                        lastUpdated = System.currentTimeMillis()
                    ))
                }
            }
        }
    }

    fun addLocation(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            locationDao.deselectAllLocations()
            val newLocation = LocationEntity(name = name, latitude = latitude, longitude = longitude, selected = true)
            locationDao.insertLocation(newLocation)
            syncWithCloud()
            refreshAllWeathers()
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
            try {
                val currentLocations = locationDao.getAllLocationsSync()
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
                
                // Direct upsert to Supabase
                supabase.from("locations").upsert(syncList)
            } catch (e: Exception) {
                // Log sync error
            }
        }
    }

    suspend fun search(query: String): List<Triple<String, Double, Double>> {
        return withContext(Dispatchers.IO) {
            geocodingRepository.searchLocations(getApplication(), query)
        }
    }
}
