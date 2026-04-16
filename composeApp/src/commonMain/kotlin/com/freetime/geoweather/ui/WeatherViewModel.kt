package com.freetime.geoweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.data.LocationDao
import com.freetime.geoweather.network.WeatherApi
import com.freetime.geoweather.network.models.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.DateTimeUnit

data class WeatherUiState(
    val weather: WeatherResponse? = null,
    val historicalWeather: WeatherResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class WeatherViewModel(
    private val api: WeatherApi,
    private val locationDao: LocationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    fun loadWeather(lat: Double, lon: Double, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getWeather(lat, lon)
                
                // Fetch historical data for the last 7 days as well
                val today = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val startDate = today.minus(7, DateTimeUnit.DAY).toString()
                val endDate = today.minus(1, DateTimeUnit.DAY).toString()
                
                val historicalResponse = try {
                    api.getHistoricalWeather(lat, lon, startDate, endDate)
                } catch (e: Exception) {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    weather = response, 
                    historicalWeather = historicalResponse,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }
}
