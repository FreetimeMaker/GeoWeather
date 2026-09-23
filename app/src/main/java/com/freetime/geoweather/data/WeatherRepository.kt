package com.freetime.geoweather.data

import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.domain.City
import com.freetime.geoweather.getAndroidAppContext
import com.freetime.geoweather.R as Res
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.*

data class HourlyForecast(
    val time: String,
    val temp: Int,
    val code: Int,
    val precipProbability: Int = 0,
    val humidity: Int? = null,
    val feelsLike: Double? = null,
    val visibilityKm: Double? = null,
    val pressure: Double? = null,
    val cloudBaseM: Double? = null,
    val precipitation: Double? = null,
    val rain: Double? = null,
    val snowfall: Double? = null,
    val windSpeed: Double? = null,
    val windGusts: Double? = null,
    val uvIndex: Double? = null
)

data class DailyForecast(
    val date: String,
    val code: Int,
    val maxTemp: Int,
    val minTemp: Int,
    val sunrise: String = "--",
    val sunset: String = "--",
    val precipSum: Double = 0.0,
    val precipProbMax: Int = 0,
    val windMax: Double = 0.0,
    val feelsLikeMax: Double? = null,
    val feelsLikeMin: Double? = null,
    val daylightDuration: Double? = null,
    val sunshineDuration: Double? = null,
    val uvMax: Double? = null,
    val rainSum: Double? = null,
    val snowfallSum: Double? = null,
    val precipitationHours: Double? = null,
    val windGustMax: Double? = null
)

data class ForecastModelPoint(
    val model: String,
    val time: String,
    val temperature: Double,
    val precipitationProbability: Int,
    val windSpeed: Double
)

data class DailyModelAgreement(
    val date: String,
    val score: Int,
    val temperatureSpread: Double,
    val precipitationSpread: Int,
    val windSpread: Double,
    val models: List<ForecastModelPoint>
)

data class ForecastConfidence(
    val score: Int,
    val temperatureSpread: Double,
    val precipitationSpread: Int,
    val windSpread: Double,
    val models: List<ForecastModelPoint>
)

data class CurrentHourExtras(
    val visibilityKm: Double?,
    val cloudBaseM: Double?,
    val pressureTrend: Int,
    val uvIndex: Double? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val europeanAqi: Int? = null,
    val alderPollen: Double? = null,
    val birchPollen: Double? = null,
    val grassPollen: Double? = null
)

