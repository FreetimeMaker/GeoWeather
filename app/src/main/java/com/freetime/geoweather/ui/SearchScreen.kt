package com.freetime.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import com.freetime.geoweather.ui.glass.GeoWeatherGlassTopBar
import com.freetime.geoweather.ui.glass.GeoWeatherGlassTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onCitySelected: () -> Unit,
    onBack: () -> Unit = onCitySelected
) {
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("search_history", android.content.Context.MODE_PRIVATE) }
    var recentSearches by remember {
        mutableStateOf(prefs.getStringSet("queries", emptySet()).orEmpty().toList().take(5))
    }
    fun rememberQuery(value: String) {
        if (value.isBlank()) return
        recentSearches = (listOf(value.trim()) + recentSearches.filterNot { it.equals(value.trim(), true) }).take(5)
        prefs.edit().putStringSet("queries", recentSearches.toSet()).apply()
    }
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    DisposableEffect(Unit) {
        onDispose { viewModel.clearSearch() }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            GeoWeatherGlassTopBar(
                title = stringResource(Res.string.search_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            GeoWeatherGlassTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchCity(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.search_placeholder)
            )

            Spacer(modifier = Modifier.height(12.dp))
            if (query.isBlank()) {
                Text(stringResource(Res.string.quick_actions_title), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .geoWeatherGlass(RoundedCornerShape(50), interactive = true)
                            .clickable {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    viewModel.refreshAllLocations()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) { Text(stringResource(Res.string.command_refresh_weather)) }
                    Box(
                        modifier = Modifier
                            .geoWeatherGlass(RoundedCornerShape(50), interactive = true)
                            .clickable(onClick = onBack)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) { Text(stringResource(Res.string.command_saved_locations)) }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (recentSearches.isNotEmpty() && query.isBlank()) {
                Text(stringResource(Res.string.recent_searches), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentSearches) { recent ->
                        Box(
                            modifier = Modifier
                                .geoWeatherGlass(RoundedCornerShape(50), interactive = true)
                                .clickable { query = recent; viewModel.searchCity(recent) }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) { Text(recent) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when {
                isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                query.isNotBlank() && results.isEmpty() -> {
                    Text(
                        text = stringResource(Res.string.search_no_results),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn {
                        items(results, key = { "${it.latitude},${it.longitude}" }) { city ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .geoWeatherGlass(RoundedCornerShape(22.dp), interactive = true)
                                    .clickable {
                                        rememberQuery(city.name)
                                        viewModel.addLocation(city)
                                        onCitySelected()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(city.name, style = MaterialTheme.typography.titleMedium)
                                Text("${city.latitude}, ${city.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
