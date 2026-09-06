package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.WeatherHistoryEntity
import com.freetime.geoweather.domain.WeatherCodes
import com.freetime.geoweather.domain.SubscriptionTier
import com.freetime.geoweather.domain.UnitSettings
import com.freetime.geoweather.domain.WeatherIconMapper
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWeatherScreen(
    viewModel: WeatherViewModel,
    subscriptionTier: SubscriptionTier,
    locations: List<LocationEntity>,
    onLocationSelected: (LocationEntity) -> Unit,
    onLocationDeleted: (LocationEntity) -> Unit,
    onOpenRadar: (LocationEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSupport: () -> Unit,
    onLogout: () -> Unit,
    onAddLocationClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val history by viewModel.selectedHistory.collectAsState()
    val selectedLocation = locations.find { it.selected } ?: locations.firstOrNull()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    Text(
                        text = stringResource(Res.string.main_plan, subscriptionTier.name),
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                    IconButton(
                        onClick = { selectedLocation?.let(onOpenRadar) },
                        enabled = selectedLocation != null
                    ) {
                        Icon(Icons.Default.Public, contentDescription = stringResource(Res.string.main_open_radar))
                    }
                    IconButton(onClick = onOpenSupport) {
                        Icon(Icons.Default.Favorite, contentDescription = stringResource(Res.string.donate_title))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.settings_title))
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(Res.string.settings_logout))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLocationClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.main_add_location))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                if (locations.size > 1) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(locations, key = { it.id }) { location ->
                            AssistChip(
                                onClick = { onLocationSelected(location) },
                                label = { Text(location.name) },
                                leadingIcon = if (location.selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                trailingIcon = {
                                    IconButton(onClick = { onLocationDeleted(location) }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.main_delete_location))
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

            when (val s = state) {
                is WeatherUiState.Loading -> CircularProgressIndicator()
                is WeatherUiState.Empty -> Text(stringResource(Res.string.main_no_locations))
                is WeatherUiState.Success -> WeatherContent(s.location, history)
                is WeatherUiState.Error -> Text("Error: ${s.message}")
            }
        }
    }
}

@Composable
fun WeatherContent(location: LocationEntity, history: List<WeatherHistoryEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(location.name, style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            val temp = location.currentTemp
            if (temp != null) {
                Text(UnitSettings.temperature(temp), style = MaterialTheme.typography.displayLarge)
            }
            
            val code = location.currentWeatherCode
            if (code != null) {
                Text(
                    WeatherIconMapper.getWeatherEmoji(code, location.isDay),
                    fontSize = 48.sp
                )
                Text(WeatherCodes.getDescription(code), style = MaterialTheme.typography.titleMedium)
            }

            WeatherDetails(location)
            HistorySection(history)
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(Res.string.main_last_updated, location.lastUpdated.toString()), 
                style = MaterialTheme.typography.bodySmall
            )

            if (location.dailyForecast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(Res.string.main_forecast), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                location.dailyForecast.forEach { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(day.date, modifier = Modifier.weight(1f))
                        Text(
                            WeatherIconMapper.getWeatherEmoji(day.weatherCode),
                            modifier = Modifier.weight(0.5f),
                            fontSize = 22.sp
                        )
                        Text(
                            WeatherCodes.getDescription(day.weatherCode),
                            modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${UnitSettings.temperature(day.maxTemp)} / ${UnitSettings.temperature(day.minTemp)}",
                            modifier = Modifier.weight(1.5f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HorizontalDivider(modifier = Modifier.alpha(0.5f))
                }
            }

            if (location.hourlyForecast.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(Res.string.main_hourly_forecast), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(location.hourlyForecast.take(24)) { hour ->
                        Card {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(hour.time.substringAfter('T').take(5))
                                Text(
                                    WeatherIconMapper.getWeatherEmoji(hour.weatherCode),
                                    fontSize = 22.sp
                                )
                                Text(
                                    UnitSettings.temperature(hour.temperature),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    WeatherCodes.getDescription(hour.weatherCode),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                hour.precipitationProbability?.let {
                                    Text("${it}% rain", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySection(history: List<WeatherHistoryEntity>) {
    if (history.isEmpty()) return

    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(Res.string.historical_trends_label), style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(8.dp))
    history.take(7).forEach { entry ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(entry.timestamp.toString(), style = MaterialTheme.typography.labelSmall)
            Text("${entry.temperature}°", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WeatherDetails(location: LocationEntity) {
    val details = listOf(
        stringResource(Res.string.weather_feels_like) to location.feelsLike?.let { UnitSettings.temperature(it) },
        stringResource(Res.string.weather_humidity) to location.humidity?.let { "${it}%" },
        stringResource(Res.string.weather_precipitation) to location.precipitation?.let { "${it} mm" },
        stringResource(Res.string.weather_wind) to location.windSpeed?.let { UnitSettings.wind(it) },
        stringResource(Res.string.weather_pressure) to location.pressure?.let { UnitSettings.pressure(it) },
        stringResource(Res.string.weather_uv) to location.uvIndex?.let { "$it" },
        stringResource(Res.string.weather_visibility) to location.visibility?.let { "${it} km" },
        stringResource(Res.string.weather_cloud_cover) to location.cloudCover?.let { "${it}%" }
    ).filter { it.second != null }

    if (details.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            details.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (label, value) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                            Text(value.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    repeat(2 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
