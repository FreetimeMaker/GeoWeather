package com.freetime.geoweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.WeatherRepository
import com.freetime.geoweather.data.WeatherHistoryEntity
import com.freetime.geoweather.data.BackupManager
import com.freetime.geoweather.data.ApiConstants
import com.freetime.geoweather.domain.City
import com.freetime.geoweather.domain.SubscriptionTier
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<City>>(emptyList())
    val searchResults: StateFlow<List<City>> = _searchResults.asStateFlow()

    val locations = repository.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedLocation = locations.map { list ->
        list.find { it.selected } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedHistory = selectedLocation
        .flatMapLatest { location ->
            location?.let { repository.getHistoryForLocation(it.name) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        selectedLocation.onEach { location ->
            if (location != null) {
                _uiState.value = WeatherUiState.Success(location)
            } else if (locations.value.isEmpty()) {
                _uiState.value = WeatherUiState.Empty
            }
        }.launchIn(viewModelScope)
    }

    fun searchCity(query: String) {
        viewModelScope.launch {
            _searchResults.value = repository.searchCity(query)
        }
    }

    fun addLocation(city: City) {
        viewModelScope.launch {
            repository.addLocation(city)
            _searchResults.value = emptyList()
        }
    }

    fun refreshWeather(subscriptionTier: SubscriptionTier) {
        viewModelScope.launch {
            val selected = repository.getSelectedLocation() ?: locations.value.firstOrNull()
            if (selected != null) {
                _uiState.value = WeatherUiState.Success(selected)
                repository.updateWeather(selected, subscriptionTier.forecastDays)
            } else {
                _uiState.value = WeatherUiState.Empty
            }
        }
    }

    fun selectLocation(location: LocationEntity, subscriptionTier: SubscriptionTier) {
        viewModelScope.launch {
            repository.selectLocation(location)
            refreshWeather(subscriptionTier)
        }
    }

    fun deleteLocation(location: LocationEntity) {
        viewModelScope.launch {
            repository.deleteLocation(location)
        }
    }

    fun importLocations(raw: String) {
        viewModelScope.launch {
            runCatching { BackupManager.importLocations(raw) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { repository.importLocations(it) }
        }
    }
}

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    object Empty : WeatherUiState()
    data class Success(val location: LocationEntity) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}
