package com.freetime.geoweather.data

import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.domain.City
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.getString

class WeatherRepository(
    private val locationDao: LocationDao,
    private val historyDao: WeatherHistoryDao,
    private val apiClient: WeatherApiClient
) {
    fun getAllLocations(): Flow<List<LocationEntity>> = locationDao.getAllLocationsFlow()
    
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

    suspend fun updateWeather(location: LocationEntity, providerUrl: String) {
        try {
            val data = apiClient.get(providerUrl)
            val updated = location.copy(
                weatherData = data,
                lastUpdated = Clock.System.now().toEpochMilliseconds()
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
            throw e
        }
    }

    suspend fun refreshSelectedLocationWeather(includeHourly: Boolean = false): LocationEntity? {
        val location = getSelectedLocation() ?: return null
        val hourlyParam = if (includeHourly) "&hourly=temperature_2m,weather_code" else ""
        val url = ApiConstants.OPEN_METEO_FORECAST + 
            "?latitude=${location.latitude}&longitude=${location.longitude}&current_weather=true$hourlyParam&timezone=auto"
        
        updateWeather(location, url)
        return getSelectedLocation()
    }

    fun getDisplayTemp(location: LocationEntity, tempUnit: String): String {
        val temp = location.currentTemp ?: return "--"
        val displayTemp = if (tempUnit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()
        val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
        return "$displayTemp$tempSuffix"
    }

    suspend fun getNotificationContent(location: LocationEntity, tempUnit: String): String {
        val tempStr = getDisplayTemp(location, tempUnit)
        val code = location.currentWeatherCode ?: 0
        val description = WeatherCodes.getDescription(code)
        return getString(Res.string.WeatherNotificationTXT, location.name, tempStr, description)
    }
}
