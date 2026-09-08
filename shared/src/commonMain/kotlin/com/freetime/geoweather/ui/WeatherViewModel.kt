package com.freetime.geoweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.WeatherRepository
import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.domain.City
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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

    fun searchCity(query: String) {
        searchJob?.cancel()
        if (query.length <= 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            try {
                val alreadySaved = locations.value
                _searchResults.value = repository.searchCity(query).filter { city ->
                    alreadySaved.none { existing ->
                        kotlin.math.abs(existing.latitude - city.latitude) < 1e-6 &&
                            kotlin.math.abs(existing.longitude - city.longitude) < 1e-6
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleLocationNotifications(location: LocationEntity) {
        viewModelScope.launch {
            try {
                repository.toggleLocationNotifications(location)
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

    fun getHourlyForecasts(location: LocationEntity) = repository.getHourlyForecasts(location)

    fun getDailyForecasts(location: LocationEntity) = repository.getDailyForecasts(location)

    fun getCurrentHourExtras(location: LocationEntity) = repository.getCurrentHourExtras(location)    fun refreshLocation(id: Long, onDone: () -> Unit = {}) {        viewModelScope.launch {
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
