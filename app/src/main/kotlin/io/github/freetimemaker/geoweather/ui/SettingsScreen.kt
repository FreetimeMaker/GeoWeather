package io.github.freetimemaker.geoweather.ui
import io.github.freetimemaker.geoweather.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.freetimemaker.geoweather.SettingsManager
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit,
    onOpenChangeLog: () -> Unit
) {
    val useSystemTheme by settingsManager.useSystemTheme.collectAsState()
    val darkModeEnabled by settingsManager.darkModeEnabled.collectAsState()
    val dynamicColor by settingsManager.dynamicColor.collectAsState()
    val tempUnit by settingsManager.tempUnit.collectAsState()
    val windUnit by settingsManager.windUnit.collectAsState()
    val language by settingsManager.language.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_nav_desc)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        SettingsContent(
            modifier = Modifier.padding(innerPadding),
            useSystemTheme = useSystemTheme,
            darkModeEnabled = darkModeEnabled,
            dynamicColor = dynamicColor,
            tempUnit = tempUnit,
            windUnit = windUnit,
            language = language,
            settingsManager = settingsManager,
            onOpenChangeLog = onOpenChangeLog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    useSystemTheme: Boolean,
    darkModeEnabled: Boolean,
    dynamicColor: Boolean,
    tempUnit: String,
    windUnit: String,
    language: String,
    settingsManager: SettingsManager,
    onOpenChangeLog: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            item {
                Text(
                    text = stringResource(R.string.appearance_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.use_system_theme_label))
                        Text(
                            stringResource(R.string.use_system_theme_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = useSystemTheme, onCheckedChange = { settingsManager.setUseSystemTheme(it) })
                }
            }
            if (!useSystemTheme) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.dark_mode_label))
                        Switch(checked = darkModeEnabled, onCheckedChange = { settingsManager.setDarkModeEnabled(it) })
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.dynamic_color_label))
                        Text(
                            stringResource(R.string.dynamic_color_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = dynamicColor, onCheckedChange = { settingsManager.setDynamicColor(it) })
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    text = stringResource(R.string.language_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Language")
                    Row {
                        FilterChip(
                            selected = language == "en",
                            onClick = { settingsManager.setLanguage("en") },
                            label = { Text("English") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = language == "de",
                            onClick = { settingsManager.setLanguage("de") },
                            label = { Text("Deutsch") }
                        )
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.temp_unit_label))
                    Row {
                        FilterChip(
                            selected = tempUnit == "celsius",
                            onClick = { settingsManager.setTempUnit("celsius") },
                            label = { Text("°C") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = tempUnit == "fahrenheit",
                            onClick = { settingsManager.setTempUnit("fahrenheit") },
                            label = { Text("°F") }
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.wind_unit_label))
                    Row {
                        FilterChip(
                            selected = windUnit == "kmh",
                            onClick = { settingsManager.setWindUnit("kmh") },
                            label = { Text("km/h") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = windUnit == "mph",
                            onClick = { settingsManager.setWindUnit("mph") },
                            label = { Text("mph") }
                        )
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                OutlinedButton(
                    onClick = onOpenChangeLog,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.open_change_log))
                }
            }
        }
}