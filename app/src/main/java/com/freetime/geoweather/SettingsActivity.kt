package com.freetime.geoweather

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
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

    val tempUnitState by sharedPreferences.collectStringAsState("temp_unit", "celsius")
    var tempUnit by remember { mutableStateOf(tempUnitState) }
    LaunchedEffect(tempUnitState) { tempUnit = tempUnitState }

    val windUnitState by sharedPreferences.collectStringAsState("wind_unit", "kmh")
    var windUnit by remember { mutableStateOf(windUnitState) }
    LaunchedEffect(windUnitState) { windUnit = windUnitState }

    val weatherProviderState by sharedPreferences.collectStringAsState("weather_provider", "open_meteo")
    var weatherProvider by remember { mutableStateOf(weatherProviderState) }
    LaunchedEffect(weatherProviderState) { weatherProvider = weatherProviderState }

    val weatherApiKeyState by sharedPreferences.collectStringAsState("weather_api_key", "")
    var weatherApiKey by remember { mutableStateOf(weatherApiKeyState) }
    LaunchedEffect(weatherApiKeyState) { weatherApiKey = weatherApiKeyState }

    val qweatherApiKeyState by sharedPreferences.collectStringAsState("qweather_api_key", "")
    var qweatherApiKey by remember { mutableStateOf(qweatherApiKeyState) }
    LaunchedEffect(qweatherApiKeyState) { qweatherApiKey = qweatherApiKeyState }

    var tempThreshold by remember {
        mutableStateOf(sharedPreferences.getInt("notif_temp_threshold", 5))
    }

    var windThreshold by remember {
        mutableStateOf(sharedPreferences.getInt("notif_wind_threshold", 15))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar area
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
            style = MaterialTheme.typography.headlineSmall
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
                    title = stringResource(R.string.dynamic_color_title),
                    subtitle = stringResource(R.string.dynamic_color_subtitle),
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

        Text(
            text = stringResource(R.string.unit_settings_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(stringResource(R.string.temperature_unit), style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tempUnit == "celsius", onClick = {
                            tempUnit = "celsius"
                            sharedPreferences.edit().putString("temp_unit", "celsius").apply()
                        })
                        Text(stringResource(R.string.unit_celsius))
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = tempUnit == "fahrenheit", onClick = {
                            tempUnit = "fahrenheit"
                            sharedPreferences.edit().putString("temp_unit", "fahrenheit").apply()
                        })
                        Text(stringResource(R.string.unit_fahrenheit))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Column {
                    Text(stringResource(R.string.wind_speed_unit), style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = windUnit == "kmh", onClick = {
                            windUnit = "kmh"
                            sharedPreferences.edit().putString("wind_unit", "kmh").apply()
                        })
                        Text(stringResource(R.string.unit_kmh))
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = windUnit == "mph", onClick = {
                            windUnit = "mph"
                            sharedPreferences.edit().putString("wind_unit", "mph").apply()
                        })
                        Text(stringResource(R.string.unit_mph))
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.notification_settings_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.temp_threshold_label, tempThreshold))
                Slider(
                    value = tempThreshold.toFloat(),
                    onValueChange = { tempThreshold = it.toInt() },
                    onValueChangeFinished = { sharedPreferences.edit().putInt("notif_temp_threshold", tempThreshold).apply() },
                    valueRange = 1f..15f,
                    steps = 14
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(stringResource(R.string.wind_threshold_label, windThreshold))
                Slider(
                    value = windThreshold.toFloat(),
                    onValueChange = { windThreshold = it.toInt() },
                    onValueChangeFinished = { sharedPreferences.edit().putInt("notif_wind_threshold", windThreshold).apply() },
                    valueRange = 5f..50f,
                    steps = 9
                )
            }
        }

        Text(
            text = stringResource(R.string.weather_provider_title),
            style = MaterialTheme.typography.headlineSmall
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = weatherProvider == "open_meteo", onClick = {
                        weatherProvider = "open_meteo"
                        sharedPreferences.edit().putString("weather_provider", "open_meteo").apply()
                    })
                    Text(stringResource(R.string.provider_open_meteo))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = weatherProvider == "weatherapi", onClick = {
                        weatherProvider = "weatherapi"
                        sharedPreferences.edit().putString("weather_provider", "weatherapi").apply()
                    })
                    Text(stringResource(R.string.provider_weatherapi))
                }
                
                if (weatherProvider == "weatherapi") {
                    OutlinedTextField(
                        value = weatherApiKey,
                        onValueChange = {
                            weatherApiKey = it
                            sharedPreferences.edit().putString("weather_api_key", it).apply()
                        },
                        label = { Text(stringResource(R.string.weather_api_key_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = qweatherApiKey,
                    onValueChange = {
                        qweatherApiKey = it
                        sharedPreferences.edit().putString("qweather_api_key", it).apply()
                    },
                    label = { Text(stringResource(R.string.qweather_api_key_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
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
