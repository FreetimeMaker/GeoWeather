package com.freetime.geoweather

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = sharedPreferences.collectAsState(key = "use_system_theme", defaultValue = true)
            val darkModeEnabled = sharedPreferences.collectAsState(key = "dark_mode_enabled", defaultValue = false)
            val dynamicColor = sharedPreferences.collectAsState(key = "dynamic_color", defaultValue = true)

            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value
            
            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { 
        context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) 
    }
    
    var darkModeEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("dark_mode_enabled", false))
    }
    
    var useSystemTheme by remember {
        mutableStateOf(sharedPreferences.getBoolean("use_system_theme", true))
    }

    var dynamicColor by remember {
        mutableStateOf(sharedPreferences.getBoolean("dynamic_color", true))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = onBack) {
                Text(stringResource(R.string.back_btn))
            }

            FilledTonalButton(onClick = {
                context.startActivity(Intent(context, ChangeLogActivity::class.java))
            }) {
                Text(stringResource(R.string.open_change_log))
            }
        }
        
        Text(
            text = stringResource(R.string.theme_settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsToggle(
                    title = "Dynamische Farben (Material You)",
                    subtitle = "Farben basierend auf deinem Hintergrundbild (Android 12+)",
                    checked = dynamicColor,
                    onCheckedChange = {
                        dynamicColor = it
                        sharedPreferences.edit().putBoolean("dynamic_color", it).apply()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                SettingsToggle(
                    title = stringResource(R.string.follow_system_theme),
                    checked = useSystemTheme,
                    onCheckedChange = {
                        useSystemTheme = it
                        sharedPreferences.edit().putBoolean("use_system_theme", it).apply()
                    }
                )

                SettingsToggle(
                    title = stringResource(R.string.force_dark_mode),
                    subtitle = stringResource(R.string.override_system_setting),
                    checked = darkModeEnabled,
                    enabled = !useSystemTheme,
                    onCheckedChange = {
                        darkModeEnabled = it
                        sharedPreferences.edit().putBoolean("dark_mode_enabled", it).apply()
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