class WeatherRepository(
    private val locationDao: LocationDao,
    private val historyDao: WeatherHistoryDao,
    private val apiClient: WeatherApiClient
) {
    private val deletedLocations = linkedSetOf<Pair<Double, Double>>()

    fun getDeletedLocationsForSync(): List<Pair<Double, Double>> = deletedLocations.toList()

    suspend fun applyDeletedLocations(locations: List<Pair<Double, Double>>) {
        locations.forEach { (lat, lon) ->
            locationDao.findByCoordinates(lat, lon)?.let { locationDao.deleteLocation(it) }
        }
    }

    fun getAllLocations(): Flow<List<LocationEntity>> =
        locationDao.getAllLocationsFlow().distinctUntilChanged()

    fun getWeatherHistory(locationName: String): Flow<List<WeatherHistoryEntity>> =
        historyDao.getHistoryForLocation(locationName)

    suspend fun getAllLocationsSync(): List<LocationEntity> =
        locationDao.getAllLocationsSync()

    fun observeLocationById(id: Long): Flow<LocationEntity?> =
        locationDao.observeLocationById(id).distinctUntilChanged()

    suspend fun getSelectedLocation(): LocationEntity? = locationDao.getSelectedLocation()

    suspend fun importBackupLocations(locations: List<LocationEntity>) {
        for (loc in locations) {
            val existing = locationDao.findByCoordinates(loc.latitude, loc.longitude)
            if (existing == null) {
                locationDao.insertLocation(loc.copy(id = 0))
            } else {
                locationDao.updateLocation(
                    existing.copy(
                        name = loc.name,
                        notificationsEnabled = loc.notificationsEnabled,
                        notificationTime = loc.notificationTime,
                        changeAlertsEnabled = loc.changeAlertsEnabled,
                        changeAlertInterval = loc.changeAlertInterval,
                        selected = loc.selected,
                        isDefault = loc.isDefault,
                        sortOrder = loc.sortOrder,
                        offlinePackEnabled = loc.offlinePackEnabled
                    )
                )
            }
        }
    }

    suspend fun searchCity(query: String) = apiClient.searchCity(query)

    suspend fun reverseGeocode(latitude: Double, longitude: Double) = apiClient.reverseGeocode(latitude, longitude)

    suspend fun addLocation(city: City): LocationEntity {
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
        deletedLocations += location.latitude to location.longitude
    }

    suspend fun setOfflinePackEnabled(location: LocationEntity, enabled: Boolean) {
        locationDao.updateLocation(location.copy(offlinePackEnabled = enabled))
    }

    suspend fun toggleLocationNotifications(location: LocationEntity) {
        locationDao.updateLocation(location.copy(notificationsEnabled = !location.notificationsEnabled))
    }

    suspend fun setLocationNotifications(location: LocationEntity, enabled: Boolean, time: String = location.notificationTime) {
        locationDao.updateLocation(location.copy(notificationsEnabled = enabled, notificationTime = time))
    }

    suspend fun setNotificationTime(location: LocationEntity, time: String) {
        locationDao.updateLocation(location.copy(notificationTime = time))
    }

    suspend fun moveLocation(location: LocationEntity, direction: Int) {
        val ordered = locationDao.getAllLocationsSync().toMutableList()
        if (ordered.isEmpty()) return
        ordered.forEachIndexed { index, item ->
            if (item.sortOrder != index) locationDao.updateLocation(item.copy(sortOrder = index))
        }
        val currentIndex = ordered.indexOfFirst { it.id == location.id }
        if (currentIndex < 0) return
        val targetIndex = (currentIndex + direction).coerceIn(0, ordered.lastIndex)
        if (targetIndex == currentIndex) return
        val current = ordered[currentIndex]
        val target = ordered[targetIndex]
        locationDao.updateLocation(current.copy(sortOrder = targetIndex))
        locationDao.updateLocation(target.copy(sortOrder = currentIndex))
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
        updateWeather(location, ApiConstants.getForecastUrl(location.latitude, location.longitude))
        return getSelectedLocation()
    }

    suspend fun refreshLocationWeather(id: Long): LocationEntity? {
        val location = locationDao.findById(id) ?: return null
        updateWeather(location, ApiConstants.getForecastUrl(location.latitude, location.longitude))
        return locationDao.findById(id)
    }

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
        val displayTemp = if (tempUnit == "fahrenheit") (temp * 9 / 5 + 32).toInt() else temp.toInt()
        val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
        return "$displayTemp$tempSuffix"
    }

    suspend fun getNotificationContent(location: LocationEntity, tempUnit: String): String {
        val tempStr = getDisplayTemp(location, tempUnit)
        val code = location.currentWeatherCode ?: 0
        val description = WeatherCodes.getDescription(code)
        return getAndroidAppContext()?.getString(
            Res.string.WeatherNotificationTXT,
            location.name,
            tempStr,
            description
        ).orEmpty()
    }

    private fun getLocationTimeZone(json: JsonObject): TimeZone {
        val zoneId = json["timezone"]?.jsonPrimitive?.contentOrNull
        return if (zoneId != null) {
            try {
                TimeZone.of(zoneId)
            } catch (_: Exception) {
                TimeZone.currentSystemDefault()
            }
        } else {
            TimeZone.currentSystemDefault()
        }
    }

    fun getHourlyForecasts(location: LocationEntity): List<HourlyForecast> {
        val data = location.weatherData ?: return emptyList()
        return try {
            val json = Json.parseToJsonElement(data).jsonObject
            val hourly = json["hourly"]?.jsonObject ?: return emptyList()
            val times = hourly["time"]?.jsonArray ?: return emptyList()
            val temps = hourly["temperature_2m"]?.jsonArray ?: return emptyList()
            val codes = hourly["weathercode"]?.jsonArray ?: hourly["weather_code"]?.jsonArray ?: return emptyList()
            val precipProbabilities = hourly["precipitation_probability"]?.jsonArray
            val humidities = hourly["relative_humidity_2m"]?.jsonArray
            val feelsLikes = hourly["apparent_temperature"]?.jsonArray
            val visibilities = hourly["visibility"]?.jsonArray
            val pressures = hourly["pressure_msl"]?.jsonArray
            val cloudBases = hourly["cloud_base"]?.jsonArray
            val precipitations = hourly["precipitation"]?.jsonArray
            val rains = hourly["rain"]?.jsonArray
            val snowfalls = hourly["snowfall"]?.jsonArray
            val windSpeeds = hourly["wind_speed_10m"]?.jsonArray
            val windGusts = hourly["wind_gusts_10m"]?.jsonArray
            val uvIndexes = hourly["uv_index"]?.jsonArray

            val locationTimeZone = getLocationTimeZone(json)
            val now = Clock.System.now().toLocalDateTime(locationTimeZone)
            val currentHourPrefix = now.toString().take(13)
            val startIndex = times.indexOfFirst {
                it.jsonPrimitive.content.startsWith(currentHourPrefix)
            }.takeIf { it >= 0 } ?: 0

            // Open-Meteo returns hourly values for the full 16-day request.
            // Keep the complete remaining forecast so every daily forecast can expose its hours.
            val count = (minOf(times.size, temps.size, codes.size) - startIndex).coerceAtLeast(0)

            List(count) { offset ->
                val i = startIndex + offset
                val timeStr = times[i].jsonPrimitive.content
                val temp = temps[i].jsonPrimitive.doubleOrNull?.toInt() ?: 0
                val code = codes[i].jsonPrimitive.intOrNull ?: 0
                HourlyForecast(
                    time = timeStr,
                    temp = temp,
                    code = code,
                    precipProbability = precipProbabilities?.getOrNull(i)?.jsonPrimitive?.intOrNull ?: 0,
                    humidity = humidities?.getOrNull(i)?.jsonPrimitive?.intOrNull,
                    feelsLike = feelsLikes?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    visibilityKm = visibilities?.getOrNull(i)?.jsonPrimitive?.doubleOrNull?.div(1000.0),
                    pressure = pressures?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    cloudBaseM = cloudBases?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    precipitation = precipitations?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    rain = rains?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    snowfall = snowfalls?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    windSpeed = windSpeeds?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    windGusts = windGusts?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    uvIndex = uvIndexes?.getOrNull(i)?.jsonPrimitive?.doubleOrNull
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getDailyForecasts(location: LocationEntity): List<DailyForecast> {
        val data = location.weatherData ?: return emptyList()
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
            val feelsLikeMaxs = daily["apparent_temperature_max"]?.jsonArray
            val feelsLikeMins = daily["apparent_temperature_min"]?.jsonArray
            val daylightDurations = daily["daylight_duration"]?.jsonArray
            val sunshineDurations = daily["sunshine_duration"]?.jsonArray
            val uvMaxs = daily["uv_index_max"]?.jsonArray
            val rainSums = daily["rain_sum"]?.jsonArray
            val snowfallSums = daily["snowfall_sum"]?.jsonArray
            val precipitationHours = daily["precipitation_hours"]?.jsonArray
            val windGustMaxs = daily["wind_gusts_10m_max"]?.jsonArray

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
                    windMax = windMaxs?.getOrNull(i)?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    feelsLikeMax = feelsLikeMaxs?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    feelsLikeMin = feelsLikeMins?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    daylightDuration = daylightDurations?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    sunshineDuration = sunshineDurations?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    uvMax = uvMaxs?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    rainSum = rainSums?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    snowfallSum = snowfallSums?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    precipitationHours = precipitationHours?.getOrNull(i)?.jsonPrimitive?.doubleOrNull,
                    windGustMax = windGustMaxs?.getOrNull(i)?.jsonPrimitive?.doubleOrNull
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAirQualityExtras(location: LocationEntity): CurrentHourExtras? {
        return try {
            val data = apiClient.get(ApiConstants.getAirQualityUrl(location.latitude, location.longitude))
            val json = Json.parseToJsonElement(data).jsonObject
            val hourly = json["hourly"]?.jsonObject ?: return null
            fun first(name: String) = hourly[name]?.jsonArray?.firstOrNull()?.jsonPrimitive?.doubleOrNull
            CurrentHourExtras(null, null, 0, null, first("pm2_5"), first("pm10"), first("european_aqi")?.toInt(), first("alder_pollen"), first("birch_pollen"), first("grass_pollen"))
        } catch (_: Exception) { null }
    }

    fun getCurrentHourExtras(location: LocationEntity): CurrentHourExtras? {
        val data = location.weatherData ?: return null
        return try {
            val json = Json.parseToJsonElement(data).jsonObject
            val hourly = json["hourly"]?.jsonObject ?: return null
            val times = hourly["time"]?.jsonArray ?: return null
            val locationTimeZone = getLocationTimeZone(json)
            val hourPrefix = Clock.System.now()
                .toLocalDateTime(locationTimeZone)
                .toString().take(13)
            val index = times.indexOfFirst { it.jsonPrimitive.content.startsWith(hourPrefix) }
            if (index < 0) return null
            val visibilityM = hourly["visibility"]?.jsonArray
                ?.getOrNull(index)?.jsonPrimitive?.doubleOrNull
            val cloudBase = hourly["cloud_base"]?.jsonArray
                ?.getOrNull(index)?.jsonPrimitive?.doubleOrNull
            val pressures = hourly["pressure_msl"]?.jsonArray
            val uvIndex = hourly["uv_index"]?.jsonArray?.getOrNull(index)?.jsonPrimitive?.doubleOrNull
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
                pressureTrend = trend,
                uvIndex = uvIndex
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDailyModelAgreement(location: LocationEntity): List<DailyModelAgreement> {
        val modelIds = listOf(
            "best_match" to "Best match",
            "ecmwf_ifs025" to "ECMWF",
            "gfs_seamless" to "GFS"
        )
        val byDate = linkedMapOf<String, MutableList<ForecastModelPoint>>()
        modelIds.forEach { (id, label) ->
            runCatching {
                val raw = apiClient.get(ApiConstants.getModelComparisonUrl(location.latitude, location.longitude, id))
                val json = Json.parseToJsonElement(raw).jsonObject
                val hourly = json["hourly"]?.jsonObject ?: return@runCatching
                val times = hourly["time"]?.jsonArray ?: return@runCatching
                val temps = hourly["temperature_2m"]?.jsonArray ?: return@runCatching
                val rain = hourly["precipitation_probability"]?.jsonArray
                val wind = hourly["wind_speed_10m"]?.jsonArray
                times.indices
                    .filter { times[it].jsonPrimitive.content.endsWith("T12:00") }
                    .take(16)
                    .forEach { index ->
                        val time = times[index].jsonPrimitive.content
                        val point = ForecastModelPoint(
                            model = label,
                            time = time,
                            temperature = temps.getOrNull(index)?.jsonPrimitive?.doubleOrNull ?: return@forEach,
                            precipitationProbability = rain?.getOrNull(index)?.jsonPrimitive?.intOrNull ?: 0,
                            windSpeed = wind?.getOrNull(index)?.jsonPrimitive?.doubleOrNull ?: 0.0
                        )
                        byDate.getOrPut(time.take(10)) { mutableListOf() }.add(point)
                    }
            }
        }
        return byDate.mapNotNull { (date, points) ->
            if (points.size < 2) return@mapNotNull null
            val tempSpread = points.maxOf { it.temperature } - points.minOf { it.temperature }
            val rainSpread = points.maxOf { it.precipitationProbability } - points.minOf { it.precipitationProbability }
            val windSpread = points.maxOf { it.windSpeed } - points.minOf { it.windSpeed }
            val penalty = (tempSpread * 12.0 + rainSpread * 0.35 + windSpread * 1.5).toInt()
            DailyModelAgreement(
                date = date,
                score = (100 - penalty).coerceIn(0, 100),
                temperatureSpread = tempSpread,
                precipitationSpread = rainSpread,
                windSpread = windSpread,
                models = points
            )
        }
    }

    suspend fun getForecastConfidence(location: LocationEntity): ForecastConfidence? {
        val modelIds = listOf(
            "best_match" to "Best match",
            "ecmwf_ifs025" to "ECMWF",
            "gfs_seamless" to "GFS"
        )
        val points = modelIds.mapNotNull { (id, label) ->
            runCatching {
                val raw = apiClient.get(ApiConstants.getModelComparisonUrl(location.latitude, location.longitude, id))
                val json = Json.parseToJsonElement(raw).jsonObject
                val hourly = json["hourly"]?.jsonObject ?: return@runCatching null
                val times = hourly["time"]?.jsonArray ?: return@runCatching null
                val temps = hourly["temperature_2m"]?.jsonArray ?: return@runCatching null
                val rain = hourly["precipitation_probability"]?.jsonArray
                val wind = hourly["wind_speed_10m"]?.jsonArray
                val zone = getLocationTimeZone(json)
                val prefix = Clock.System.now().toLocalDateTime(zone).toString().take(13)
                val index = times.indexOfFirst { it.jsonPrimitive.content.startsWith(prefix) }.takeIf { it >= 0 } ?: 0
                ForecastModelPoint(
                    model = label,
                    time = times.getOrNull(index)?.jsonPrimitive?.content ?: return@runCatching null,
                    temperature = temps.getOrNull(index)?.jsonPrimitive?.doubleOrNull ?: return@runCatching null,
                    precipitationProbability = rain?.getOrNull(index)?.jsonPrimitive?.intOrNull ?: 0,
                    windSpeed = wind?.getOrNull(index)?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
            }.getOrNull()
        }
        if (points.size < 2) return null
        val tempSpread = (points.maxOf { it.temperature } - points.minOf { it.temperature }).coerceAtLeast(0.0)
        val rainSpread = points.maxOf { it.precipitationProbability } - points.minOf { it.precipitationProbability }
        val windSpread = (points.maxOf { it.windSpeed } - points.minOf { it.windSpeed }).coerceAtLeast(0.0)
        val penalty = (tempSpread * 12.0 + rainSpread * 0.35 + windSpread * 1.5).toInt()
        return ForecastConfidence(
            score = (100 - penalty).coerceIn(0, 100),
            temperatureSpread = tempSpread,
            precipitationSpread = rainSpread,
            windSpread = windSpread,
            models = points
        )
    }

}
