package com.freetime.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.data.AppSettings
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWeatherScreen(
    viewModel: WeatherViewModel,
    appSettings: AppSettings,
    onAddLocationClick: () -> Unit,
    onLocationClick: (LocationEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onDonateClick: () -> Unit,
    onRadarClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val tempUnit by appSettings.tempUnit.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    IconButton(onClick = onRadarClick) {
                        Icon(Icons.Default.Public, contentDescription = "Radar")
                    }
                    IconButton(onClick = onDonateClick) {
                        Icon(Icons.Default.Favorite, contentDescription = "Donate")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddLocationClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Location")
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
            when (val s = state) {
                is WeatherUiState.Loading -> CircularProgressIndicator()
                is WeatherUiState.Empty -> Text(stringResource(Res.string.main_no_locations))
                is WeatherUiState.Success -> {
                    Box(modifier = Modifier.clickable { onLocationClick(s.location) }) {
                        WeatherContent(s.location, tempUnit)
                    }
                }
                is WeatherUiState.Error -> Text("Error: ${s.message}")
            }
        }
    }
}

@Composable
fun WeatherContent(location: LocationEntity, tempUnit: String) {
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
                val displayTemp = if (tempUnit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()
                val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                Text("$displayTemp$tempSuffix", style = MaterialTheme.typography.displayLarge)
            }
            
            val code = location.currentWeatherCode
            if (code != null) {
                Text(stringResource(WeatherCodes.getStringResource(code)), style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(Res.string.main_last_updated, location.lastUpdated.toString()), style = MaterialTheme.typography.bodySmall)
        }
    }
}
