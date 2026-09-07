package com.freetime.geoweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.WeatherRepository
import com.freetime.geoweather.ApiConstants
import com.freetime.geoweather.domain.City
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

    init {
        refreshWeather()
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

    fun refreshWeather() {
        viewModelScope.launch {
            val selected = repository.getSelectedLocation() ?: locations.value.firstOrNull()
            if (selected != null) {
                _uiState.value = WeatherUiState.Success(selected)
                val url = ApiConstants.OPEN_METEO_FORECAST + 
                    "?latitude=${selected.latitude}&longitude=${selected.longitude}" +
                    "&current_weather=true&hourly=temperature_2m,weathercode&daily=weathercode,temperature_2m_max,temperature_2m_min"
                
                repository.updateWeather(selected, url)
            } else {
                _uiState.value = WeatherUiState.Empty
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
