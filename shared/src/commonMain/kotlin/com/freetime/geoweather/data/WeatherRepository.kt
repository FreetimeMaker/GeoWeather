package com.freetime.geoweather.data

import com.freetime.geoweather.domain.City
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

class WeatherRepository(
    private val locationDao: LocationDao,
    private val historyDao: WeatherHistoryDao,
    private val apiClient: WeatherApiClient
) {
    fun getAllLocations(): Flow<List<LocationEntity>> = locationDao.getAllLocations()

    fun getHistoryForLocation(locationName: String): Flow<List<WeatherHistoryEntity>> =
        historyDao.getHistoryForLocation(locationName)
    
    suspend fun getSelectedLocation(): LocationEntity? = locationDao.getSelectedLocation()

    suspend fun searchCity(query: String) = apiClient.searchCity(query)

    suspend fun addLocation(city: City) {
        val entity = LocationEntity(
            name = city.name,
            latitude = city.latitude,
            longitude = city.longitude
        )
        locationDao.insertLocation(entity)
    }

    suspend fun addLocation(name: String, lat: Double, lon: Double) {
        val entity = LocationEntity(
            name = name,
            latitude = lat,
            longitude = lon
        )
        locationDao.insertLocation(entity)
    }

    suspend fun importLocations(locations: List<LocationEntity>) {
        locations.forEach { locationDao.insertLocation(it.copy(id = 0)) }
    }

    suspend fun updateWeather(location: LocationEntity, forecastDays: Int) {
        try {
            val url = apiClient.buildForecastUrl(location.latitude, location.longitude, forecastDays)
            val data = apiClient.get(url)
            val updated = location.copy(
                weatherData = data,
                lastUpdated = kotlin.time.Clock.System.now().toEpochMilliseconds()
            )
            locationDao.updateLocation(updated)
            
            // Add to history
            updated.currentTemp?.let { temp ->
                historyDao.insertHistory(
                    WeatherHistoryEntity(
                        location = updated.name,
                        temperature = temp
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun selectLocation(location: LocationEntity) {
        locationDao.deselectAllLocations()
        locationDao.updateLocation(location.copy(selected = true))
    }

    suspend fun deleteLocation(location: LocationEntity) {
        locationDao.deleteLocation(location)
    }
}
