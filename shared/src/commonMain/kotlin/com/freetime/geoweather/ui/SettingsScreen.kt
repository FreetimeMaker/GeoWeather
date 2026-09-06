package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.domain.SubscriptionTier
import com.freetime.geoweather.domain.UnitSettings
import geoweather.shared.generated.resources.Res
import geoweather.shared.generated.resources.back_nav_desc
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    subscriptionTier: SubscriptionTier,
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenChangeLog: () -> Unit,
    onOpenSupport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back_nav_desc)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current plan", style = MaterialTheme.typography.titleMedium)
                    Text(subscriptionTier.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Up to ${subscriptionTier.forecastDays}-day forecast",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Unit Settings", style = MaterialTheme.typography.titleMedium)
                    Text("Temperature", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = UnitSettings.temperature == "celsius",
                            onClick = { UnitSettings.temperature = "celsius" },
                            label = { Text("°C") }
                        )
                        FilterChip(
                            selected = UnitSettings.temperature == "fahrenheit",
                            onClick = { UnitSettings.temperature = "fahrenheit" },
                            label = { Text("°F") }
                        )
                    }
                    Text("Wind", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("kmh" to "km/h", "mph" to "mph", "ms" to "m/s").forEach { (key, label) ->
                            FilterChip(
                                selected = UnitSettings.wind == key,
                                onClick = { UnitSettings.wind = key },
                                label = { Text(label) }
                            )
                        }
                    }
                    Text("Pressure", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = UnitSettings.pressure == "hpa",
                            onClick = { UnitSettings.pressure = "hpa" },
                            label = { Text("hPa") }
                        )
                        FilterChip(
                            selected = UnitSettings.pressure == "mmhg",
                            onClick = { UnitSettings.pressure = "mmhg" },
                            label = { Text("mmHg") }
                        )
                    }
                }
            }
            Button(onClick = onOpenBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Backup & Restore")
            }
            Button(onClick = onOpenChangeLog, modifier = Modifier.fillMaxWidth()) {
                Text("What's new")
            }
            Button(onClick = onOpenSupport, modifier = Modifier.fillMaxWidth()) {
                Text("Support GeoWeather")
            }
        }
    }
}
