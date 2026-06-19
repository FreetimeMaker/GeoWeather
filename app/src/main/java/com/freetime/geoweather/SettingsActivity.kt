package com.freetime.geoweather

import android.annotation.SuppressLint
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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
            val oledBlackState = sharedPreferences.collectAsState(key = "oled_black", defaultValue = false)

            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value
            
            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value, oledBlack = oledBlackState.value) {
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

@SuppressLint("ApplySharedPref")
@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPreferences = remember { 
        context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) 
    }
    val db = remember { LocationDatabase.getDatabase(context) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val locations = db.locationDao().getAllLocationsSync()
                    val root = JSONObject()
                    val array = JSONArray()
                    locations.forEach { loc ->
                        val obj = JSONObject()
                        obj.put("name", loc.name)
                        obj.put("lat", loc.latitude)
                        obj.put("lon", loc.longitude)
                        obj.put("notif_enabled", loc.notificationsEnabled)
                        obj.put("notif_time", loc.notificationTime)
                        obj.put("alert_enabled", loc.changeAlertsEnabled)
                        obj.put("alert_interval", loc.changeAlertInterval)
                        obj.put("is_default", loc.isDefault)
                        array.put(obj)
                    }
                    root.put("locations", array)
                    root.put("version", 1)

                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        OutputStreamWriter(os).use { writer ->
                            writer.write(root.toString(4))
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMsg = context.getString(R.string.export_failed)
                        Toast.makeText(context, "$errorMsg: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val content = reader.readText()
                        val root = JSONObject(content)
                        val array = root.getJSONArray("locations")
                        
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val lat = obj.getDouble("lat")
                            val lon = obj.getDouble("lon")
                            
                            val existing = db.locationDao().findByCoordinates(lat, lon)
                            if (existing == null) {
                                db.locationDao().insertLocation(LocationEntity(
                                    name = obj.getString("name"),
                                    latitude = lat,
                                    longitude = lon,
                                    notificationsEnabled = obj.optBoolean("notif_enabled", false),
                                    notificationTime = obj.optString("notif_time", "08:00"),
                                    changeAlertsEnabled = obj.optBoolean("alert_enabled", false),
                                    changeAlertInterval = obj.optString("alert_interval", "3"),
                                    isDefault = obj.optBoolean("is_default", false)
                                ))
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMsg = context.getString(R.string.import_failed)
                        Toast.makeText(context, "$errorMsg: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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

    var oledBlack by remember {
        mutableStateOf(sharedPreferences.getBoolean("oled_black", false))
    }

    var disablePrivateView by remember {
        mutableStateOf(sharedPreferences.getBoolean("disable_private_view", false))
    }

    var openExternalBrowser by remember {
        mutableStateOf(sharedPreferences.getBoolean("open_external_browser", false))
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

    val tomorrowIoApiKeyState by sharedPreferences.collectStringAsState("tomorrow_io_api_key", "")
    var tomorrowIoApiKey by remember { mutableStateOf(tomorrowIoApiKeyState) }
    LaunchedEffect(tomorrowIoApiKeyState) { tomorrowIoApiKey = tomorrowIoApiKeyState }

    val visualCrossingApiKeyState by sharedPreferences.collectStringAsState("visual_crossing_api_key", "")
    var visualCrossingApiKey by remember { mutableStateOf(visualCrossingApiKeyState) }
    LaunchedEffect(visualCrossingApiKeyState) { visualCrossingApiKey = visualCrossingApiKeyState }

    val openWeatherMapApiKeyState by sharedPreferences.collectStringAsState("open_weather_map_api_key", "")
    var openWeatherMapApiKey by remember { mutableStateOf(openWeatherMapApiKeyState) }
    LaunchedEffect(openWeatherMapApiKeyState) { openWeatherMapApiKey = openWeatherMapApiKeyState }

    val qweatherApiKeyState by sharedPreferences.collectStringAsState("qweather_api_key", "")
    var qweatherApiKey by remember { mutableStateOf(qweatherApiKeyState) }
    LaunchedEffect(qweatherApiKeyState) { qweatherApiKey = qweatherApiKeyState }

    val iconThemeState by sharedPreferences.collectStringAsState("icon_theme", "google")
    var iconTheme by remember { mutableStateOf(iconThemeState) }
    LaunchedEffect(iconThemeState) { iconTheme = iconThemeState }

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

                SettingsToggle(
                    title = stringResource(R.string.oled_black_title),
                    subtitle = stringResource(R.string.oled_black_subtitle),
                    checked = oledBlack,
                    onCheckedChange = {
                        oledBlack = it
                        sharedPreferences.edit().putBoolean("oled_black", it).apply()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(stringResource(R.string.app_icon_theme_title), style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = iconTheme == "google", onClick = {
                            iconTheme = "google"
                            sharedPreferences.edit().putString("icon_theme", "google").apply()
                        })
                        Text(stringResource(R.string.icon_theme_google))
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = iconTheme == "minimal", onClick = {
                            iconTheme = "minimal"
                            sharedPreferences.edit().putString("icon_theme", "minimal").apply()
                        })
                        Text(stringResource(R.string.icon_theme_minimal))
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = iconTheme == "retro", onClick = {
                            iconTheme = "retro"
                            sharedPreferences.edit().putString("icon_theme", "retro").apply()
                        })
                        Text(stringResource(R.string.icon_theme_retro))
                    }
                }

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
            text = stringResource(R.string.webview_settings_title),
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
                    title = stringResource(R.string.disable_private_view),
                    subtitle = stringResource(R.string.disable_private_view_subtitle),
                    checked = disablePrivateView,
                    onCheckedChange = {
                        disablePrivateView = it
                        sharedPreferences.edit().putBoolean("disable_private_view", it).apply()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SettingsToggle(
                    title = stringResource(R.string.open_external_browser),
                    subtitle = stringResource(R.string.open_external_browser_subtitle),
                    checked = openExternalBrowser,
                    onCheckedChange = {
                        openExternalBrowser = it
                        sharedPreferences.edit().putBoolean("open_external_browser", it).apply()
                    }
                )
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
                // Open-Meteo (Free, no API key required)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = weatherProvider == "open_meteo", onClick = {
                        weatherProvider = "open_meteo"
                        sharedPreferences.edit().putString("weather_provider", "open_meteo").apply()
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.provider_open_meteo))
                        Text(stringResource(R.string.provider_free_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // WeatherAPI
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = weatherProvider == "weatherapi", onClick = {
                        weatherProvider = "weatherapi"
                        sharedPreferences.edit().putString("weather_provider", "weatherapi").apply()
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.provider_weatherapi))
                        Text(stringResource(R.string.provider_premium_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (weatherProvider == "weatherapi") {
                    OutlinedTextField(
                        value = weatherApiKey,
                        onValueChange = { newValue ->
                            weatherApiKey = newValue
                            sharedPreferences.edit().putString("weather_api_key", newValue).apply()
                        },
                        label = { Text(stringResource(R.string.api_key_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Tomorrow.io
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = weatherProvider == "tomorrow_io", onClick = {
                        weatherProvider = "tomorrow_io"
                        sharedPreferences.edit().putString("weather_provider", "tomorrow_io").apply()
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tomorrow.io")
                        Text(stringResource(R.string.provider_tomorrow_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (weatherProvider == "tomorrow_io") {
                    OutlinedTextField(
                        value = tomorrowIoApiKey,
                        onValueChange = { newValue ->
                            tomorrowIoApiKey = newValue
                            sharedPreferences.edit().putString("tomorrow_io_api_key", newValue).apply()
                        },
                        label = { Text(stringResource(R.string.api_key_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Visual Crossing
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = weatherProvider == "visualcrossing", onClick = {
                        weatherProvider = "visualcrossing"
                        sharedPreferences.edit().putString("weather_provider", "visualcrossing").apply()
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Visual Crossing")
                        Text(stringResource(R.string.provider_visual_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (weatherProvider == "visualcrossing") {
                    OutlinedTextField(
                        value = visualCrossingApiKey,
                        onValueChange = { newValue ->
                            visualCrossingApiKey = newValue
                            sharedPreferences.edit().putString("visual_crossing_api_key", newValue).apply()
                        },
                        label = { Text(stringResource(R.string.api_key_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // OpenWeatherMap
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = weatherProvider == "openweathermap", onClick = {
                        weatherProvider = "openweathermap"
                        sharedPreferences.edit().putString("weather_provider", "openweathermap").apply()
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OpenWeatherMap")
                        Text(stringResource(R.string.provider_owm_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (weatherProvider == "openweathermap") {
                    OutlinedTextField(
                        value = openWeatherMapApiKey,
                        onValueChange = { newValue ->
                            openWeatherMapApiKey = newValue
                            sharedPreferences.edit().putString("open_weather_map_api_key", newValue).apply()
                        },
                        label = { Text(stringResource(R.string.api_key_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // QWeather (Moon data & astronomical data)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = weatherProvider == "qweather", onClick = {
                        weatherProvider = "qweather"
                        sharedPreferences.edit().putString("weather_provider", "qweather").apply()
                    })
                    Column(modifier = Modifier.weight(1f)) {
                        Text("QWeather")
                        Text(stringResource(R.string.provider_qweather_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (weatherProvider == "qweather") {
                    OutlinedTextField(
                        value = qweatherApiKey,
                        onValueChange = { newValue ->
                            qweatherApiKey = newValue
                            sharedPreferences.edit().putString("qweather_api_key", newValue).apply()
                        },
                        label = { Text(stringResource(R.string.api_key_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.backup_restore_title),
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
                Button(
                    onClick = { exportLauncher.launch("geoweather_backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.export_locations))
                }
                
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.import_locations))
                }
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

// Helper extension function for SharedPreferences
@SuppressLint("ApplySharedPref")
private inline fun SharedPreferences.editApply(block: SharedPreferences.Editor.() -> Unit) {
    edit().apply(block).apply()
}

