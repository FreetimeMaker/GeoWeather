package com.freetime.geoweather.data

import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.domain.City
import geoweather.shared.generated.resources.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import org.jetbrains.compose.resources.getString

data class HourlyForecast(val time: String, val temp: Int, val code: Int)

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

    suspend fun selectLocation(location: LocationEntity) {
        locationDao.deselectAllLocations()
        locationDao.updateLocation(location.copy(selected = true))
    }

    suspend fun deleteLocation(location: LocationEntity) {
        locationDao.deleteLocation(location)
    }

    suspend fun toggleLocationNotifications(location: LocationEntity) {
        locationDao.updateLocation(location.copy(notificationsEnabled = !location.notificationsEnabled))
    }

    suspend fun toggleDefaultLocation(location: LocationEntity) {
        if (location.isDefault) {
            locationDao.updateLocation(location.copy(isDefault = false))
        } else {
            locationDao.clearDefaultLocation()
            locationDao.updateLocation(location.copy(isDefault = true))
        }
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

    fun getHourlyForecasts(location: LocationEntity): List<HourlyForecast> {
        val data = location.weatherData ?: return emptyList()
        return try {
            val json = Json.parseToJsonElement(data).jsonObject
            val hourly = json["hourly"]?.jsonObject ?: return emptyList()
            val times = hourly["time"]?.jsonArray ?: return emptyList()
            val temps = hourly["temperature_2m"]?.jsonArray ?: return emptyList()
            val codes = hourly["weathercode"]?.jsonArray ?: hourly["weather_code"]?.jsonArray ?: return emptyList()

            List(minOf(times.size, temps.size, codes.size, 24)) { i ->
                val timeStr = times[i].jsonPrimitive.content.split("T").last()
                val temp = temps[i].jsonPrimitive.doubleOrNull?.toInt() ?: 0
                val code = codes[i].jsonPrimitive.intOrNull ?: 0
                HourlyForecast(timeStr, temp, code)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
