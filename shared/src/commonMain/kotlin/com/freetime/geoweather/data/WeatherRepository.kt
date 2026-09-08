package com.freetime.geoweather.data

import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.domain.City
import geoweather.shared.generated.resources.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.*
import org.jetbrains.compose.resources.getString

data class HourlyForecast(val time: String, val temp: Int, val code: Int)

data class DailyForecast(
    val date: String,
    val code: Int,
    val maxTemp: Int,
    val minTemp: Int,
    val sunrise: String = "--",
    val sunset: String = "--",
    val precipSum: Double = 0.0,
    val precipProbMax: Int = 0,
    val windMax: Double = 0.0
)

/** Values taken from the hourly arrays at the current hour. */
data class CurrentHourExtras(
    val visibilityKm: Double?,
    val cloudBaseM: Double?,
    /** -1 = falling, 0 = stable, +1 = rising */
    val pressureTrend: Int
)

class WeatherRepository(
    private val locationDao: LocationDao,
    private val historyDao: WeatherHistoryDao,
    private val apiClient: WeatherApiClient
) {
    fun getAllLocations(): Flow<List<LocationEntity>> =
        locationDao.getAllLocationsFlow().distinctUntilChanged()

    fun observeLocationById(id: Long): Flow<LocationEntity?> =
        locationDao.observeLocationById(id).distinctUntilChanged()

    suspend fun getSelectedLocation(): LocationEntity? = locationDao.getSelectedLocation()

    suspend fun searchCity(query: String) = apiClient.searchCity(query)

    suspend fun addLocation(city: City): LocationEntity {
        // Already saved -> just select it instead of crashing on the UNIQUE index
        locationDao.findByCoordinates(city.latitude, city.longitude)?.let {
            selectLocation(it)
            return it
        }
        locationDao.deselectAllLocations()
        val entity = LocationEntity(
            name = city.name,
            latitude = city.latitude,
            longitude = city.longitude,
            selected = true
        )
        val id = locationDao.insertLocation(entity)
        if (id == -1L) {
            return locationDao.findByCoordinates(city.latitude, city.longitude) ?: entity
        }
        return entity.copy(id = id)
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

    // NB: includeHourly is kept for compatibility; hourly data is always fetched.
    suspend fun refreshSelectedLocationWeather(includeHourly: Boolean = false): LocationEntity? {
        val location = getSelectedLocation() ?: return null
        updateWeather(location, ApiConstants.getForecastUrl(location.latitude, location.longitude))
        return getSelectedLocation()
    }

    suspend fun refreshLocationWeather(id: Long): LocationEntity? {
        val location = locationDao.findById(id) ?: return null
        updateWeather(location, ApiConstants.getForecastUrl(location.latitude, location.longitude))
        return locationDao.findById(id)
    }

    /** Fetches weather JSON without persisting (e.g. for the transient current location). */
    suspend fun fetchWeatherData(latitude: Double, longitude: Double): String? {
        return try {
            apiClient.get(ApiConstants.getForecastUrl(latitude, longitude))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
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

    fun getHourlyForecasts(location: LocationEntity): List<HourlyForecast> {        val data = location.weatherData ?: return emptyList()
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

    fun getDailyForecasts(location: LocationEntity): List<DailyForecast> {        val data = location.weatherData ?: return emptyList()
        return try {
            val json = Json.parseToJsonElement(data).jsonObject
            val daily = json["daily"]?.jsonObject ?: return emptyList()
            val dates = daily["time"]?.jsonArray ?: return emptyList()
            val codes = daily["weather_code"]?.jsonArray ?: daily["weathercode"]?.jsonArray ?: return emptyList()
            val maxTemps = daily["temperature_2m_max"]?.jsonArray ?: return emptyList()
            val minTemps = daily["temperature_2m_min"]?.jsonArray ?: return emptyList()
            val sunrises = daily["sunrise"]?.jsonArray
            val sunsets = daily["sunset"]?.jsonArray
            val precipSums = daily["precipitation_sum"]?.jsonArray
            val precipProbs = daily["precipitation_probability_max"]?.jsonArray
            val windMaxs = daily["wind_speed_10m_max"]?.jsonArray

            List(minOf(dates.size, codes.size, maxTemps.size, minTemps.size, 16)) { i ->
                DailyForecast(
                    date = dates[i].jsonPrimitive.content,
                    code = codes[i].jsonPrimitive.intOrNull ?: 0,
                    maxTemp = maxTemps[i].jsonPrimitive.doubleOrNull?.toInt() ?: 0,
                    minTemp = minTemps[i].jsonPrimitive.doubleOrNull?.toInt() ?: 0,
                    sunrise = sunrises?.getOrNull(i)?.jsonPrimitive?.content ?: "--",
                    sunset = sunsets?.getOrNull(i)?.jsonPrimitive?.content ?: "--",
                    precipSum = precipSums?.getOrNull(i)?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    precipProbMax = precipProbs?.getOrNull(i)?.jsonPrimitive?.intOrNull ?: 0,
                    windMax = windMaxs?.getOrNull(i)?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCurrentHourExtras(location: LocationEntity): CurrentHourExtras? {
        val data = location.weatherData ?: return null
        return try {
            val json = Json.parseToJsonElement(data).jsonObject
            val hourly = json["hourly"]?.jsonObject ?: return null
            val times = hourly["time"]?.jsonArray ?: return null
            val hourPrefix = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .toString().take(13)
            val index = times.indexOfFirst { it.jsonPrimitive.content.startsWith(hourPrefix) }
            if (index < 0) return null
            val visibilityM = hourly["visibility"]?.jsonArray
                ?.getOrNull(index)?.jsonPrimitive?.doubleOrNull
            val cloudBase = hourly["cloud_base"]?.jsonArray
                ?.getOrNull(index)?.jsonPrimitive?.doubleOrNull
            val pressures = hourly["pressure_msl"]?.jsonArray
            val trend = if (pressures != null && index >= 3) {
                val current = pressures[index].jsonPrimitive.doubleOrNull ?: 0.0
                val past = pressures[index - 3].jsonPrimitive.doubleOrNull ?: 0.0
                val diff = current - past
                when {
                    diff > 1.0 -> 1
                    diff < -1.0 -> -1
                    else -> 0
                }
            } else {
                0
            }
            CurrentHourExtras(
                visibilityKm = visibilityM?.div(1000.0),
                cloudBaseM = cloudBase,
                pressureTrend = trend
            )
        } catch (e: Exception) {
            null
        }
    }
}
