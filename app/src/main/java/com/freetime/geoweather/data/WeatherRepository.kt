package com.freetime.geoweather.data

import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.domain.City
import com.freetime.geoweather.getAndroidAppContext
import com.freetime.geoweather.R as Res
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
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
    private val appContext: android.content.Context,
    private val locationDao: LocationDao,
    private val historyDao: WeatherHistoryDao,
    private val forecastSnapshotDao: ForecastSnapshotDao,
    private val apiClient: WeatherApiClient
) {
    private val deletedLocations = linkedSetOf<Pair<Double, Double>>()

    private fun parseWeatherJson(data: String, section: String): JsonObject? = try {
        Json.parseToJsonElement(data).jsonObject
    } catch (e: Exception) {
        Log.w("WeatherRepository", "Failed to parse $section weather data", e)
        null
    }

    private suspend fun syncLocationBackup() = LocationBackupStore.sync(appContext, locationDao)

    fun getDeletedLocationsForSync(): List<Pair<Double, Double>> = deletedLocations.toList()

    suspend fun applyDeletedLocations(locations: List<Pair<Double, Double>>) {
        locations.forEach { (lat, lon) ->
            locationDao.findByCoordinates(lat, lon)?.let { locationDao.deleteLocation(it) }
        }
        syncLocationBackup()
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

    suspend fun getLocationById(id: Long): LocationEntity? = locationDao.findById(id)

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
        syncLocationBackup()
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
        syncLocationBackup()
        return entity.copy(id = id)
    }

    suspend fun selectLocation(location: LocationEntity) {
        locationDao.deselectAllLocations()
        locationDao.updateLocation(location.copy(selected = true))
        syncLocationBackup()
    }

    suspend fun deleteLocation(location: LocationEntity) {
        locationDao.deleteLocation(location)
        deletedLocations += location.latitude to location.longitude
        syncLocationBackup()
    }

    suspend fun setOfflinePackEnabled(location: LocationEntity, enabled: Boolean) {
        locationDao.updateLocation(location.copy(offlinePackEnabled = enabled))
        syncLocationBackup()
    }

    suspend fun clearOfflinePackCache(location: LocationEntity) {
        locationDao.updateLocation(
            location.copy(
                weatherData = null,
                lastUpdated = 0L
            )
        )
    }

    suspend fun toggleLocationNotifications(location: LocationEntity) {
        locationDao.updateLocation(location.copy(notificationsEnabled = !location.notificationsEnabled))
        syncLocationBackup()
    }

    suspend fun setLocationNotifications(location: LocationEntity, enabled: Boolean, time: String = location.notificationTime) {
        locationDao.updateLocation(location.copy(notificationsEnabled = enabled, notificationTime = time))
        syncLocationBackup()
    }

    suspend fun setNotificationTime(location: LocationEntity, time: String) {
        locationDao.updateLocation(location.copy(notificationTime = time))
        syncLocationBackup()
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
        syncLocationBackup()
    }

    suspend fun toggleDefaultLocation(location: LocationEntity) {
        if (location.isDefault) {
            locationDao.updateLocation(location.copy(isDefault = false))
        } else {
            locationDao.clearDefaultLocation()
            locationDao.updateLocation(location.copy(isDefault = true))
        }
        syncLocationBackup()
    }

    suspend fun updateWeather(location: LocationEntity, providerUrl: String) {
        try {
            val data = apiClient.get(providerUrl)
            val updated = location.copy(
                weatherData = data,
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )
            locationDao.updateLocation(updated)
            recordForecastAccuracy(updated)

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

    private val weatherRefreshMutexes = mutableMapOf<Long, Mutex>()
    private val weatherRefreshMutexesGuard = Mutex()

    private suspend fun refreshMutex(locationId: Long): Mutex =
        weatherRefreshMutexesGuard.withLock {
            weatherRefreshMutexes.getOrPut(locationId) { Mutex() }
        }

    suspend fun refreshSelectedLocationWeather(includeHourly: Boolean = false, minAgeMillis: Long = 0L): LocationEntity? {
        val location = getSelectedLocation() ?: return null
        return refreshLocationWeather(location.id, minAgeMillis)
    }

    suspend fun refreshLocationWeather(id: Long, minAgeMillis: Long = 0L): LocationEntity? {
        val mutex = refreshMutex(id)
        return mutex.withLock {
            val location = locationDao.findById(id) ?: return@withLock null
            val now = Clock.System.now().toEpochMilliseconds()
            if (minAgeMillis > 0L && location.weatherData != null &&
                location.lastUpdated > 0L && now - location.lastUpdated < minAgeMillis
            ) {
                return@withLock location
            }
            updateWeather(location, ApiConstants.getForecastUrl(location.latitude, location.longitude))
            locationDao.findById(id)
        }
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
            OpenMeteoForecastParser.hourly(data)
        } catch (e: Exception) {
            Log.w("WeatherRepository", "Failed to parse hourly forecast", e)
            emptyList()
        }
    }

    fun getDailyForecasts(location: LocationEntity): List<DailyForecast> {
        val data = location.weatherData ?: return emptyList()
        return try {
            OpenMeteoForecastParser.daily(data)
        } catch (e: Exception) {
            Log.w("WeatherRepository", "Failed to parse daily forecast", e)
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
        val json = parseWeatherJson(data, "current hour extras") ?: return null
        return try {
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
            Log.w("WeatherRepository", "Failed to read current-hour extras", e)
            null
        }
    }

    data class ModelComparisonResult(
        val confidence: ForecastConfidence?,
        val dailyAgreement: List<DailyModelAgreement>
    )

    suspend fun getModelComparison(location: LocationEntity): ModelComparisonResult {
        val modelIds = listOf(
            "best_match" to "Best match",
            "ecmwf_ifs025" to "ECMWF",
            "gfs_seamless" to "GFS"
        )
        val byDate = linkedMapOf<String, MutableList<ForecastModelPoint>>()
        val currentPoints = mutableListOf<ForecastModelPoint>()

        modelIds.forEach { (id, label) ->
            runCatching {
                val raw = apiClient.get(ApiConstants.getModelComparisonUrl(location.latitude, location.longitude, id))
                val json = Json.parseToJsonElement(raw).jsonObject
                val hourly = json["hourly"]?.jsonObject ?: return@runCatching
                val times = hourly["time"]?.jsonArray ?: return@runCatching
                val temps = hourly["temperature_2m"]?.jsonArray ?: return@runCatching
                val rain = hourly["precipitation_probability"]?.jsonArray
                val wind = hourly["wind_speed_10m"]?.jsonArray
                val zone = getLocationTimeZone(json)
                val hourPrefix = Clock.System.now().toLocalDateTime(zone).toString().take(13)
                val currentIndex = times.indexOfFirst { it.jsonPrimitive.content.startsWith(hourPrefix) }
                    .takeIf { it >= 0 } ?: 0

                fun pointAt(index: Int): ForecastModelPoint? {
                    val time = times.getOrNull(index)?.jsonPrimitive?.content ?: return null
                    val temperature = temps.getOrNull(index)?.jsonPrimitive?.doubleOrNull ?: return null
                    return ForecastModelPoint(
                        model = label,
                        time = time,
                        temperature = temperature,
                        precipitationProbability = rain?.getOrNull(index)?.jsonPrimitive?.intOrNull ?: 0,
                        windSpeed = wind?.getOrNull(index)?.jsonPrimitive?.doubleOrNull ?: 0.0
                    )
                }

                pointAt(currentIndex)?.let(currentPoints::add)
                times.indices
                    .filter { times[it].jsonPrimitive.content.endsWith("T12:00") }
                    .take(16)
                    .forEach { index ->
                        pointAt(index)?.let { point ->
                            byDate.getOrPut(point.time.take(10)) { mutableListOf() }.add(point)
                        }
                    }
            }
        }

        fun score(points: List<ForecastModelPoint>): Triple<Int, Triple<Double, Int, Double>, List<ForecastModelPoint>>? {
            if (points.size < 2) return null
            val tempSpread = (points.maxOf { it.temperature } - points.minOf { it.temperature }).coerceAtLeast(0.0)
            val rainSpread = points.maxOf { it.precipitationProbability } - points.minOf { it.precipitationProbability }
            val windSpread = (points.maxOf { it.windSpeed } - points.minOf { it.windSpeed }).coerceAtLeast(0.0)
            val penalty = (tempSpread * 12.0 + rainSpread * 0.35 + windSpread * 1.5).toInt()
            return Triple((100 - penalty).coerceIn(0, 100), Triple(tempSpread, rainSpread, windSpread), points)
        }

        val confidence = score(currentPoints)?.let { (value, spreads, points) ->
            ForecastConfidence(value, spreads.first, spreads.second, spreads.third, points)
        }
        val daily = byDate.mapNotNull { (date, points) ->
            score(points)?.let { (value, spreads, models) ->
                DailyModelAgreement(date, value, spreads.first, spreads.second, spreads.third, models)
            }
        }
        return ModelComparisonResult(confidence, daily)
    }

    suspend fun getDailyModelAgreement(location: LocationEntity): List<DailyModelAgreement> =
        getModelComparison(location).dailyAgreement

    suspend fun getForecastConfidence(location: LocationEntity): ForecastConfidence? =
        getModelComparison(location).confidence

    private suspend fun recordForecastAccuracy(location: LocationEntity) {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val data = location.weatherData ?: return
        val json = runCatching { Json.parseToJsonElement(data).jsonObject }.getOrNull() ?: return
        val zone = getLocationTimeZone(json)
        val hourly = getHourlyForecasts(location)
        val horizons = listOf(1, 6, 24, 72, 168)

        fun epochMillis(time: String): Long? = runCatching {
            LocalDateTime.parse(time).toInstant(zone).toEpochMilliseconds()
        }.getOrNull()

        horizons.forEach { hours ->
            val desired = nowMs + hours * 60L * 60L * 1000L
            val forecast = hourly.minByOrNull { item ->
                kotlin.math.abs((epochMillis(item.time) ?: Long.MAX_VALUE / 2) - desired)
            } ?: return@forEach
            val target = epochMillis(forecast.time) ?: return@forEach
            if (kotlin.math.abs(target - desired) > 90L * 60L * 1000L) return@forEach

            if (forecastSnapshotDao.findNearbySnapshot(location.id, target, hours, nowMs) == null) {
                forecastSnapshotDao.insert(
                    ForecastSnapshotEntity(
                        locationId = location.id,
                        issuedAt = nowMs,
                        targetEpochMillis = target,
                        horizonHours = hours,
                        forecastTemperature = forecast.temp.toDouble()
                    )
                )
            }
        }

        location.currentTemp?.let { actual ->
            // Only compare a forecast with an observation close to its intended
            // target time. Old targets are never evaluated with today's value.
            forecastSnapshotDao.getPending(location.id)
                .filter { kotlin.math.abs(it.targetEpochMillis - nowMs) <= 90L * 60L * 1000L }
                .forEach { forecastSnapshotDao.evaluate(it.id, actual, nowMs) }
        }
        forecastSnapshotDao.deleteOlderThan(nowMs - 90L * 24L * 60L * 60L * 1000L)
    }

    suspend fun getForecastAccuracy(locationId: Long): List<ForecastAccuracyBucket> {
        val rows = forecastSnapshotDao.getEvaluated(locationId)
        return listOf(1, 6, 24, 72, 168).mapNotNull { target ->
            val selected = rows.filter { it.horizonHours == target }
            if (selected.isEmpty()) return@mapNotNull null
            ForecastAccuracyBucket(
                horizonHours = target,
                samples = selected.size,
                meanAbsoluteError = selected.mapNotNull { row ->
                    row.actualTemperature?.let { kotlin.math.abs(it - row.forecastTemperature) }
                }.average()
            )
        }
    }

}
