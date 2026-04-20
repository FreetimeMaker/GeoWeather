package com.freetime.geoweather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freetime.geoweather.network.WeatherApi
import com.freetime.geoweather.network.models.GeocodingResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(private val api: WeatherApi) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    @OptIn(FlowPreview::class)
    val searchEffect = searchQuery
        .debounce(500)
        .filter { it.length >= 2 }
        .onEach { query ->
            _isSearching.value = true
            try {
                val result = api.searchLocations(query, "en")
                _searchResults.value = if (result.isSuccess) {
                    result.getOrNull()?.results ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
        .launchIn(viewModelScope)

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }
}
