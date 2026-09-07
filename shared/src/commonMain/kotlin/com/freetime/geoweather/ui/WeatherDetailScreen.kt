package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.WeatherIconMapper
import com.freetime.geoweather.data.AppSettings
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    location: LocationEntity,
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val tempUnit by appSettings.tempUnit.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(location.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val code = location.currentWeatherCode ?: 0
                val rawTemp = location.currentTemp ?: 0.0
                val displayTemp = if (tempUnit == "fahrenheit") (rawTemp * 9/5 + 32).toInt() else rawTemp.toInt()
                val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"

                Icon(
                    painter = painterResource(WeatherIconMapper.getWeatherIcon(code)),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = "$displayTemp$tempSuffix",
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = stringResource(WeatherCodes.getStringResource(code)),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WeatherDetailItem(stringResource(Res.string.latitude_label), "${location.latitude}")
                        WeatherDetailItem(stringResource(Res.string.longitude_label), "${location.longitude}")
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
