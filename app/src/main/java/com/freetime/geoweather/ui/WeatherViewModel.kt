package com.freetime.geoweather.ui

import com.freetime.geoweather.WeatherNotificationScheduler
import com.freetime.geoweather.getAndroidAppContext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.WeatherRepository
import com.freetime.geoweather.data.exportLocationsJson
import com.freetime.geoweather.data.parseLocationsBackup
import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.domain.City
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<City>>(emptyList())
    val searchResults: StateFlow<List<City>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    val locations = repository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun repositoryForSync(): WeatherRepository = repository

    fun searchCity(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            try {
                val alreadySaved = repository.getAllLocationsSync()
                _searchResults.value = repository.searchCity(query).filterNot { city ->
                    alreadySaved.any { existing ->
                        kotlin.math.abs(existing.latitude - city.latitude) < 0.001 &&
                            kotlin.math.abs(existing.longitude - city.longitude) < 0.001
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun addLocation(city: City) {
        viewModelScope.launch {
            try {
                repository.addLocation(city)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _searchResults.value = emptyList()
            }
        }
    }
    fun selectLocation(location: LocationEntity) {
        viewModelScope.launch {
            try {
                repository.selectLocation(location)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch {
            try {
                repository.deleteLocation(location)
                getAndroidAppContext()?.let { WeatherNotificationScheduler.cancel(it, location.id) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setOfflinePackEnabled(location: LocationEntity, enabled: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setOfflinePackEnabled(location, enabled) }
                .onFailure { it.printStackTrace() }
        }
    }

    fun toggleLocationNotifications(location: LocationEntity) {
        viewModelScope.launch {
            try {
                repository.toggleLocationNotifications(location)
                val context = getAndroidAppContext()
                if (context != null) {
                    val updated = repository.getAllLocationsSync().firstOrNull { it.id == location.id }
                    if (updated != null) WeatherNotificationScheduler.schedule(context, updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setLocationNotifications(location: LocationEntity, enabled: Boolean, time: String) {
        viewModelScope.launch {
            try {
                repository.setLocationNotifications(location, enabled, time)
                val context = getAndroidAppContext()
                val updated = repository.getAllLocationsSync().firstOrNull { it.id == location.id }
                if (context != null && updated != null) {
                    if (enabled) WeatherNotificationScheduler.schedule(context, updated)
                    else WeatherNotificationScheduler.cancel(context, location.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun moveLocation(location: LocationEntity, direction: Int) {
        viewModelScope.launch {
            try {
                repository.moveLocation(location, direction)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleDefaultLocation(location: LocationEntity) {
        viewModelScope.launch {
            try {
                repository.toggleDefaultLocation(location)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun refreshAllLocations(offlinePacksOnly: Boolean = false) {
        locations.value
            .filter { !offlinePacksOnly || it.offlinePackEnabled }
            .map { location ->
                viewModelScope.async {
                    runCatching { repository.refreshLocationWeather(location.id) }
                }
            }.awaitAll()
    }

    fun refreshWeather() {
        viewModelScope.launch {
            try {
                val selected = repository.getSelectedLocation() ?: locations.value.firstOrNull()
                if (selected != null) {
                    _uiState.value = WeatherUiState.Success(selected)
                    repository.updateWeather(
                        selected,
                        ApiConstants.getForecastUrl(selected.latitude, selected.longitude)
                    )
                } else {
                    _uiState.value = WeatherUiState.Empty
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_uiState.value is WeatherUiState.Loading) {
                    _uiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun observeLocation(id: Long): Flow<LocationEntity?> = repository.observeLocationById(id)

    suspend fun getModelComparison(location: LocationEntity) = repository.getModelComparison(location)
    suspend fun getForecastConfidence(location: LocationEntity) = repository.getForecastConfidence(location)
    suspend fun getDailyModelAgreement(location: LocationEntity) = repository.getDailyModelAgreement(location)
    suspend fun getForecastAccuracy(locationId: Long) = repository.getForecastAccuracy(locationId)

    fun getHourlyForecasts(location: LocationEntity) = repository.getHourlyForecasts(location)

    fun getDailyForecasts(location: LocationEntity) = repository.getDailyForecasts(location)

    fun getWeatherHistory(locationName: String) = repository.getWeatherHistory(locationName)

    fun getCurrentHourExtras(location: LocationEntity) = repository.getCurrentHourExtras(location)

    suspend fun getAirQualityExtras(location: LocationEntity) = repository.getAirQualityExtras(location)

    /** Builds the backup JSON, or null on error. */
    suspend fun buildBackupJson(): String? {
        return try {
            exportLocationsJson(repository.getAllLocationsSync())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Imports a backup JSON payload. Returns false on error. */
    suspend fun importBackupJson(content: String): Boolean {
        return try {
            repository.importBackupLocations(parseLocationsBackup(content))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }    fun refreshLocation(id: Long, onDone: () -> Unit = {}) {        viewModelScope.launch {
            try {
                repository.refreshLocationWeather(id)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onDone()
            }
        }
    }

    fun fetchTransientWeather(
        name: String,
        latitude: Double,
        longitude: Double,
        onDone: (LocationEntity?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val data = repository.fetchWeatherData(latitude, longitude)
                if (data != null) {
                    onDone(
                        LocationEntity(
                            name = name,
                            latitude = latitude,
                            longitude = longitude,
                            weatherData = data,
                            lastUpdated = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        )
                    )
                } else {
                    onDone(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onDone(null)
            }
        }
    }
}

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data object Empty : WeatherUiState()
    data class Success(val location: LocationEntity) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}
