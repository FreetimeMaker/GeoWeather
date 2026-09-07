package com.freetime.geoweather.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.data.AppSettings
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings,
    onBack: () -> Unit,
    onChangeLogClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val tempUnit by appSettings.tempUnit.collectAsState()
    val useSystemTheme by appSettings.useSystemTheme.collectAsState()
    val darkModeEnabled by appSettings.darkModeEnabled.collectAsState()
    val dynamicColor by appSettings.dynamicColor.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(stringResource(Res.string.settings_theme), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            SettingsToggle(stringResource(Res.string.follow_system_theme), useSystemTheme) { appSettings.setUseSystemTheme(it) }
            if (!useSystemTheme) {
                SettingsToggle(stringResource(Res.string.force_dark_mode), darkModeEnabled) { appSettings.setDarkModeEnabled(it) }
            }
            SettingsToggle(stringResource(Res.string.dynamic_color_title), dynamicColor) { appSettings.setDynamicColor(it) }
            
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.settings_units), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.temperature_unit))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { 
                    appSettings.setTempUnit(if (tempUnit == "celsius") "fahrenheit" else "celsius") 
                }) {
                    Text(if (tempUnit == "celsius") stringResource(Res.string.unit_celsius) else stringResource(Res.string.unit_fahrenheit))
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = onChangeLogClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.open_change_log))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAboutClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.about_title))
            }
        }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Spacer(Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
