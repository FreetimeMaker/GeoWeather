package com.freetime.geoweather

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freetime.geoweather.ui.LocationsViewModel
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
    val locationsViewModel: LocationsViewModel = viewModel()
    val sharedPreferences = remember { 
        context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) 
    }
    
    var darkModeEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("dark_mode_enabled", false)) }
    var useSystemTheme by remember { mutableStateOf(sharedPreferences.getBoolean("use_system_theme", true)) }
    var dynamicColor by remember { mutableStateOf(sharedPreferences.getBoolean("dynamic_color", true)) }
    var showHistoricalData by remember { mutableStateOf(sharedPreferences.getBoolean("show_historical_data", true)) }

    val authManager = remember { AuthManager.getInstance(context) }
    var isAuthenticated by remember { mutableStateOf(authManager.isAuthenticated) }
    val userInfo = authManager.userInfo

    val tempUnitState by sharedPreferences.collectStringAsState("temp_unit", "celsius")
    var tempUnit by remember { mutableStateOf(tempUnitState) }
    LaunchedEffect(tempUnitState) { tempUnit = tempUnitState }

    val windUnitState by sharedPreferences.collectStringAsState("wind_unit", "kmh")
    var windUnit by remember { mutableStateOf(windUnitState) }
    LaunchedEffect(windUnitState) { windUnit = windUnitState }

    val weatherProviderState by sharedPreferences.collectStringAsState("weather_provider", "open_meteo")
    var weatherProvider by remember { mutableStateOf(weatherProviderState) }
    LaunchedEffect(weatherProviderState) { weatherProvider = weatherProviderState }

    var weatherApiKey by remember { mutableStateOf(sharedPreferences.getString("weather_api_key", "") ?: "") }
    var qweatherApiKey by remember { mutableStateOf(sharedPreferences.getString("qweather_api_key", "") ?: "") }
    var tomorrowIoKey by remember { mutableStateOf(sharedPreferences.getString("tomorrow_io_key", "") ?: "") }
    var visualCrossingKey by remember { mutableStateOf(sharedPreferences.getString("visual_crossing_key", "") ?: "") }

    var tempThreshold by remember { mutableStateOf(sharedPreferences.getInt("notif_temp_threshold", 5)) }
    var windThreshold by remember { mutableStateOf(sharedPreferences.getInt("notif_wind_threshold", 15)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = onBack) { Text(stringResource(R.string.back_btn)) }
            FilledTonalButton(onClick = { context.startActivity(Intent(context, ChangeLogActivity::class.java)) }) {
                Text(stringResource(R.string.open_change_log))
            }
        }

        Text(text = stringResource(R.string.account_title), style = MaterialTheme.typography.headlineSmall)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAuthenticated && userInfo != null) {
                    Text(text = stringResource(R.string.logged_in_as), style = MaterialTheme.typography.labelMedium)
                    Text(text = userInfo.name, style = MaterialTheme.typography.titleMedium)
                    
                    val tierString = if (userInfo.subscriptionTier == "premium") stringResource(R.string.tier_premium) else if (userInfo.subscriptionTier == "freemium") stringResource(R.string.tier_freemium) else stringResource(R.string.tier_free)
                    Text(text = stringResource(R.string.subscription_tier, tierString), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    
                    Button(onClick = { locationsViewModel.syncWithCloud() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.sync_now))
                    }
                    
                    Button(onClick = { authManager.logout(); isAuthenticated = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text(stringResource(R.string.logout_button))
                    }
                } else {
                    Button(onClick = { context.startActivity(Intent(context, AuthActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.login_title))
                    }
                }
            }
        }
        
        Text(text = stringResource(R.string.theme_settings_title), style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsToggle(title = stringResource(R.string.dynamic_color_title), checked = dynamicColor, onCheckedChange = { dynamicColor = it; sharedPreferences.edit().putBoolean("dynamic_color", it).apply() })
                SettingsToggle(title = stringResource(R.string.follow_system_theme), checked = useSystemTheme, onCheckedChange = { useSystemTheme = it; sharedPreferences.edit().putBoolean("use_system_theme", it).apply() })
                SettingsToggle(title = stringResource(R.string.force_dark_mode), checked = darkModeEnabled, enabled = !useSystemTheme, onCheckedChange = { darkModeEnabled = it; sharedPreferences.edit().putBoolean("dark_mode_enabled", it).apply() })
            }
        }

        Text(text = stringResource(R.string.weather_provider_title), style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("open_meteo", "weatherapi", "tomorrow", "visualcrossing", "accuweather").forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = weatherProvider == p, onClick = { 
                            weatherProvider = p
                            sharedPreferences.edit().putString("weather_provider", p).apply() 
                        })
                        Text(p.replace("_", " ").replaceFirstChar { it.uppercase() })
                    }
                }

                if (weatherProvider == "weatherapi") {
                    OutlinedTextField(value = weatherApiKey, onValueChange = { weatherApiKey = it; sharedPreferences.edit().putString("weather_api_key", it).apply() }, label = { Text("WeatherAPI Key") }, modifier = Modifier.fillMaxWidth())
                }
                if (weatherProvider == "tomorrow") {
                    OutlinedTextField(value = tomorrowIoKey, onValueChange = { tomorrowIoKey = it; sharedPreferences.edit().putString("tomorrow_io_key", it).apply() }, label = { Text("Tomorrow.io Key") }, modifier = Modifier.fillMaxWidth())
                }
                if (weatherProvider == "visualcrossing") {
                    OutlinedTextField(value = visualCrossingKey, onValueChange = { visualCrossingKey = it; sharedPreferences.edit().putString("visual_crossing_key", it).apply() }, label = { Text("Visual Crossing Key") }, modifier = Modifier.fillMaxWidth())
                }
                
                OutlinedTextField(value = qweatherApiKey, onValueChange = { qweatherApiKey = it; sharedPreferences.edit().putString("qweather_api_key", it).apply() }, label = { Text("QWeather Key (Moon Data)") }, modifier = Modifier.fillMaxWidth())
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsToggle(title: String, subtitle: String? = null, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}
