package io.github.freetimemaker.geoweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.freetimemaker.geoweather.currentInstant
import io.github.freetimemaker.geoweather.data.LocationDao
import io.github.freetimemaker.geoweather.network.WeatherApi
import io.github.freetimemaker.geoweather.network.models.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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
                val result = api.getWeather(lat, lon)
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    
                    // Fetch historical data for the last 7 days as well
                    val today = currentInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val startDate = today.minus(7, DateTimeUnit.DAY).toString()
                    val endDate = today.minus(1, DateTimeUnit.DAY).toString()
                    
                    val historicalResult = api.getHistoricalWeather(lat, lon, startDate, endDate)
                    val historicalResponse = if (historicalResult.isSuccess) historicalResult.getOrNull() else null

                    _uiState.value = _uiState.value.copy(
                        weather = response, 
                        historicalWeather = historicalResponse,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, 
                        error = result.exceptionOrNull()?.message ?: "Failed to load weather data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }
}
