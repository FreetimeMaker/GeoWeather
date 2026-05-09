package com.freetime.geoweather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.WeatherHistoryEntity
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class WeatherDetailActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        checkNotificationPermission()

        val name = intent.getStringExtra("name") ?: getString(R.string.unknown_location)
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)

        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = sharedPreferences.collectAsState(key = "use_system_theme", defaultValue = true)
            val darkModeEnabled = sharedPreferences.collectAsState(key = "dark_mode_enabled", defaultValue = false)
            val dynamicColor = sharedPreferences.collectAsState(key = "dynamic_color", defaultValue = true)
            
            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value
            
            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value) {
                WeatherDetailScreen(
                    name = name,
                    lat = lat,
                    lon = lon,
                    onBack = { finish() }
                )
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

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    name: String,
    lat: Double,
    lon: Double,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val db = remember { LocationDatabase.getDatabase(context) }
    val sharedPreferences = remember { context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
    val authManager = remember { AuthManager.getInstance(context) }
    val weatherRepository = remember { WeatherRepository(context) }
    val userInfo = authManager.userInfo
    val isPremium = userInfo?.subscriptionTier == "premium"
    
    val tempUnit by sharedPreferences.collectStringAsState("temp_unit", "celsius")
    val windUnit by sharedPreferences.collectStringAsState("wind_unit", "kmh")
    val weatherProvider by sharedPreferences.collectStringAsState("weather_provider", "open_meteo")
    val showHistoricalData by sharedPreferences.collectAsState("show_historical_data", true)

    var weatherJson by remember { mutableStateOf<String?>(null) }
    var aqiJson by remember { mutableStateOf<String?>(null) }
    var moonPhaseName by remember { mutableStateOf<String?>(null) }
    var forecastList by remember { mutableStateOf<List<DailyForecast>>(emptyList()) }
    var hourlyForecastList by remember { mutableStateOf<List<HourlyForecast>>(emptyList()) }
    var historicalData by remember { mutableStateOf<List<DailyForecast>>(emptyList()) }
    var currentTemp by remember { mutableStateOf<Double?>(null) }
    var currentConditionCode by remember { mutableStateOf<Int?>(null) }
    var currentIsDay by remember { mutableStateOf(true) }
    var responseProvider by remember { mutableStateOf("open_meteo") }
    
    val localHistory by db.weatherHistoryDao().getHistoryForLocation(name).observeAsState(initial = emptyList())
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var selectedDayIndex by remember { mutableStateOf(-1) }

    suspend fun refreshWeatherData(forceRefresh: Boolean = false) {
        try {
            isRefreshing = true
            errorMessage = null
            val entity = withContext(Dispatchers.IO) { db.locationDao().findByCoordinates(lat, lon) }
            val currentTime = System.currentTimeMillis()
            
            val lastUpdated = entity?.lastUpdated
            val dataAgeMinutes = if (lastUpdated != null) (currentTime - lastUpdated) / (1000 * 60) else Long.MAX_VALUE

            val forecastDays = if (isPremium) 7 else 3

            if (!forceRefresh && entity?.weatherData != null && dataAgeMinutes < 30) {
                val json = entity.weatherData
                weatherJson = json
                
                // Parse basic info from cache immediately
                try {
                    val root = JSONObject(json)
                    val current = if (root.has("current")) root.getJSONObject("current") else root.getJSONObject("current_weather")
                    currentTemp = if (current.has("temperature_2m")) current.getDouble("temperature_2m") else current.getDouble("temperature")
                    currentConditionCode = current.getInt("weathercode")
                    currentIsDay = current.optInt("is_day", 1) == 1
                    responseProvider = "open_meteo" // Assuming cache is Open-Meteo
                } catch (_: Exception) {}

                scope.launch { 
                    val result = weatherRepository.getWeatherData(lat, lon, forecastDays)
                    if (result is WeatherRepository.WeatherDataResult.Success) {
                        weatherJson = result.rawJson
                        forecastList = result.dailyForecast
                        hourlyForecastList = result.hourlyForecast
                        aqiJson = result.aqiJson
                        moonPhaseName = result.moonPhase
                        currentTemp = result.temp
                        currentConditionCode = result.weatherCode
                        currentIsDay = result.isDay
                        responseProvider = result.provider
                        errorMessage = null
                    }
                }
            } else {
                val result = weatherRepository.getWeatherData(lat, lon, forecastDays)
                
                when (result) {
                    is WeatherRepository.WeatherDataResult.Success -> {
                        weatherJson = result.rawJson
                        forecastList = result.dailyForecast
                        hourlyForecastList = result.hourlyForecast
                        aqiJson = result.aqiJson
                        moonPhaseName = result.moonPhase
                        currentTemp = result.temp
                        currentConditionCode = result.weatherCode
                        currentIsDay = result.isDay
                        responseProvider = result.provider
                        
                        if (result.provider == "open_meteo") {
                            entity?.copy(weatherData = result.rawJson, lastUpdated = currentTime)?.let {
                                withContext(Dispatchers.IO) { db.locationDao().updateLocation(it) }
                            }
                        }
                        errorMessage = null
                    }
                    is WeatherRepository.WeatherDataResult.Error -> {
                        errorMessage = result.message
                    }
                }
                
                if (showHistoricalData) {
                    historicalData = weatherRepository.getHistoricalData(lat, lon, 3)
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: context.getString(R.string.error_loading_weather)
        } finally {
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { refreshWeatherData() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc)) }
                },
                actions = {
                    IconButton(onClick = { scope.launch { refreshWeatherData(true) } }, enabled = !isRefreshing) {
                        if (isRefreshing) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_nav_desc))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { scope.launch { refreshWeatherData(true) } }) {
                            Text(stringResource(R.string.refresh_nav_desc))
                        }
                    }
                }
            }

            if (weatherJson == null && isRefreshing) {
                item { 
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator() 
                    }
                }
            }

            if (weatherJson == null && !isRefreshing && errorMessage == null) {
                item {
                    Column(
                        modifier = Modifier.fillParentMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.no_locations_msg))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { scope.launch { refreshWeatherData(true) } }) {
                            Text(stringResource(R.string.refresh_nav_desc))
                        }
                    }
                }
            }

            currentTemp?.let { temp ->
                val displayTemp = if (tempUnit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()

                item {
                    val tempSuffix = if (tempUnit == "fahrenheit") stringResource(R.string.temp_f_suffix) else stringResource(R.string.temp_c_suffix)

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        currentConditionCode?.let { code ->
                            Icon(
                                painter = painterResource(id = WeatherIconMapper.getIcon(code, responseProvider, currentIsDay)),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = Color.Unspecified
                            )
                            Text("$displayTemp$tempSuffix", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                            Text(WeatherCodes.getDescription(code, context, responseProvider), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                weatherJson?.let { json ->
                    item {
                        WeatherDetailsGrid(JSONObject(json), tempUnit, windUnit, responseProvider)
                    }
                }
// ... rest of the items ...


                if (hourlyForecastList.isNotEmpty()) {
                    item {
                        HourlyForecastSection(hourlyForecastList, tempUnit, weatherProvider)
                    }
                }

                if (forecastList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            val label = if (isPremium) stringResource(R.string.SevenDayForecastTXT) else stringResource(R.string.forecast_3day_label)
                            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            forecastList.forEachIndexed { index, forecast ->
                                ForecastItemRow(
                                    forecast = forecast,
                                    tempUnit = tempUnit,
                                    isSelected = selectedDayIndex == index,
                                    weatherProvider = weatherProvider,
                                    onClick = { selectedDayIndex = if (selectedDayIndex == index) -1 else index }
                                )
                            }
                        }
                    }
                }

                if (moonPhaseName != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🌙", fontSize = 24.sp)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(text = stringResource(R.string.MoonPhaseTXT), style = MaterialTheme.typography.labelMedium)
                                    Text(text = moonPhaseName ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (historicalData.isNotEmpty()) {
                    item {
                        HistoricalTrendsSection(historicalData, tempUnit)
                    }
                }

                if (localHistory.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(stringResource(R.string.weather_history_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            localHistory.take(5).forEach { entry ->
                                HistoryItemRow(entry, tempUnit)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun ForecastItemRow(
    forecast: DailyForecast,
    tempUnit: String,
    isSelected: Boolean,
    weatherProvider: String = "open_meteo",
    onClick: () -> Unit
) {
    val tempSuffix = stringResource(R.string.temp_deg_suffix)
    val displayMax = if (tempUnit == "fahrenheit") (forecast.tempMax * 9/5 + 32).toInt() else forecast.tempMax.toInt()
    val displayMin = if (tempUnit == "fahrenheit") (forecast.tempMin * 9/5 + 32).toInt() else forecast.tempMin.toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    ) {
        Column {
            ListItem(
                headlineContent = { Text(forecast.date) },
                supportingContent = { Text(WeatherCodes.getDescription(forecast.weatherCode, LocalContext.current, weatherProvider)) },
                trailingContent = { Text("$displayMax$tempSuffix / $displayMin$tempSuffix", fontWeight = FontWeight.Bold) },
                leadingContent = { 
                    Icon(
                        painter = painterResource(WeatherIconMapper.getIcon(forecast.weatherCode, weatherProvider, true)), 
                        contentDescription = null, 
                        modifier = Modifier.size(32.dp), 
                        tint = Color.Unspecified
                    ) 
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            AnimatedVisibility(visible = isSelected) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailItemSmall(label = stringResource(R.string.trend_max), value = "$displayMax$tempSuffix")
                    DetailItemSmall(label = stringResource(R.string.trend_min), value = "$displayMin$tempSuffix")
                }
            }
        }
    }
}

@Composable
fun DetailItemSmall(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun HistoricalTrendsSection(data: List<DailyForecast>, tempUnit: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.historical_trends_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val points = data.size
                        if (points < 2) return@Canvas

                        val maxTemp = data.maxOf { it.tempMax }.toFloat()
                        val minTemp = data.minOf { it.tempMin }.toFloat()
                        val range = (maxTemp - minTemp).coerceAtLeast(1f)

                        val pathMax = Path()
                        val pathMin = Path()

                        data.forEachIndexed { index, forecast ->
                            val x = index * (width / (points - 1))
                            val yMax = height - ((forecast.tempMax.toFloat() - minTemp) / range * height)
                            val yMin = height - ((forecast.tempMin.toFloat() - minTemp) / range * height)

                            if (index == 0) {
                                pathMax.moveTo(x, yMax)
                                pathMin.moveTo(x, yMin)
                            } else {
                                pathMax.lineTo(x, yMax)
                                pathMin.lineTo(x, yMin)
                            }
                        }

                        drawPath(pathMax, color = Color.Red, style = Stroke(width = 3.dp.toPx()))
                        drawPath(pathMin, color = Color.Blue, style = Stroke(width = 3.dp.toPx()))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    data.forEach { forecast ->
                        Text(text = forecast.date.split(",").last().trim(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.trend_min), color = Color.Blue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.trend_max), color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WeatherDetailsGrid(weatherObj: JSONObject, tempUnit: String, windUnit: String, provider: String) {
    val (wind, feelsLike, humidity) = if (provider == "weatherapi") {
        val current = weatherObj.getJSONObject("current")
        Triple(current.getDouble("wind_kph"), current.getDouble("feelslike_c"), current.getInt("humidity"))
    } else {
        val root = weatherObj
        val current = if (root.has("current")) root.getJSONObject("current") else root.getJSONObject("current_weather")
        val hourly = root.optJSONObject("hourly")
        val currentIndex = if (hourly != null) getCurrentHourIndex(hourly.getJSONArray("time")) else -1
        val windVal = if (current.has("windspeed_10m")) current.getDouble("windspeed_10m") else current.getDouble("windspeed")
        val feelsVal = if (currentIndex >= 0) hourly?.getJSONArray("apparent_temperature")?.optDouble(currentIndex, current.optDouble("temperature_2m", 0.0)) ?: current.optDouble("temperature_2m", 0.0) else current.optDouble("temperature_2m", 0.0)
        val humVal = if (currentIndex >= 0) hourly?.getJSONArray("relativehumidity_2m")?.optInt(currentIndex, 0) ?: 0 else 0
        Triple(windVal, feelsVal, humVal)
    }

    val displayWind = if (windUnit == "mph") (wind * 0.621371).toInt() else wind.toInt()
    val windSuffix = if (windUnit == "mph") stringResource(R.string.wind_mph_suffix) else stringResource(R.string.wind_kmh_suffix)
    val displayFeelsLike = if (tempUnit == "fahrenheit") (feelsLike * 9/5 + 32).toInt() else feelsLike.toInt()
    val tempSuffix = if (tempUnit == "fahrenheit") stringResource(R.string.temp_f_suffix) else stringResource(R.string.temp_c_suffix)
    val humiditySuffix = stringResource(R.string.humidity_suffix)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DetailItem(label = stringResource(R.string.wind_label), value = "$displayWind$windSuffix")
                DetailItem(label = stringResource(R.string.feels_like_label), value = "$displayFeelsLike$tempSuffix")
                DetailItem(label = stringResource(R.string.humidity_label), value = "$humidity$humiditySuffix")
            }
            val (sunrise, sunset) = if (provider == "weatherapi") {
                val astro = weatherObj.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONObject("astro")
                astro.getString("sunrise") to astro.getString("sunset")
            } else if (weatherObj.has("daily")) {
                val daily = weatherObj.getJSONObject("daily")
                formatTime(daily.getJSONArray("sunrise").getString(0)) to formatTime(daily.getJSONArray("sunset").getString(0))
            } else { null to null }

            if (sunrise != null && sunset != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DetailItem(label = stringResource(R.string.sunrise_label), value = sunrise)
                    DetailItem(label = stringResource(R.string.sunset_label), value = sunset)
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HourlyForecastSection(list: List<HourlyForecast>, tempUnit: String, weatherProvider: String = "open_meteo") {
    val tempSuffix = if (tempUnit == "fahrenheit") stringResource(R.string.temp_f_suffix) else stringResource(R.string.temp_c_suffix)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.hourly_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { forecast ->
                val displayTemp = if (tempUnit == "fahrenheit") (forecast.temp * 9/5 + 32).toInt() else forecast.temp.toInt()
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(forecast.time, style = MaterialTheme.typography.labelMedium)
                        Icon(painter = painterResource(WeatherIconMapper.getIcon(forecast.weatherCode, weatherProvider, true)), contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Unspecified)
                        Text("$displayTemp$tempSuffix", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(entry: WeatherHistoryEntity, tempUnit: String) {
    val dateString = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    val displayTemp = if (tempUnit == "fahrenheit") (entry.temperature * 9/5 + 32).toInt() else entry.temperature.toInt()
    val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = entry.location, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "$displayTemp$tempSuffix", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = entry.conditions ?: "", style = MaterialTheme.typography.bodyMedium)
                Text(text = dateString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

fun getCurrentHourIndex(timesArray: JSONArray): Int {
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    for (i in 0 until timesArray.length()) {
        val date = try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(timesArray.getString(i)) } catch(_:Exception) { null } ?: continue
        val cal = Calendar.getInstance().apply { time = date }
        if (cal.get(Calendar.HOUR_OF_DAY) == currentHour && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) return i
    }
    return 0
}

fun formatTime(timeString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(timeString)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())
    } catch (_: Exception) { timeString.takeLast(5) }
}
