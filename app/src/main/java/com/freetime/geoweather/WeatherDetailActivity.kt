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
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
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
    private val weatherCodeState = mutableStateOf(0)
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
            val oledBlack = sharedPreferences.collectAsState(key = "oled_black", defaultValue = false)
            
            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value
            
            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value, oledBlack = oledBlack.value) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedWeatherBackground(weatherCode = weatherCodeState.value)
                    
                    WeatherDetailScreen(
                        name = name,
                        lat = lat,
                        lon = lon,
                        onBack = { finish() },
                        onWeatherCodeUpdate = { weatherCodeState.value = it }
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
    onBack: () -> Unit,
    onWeatherCodeUpdate: (Int) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val db = remember { LocationDatabase.getDatabase(context) }
    val sharedPreferences = remember { context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
    
    val tempUnit by sharedPreferences.collectStringAsState("temp_unit", "celsius")
    val windUnit by sharedPreferences.collectStringAsState("wind_unit", "kmh")
    val errorLoadingWeather = stringResource(R.string.error_loading_weather)

    var weatherJson by remember { mutableStateOf<String?>(null) }
    var aqiJson by remember { mutableStateOf<String?>(null) }
    var moonPhaseName by remember { mutableStateOf<String?>(null) }
    var healthData by remember { mutableStateOf<HealthData?>(null) }
    var forecastList by remember { mutableStateOf<List<DailyForecast>>(emptyList()) }
    var hourlyForecastList by remember { mutableStateOf<List<HourlyForecast>>(emptyList()) }
    var historicalData by remember { mutableStateOf<List<DailyForecast>>(emptyList()) }
    var onThisDayData by remember { mutableStateOf<DailyForecast?>(null) }
    var earthquakeList by remember { mutableStateOf<List<Earthquake>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var selectedDayIndex by remember { mutableStateOf(-1) }

    suspend fun refreshWeatherData(forceRefresh: Boolean = false) {
        try {
            isRefreshing = true
            val entity = withContext(Dispatchers.IO) { db.locationDao().findByCoordinates(lat, lon) }
            val currentTime = System.currentTimeMillis()
            
            val lastUpdated = entity?.lastUpdated
            val dataAgeMinutes = if (lastUpdated != null) (currentTime - lastUpdated) / (1000 * 60) else Long.MAX_VALUE
            val cachedWeatherData = entity?.weatherData
            val cacheHasPrecipitation = cachedWeatherData?.let { hasPrecipitationData(it) } ?: false

            var json: String? = null
            var aqiJsonResponse: String? = null
            val aqiUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&hourly=european_aqi,uv_index,alnus_pollen,betula_pollen,grass_pollen&timezone=auto"

            if (!forceRefresh && cachedWeatherData != null && cacheHasPrecipitation && dataAgeMinutes < 30) {
                json = cachedWeatherData
                weatherJson = json
            } else {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,pressure_msl&hourly=temperature_2m,weather_code,relative_humidity_2m,pressure_msl,apparent_temperature,precipitation,precipitation_probability,visibility,cloud_base,wind_speed_10m,wind_gusts_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_sum,precipitation_probability_max,wind_speed_10m_max&forecast_days=16&timezone=auto"
                val histUrl = "https://archive-api.open-meteo.com/v1/archive?latitude=$lat&longitude=$lon&start_date=${getYesterdayDate(-7)}&end_date=${getYesterdayDate(0)}&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
                val onThisDayUrl = "https://archive-api.open-meteo.com/v1/archive?latitude=$lat&longitude=$lon&start_date=${getOneYearAgoDate()}&end_date=${getOneYearAgoDate()}&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto"

                json = withContext(Dispatchers.IO) { httpGet(url) }

                try {
                    val histJson = withContext(Dispatchers.IO) { httpGet(histUrl) }
                    historicalData = parseForecastData(histJson)
                    
                    val onThisDayJson = withContext(Dispatchers.IO) { httpGet(onThisDayUrl) }
                    onThisDayData = parseForecastData(onThisDayJson).firstOrNull()

                    val eqUrl = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&latitude=$lat&longitude=$lon&maxradiuskm=500&limit=5"
                    val eqJson = withContext(Dispatchers.IO) { httpGet(eqUrl) }
                    earthquakeList = parseEarthquakeData(eqJson)
                } catch (_: Exception) {}
            }

            aqiJsonResponse = withContext(Dispatchers.IO) { try { httpGet(aqiUrl) } catch (e: Exception) { null } }
            healthData = parseHealthData(aqiJsonResponse)

            entity?.copy(weatherData = json, lastUpdated = currentTime)?.let {
                withContext(Dispatchers.IO) { db.locationDao().updateLocation(it) }
            }

            weatherJson = json
            aqiJson = aqiJsonResponse
            if (json != null) {
                try {
                    val jsonObj = JSONObject(json)
                    val sunrise = jsonObj.getJSONObject("daily").getJSONArray("sunrise").optString(0)
                    val sunset = jsonObj.getJSONObject("daily").getJSONArray("sunset").optString(0)
                    WeatherIconMapper.setSunTimes(sunrise, sunset)
                } catch (e: Exception) {}
                
                forecastList = parseForecastData(json)
                hourlyForecastList = parseHourlyForecastData(json)
            }
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message ?: errorLoadingWeather
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
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }

            weatherJson?.let { json ->
                val jsonObj = JSONObject(json)
                val current = jsonObj.optJSONObject("current") ?: jsonObj.optJSONObject("current_weather")
                val temperature = current?.optDouble("temperature_2m") ?: current?.optDouble("temperature") ?: 0.0
                val weatherCode = current?.let {
                    when {
                        it.has("weather_code") -> it.getInt("weather_code")
                        it.has("weathercode") -> it.getInt("weathercode")
                        else -> 0
                    }
                } ?: 0
                onWeatherCodeUpdate(weatherCode)
                val temp = temperature
                
                val displayTemp = if (tempUnit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()

                item {
                    val tempSuffix = if (tempUnit == "fahrenheit") stringResource(R.string.temp_f_suffix) else stringResource(R.string.temp_c_suffix)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = WeatherIconMapper.getWeatherIcon(weatherCode, "google")),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = Color.Unspecified
                        )
                        Text("$displayTemp$tempSuffix", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                        Text(WeatherCodes.getDescription(weatherCode, context), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    WeatherAlertsSection(weatherCode)
                }

                item {
                    WeatherDetailsGrid(jsonObj, healthData, tempUnit, windUnit, lat, lon)
                }

                if (earthquakeList.isNotEmpty()) {
                    item {
                        EarthquakeSection(earthquakeList)
                    }
                }

                item {
                    HourlyWeatherChart(hourlyForecastList, tempUnit, windUnit)
                }

                item {
                    ActivityScoreSection(jsonObj, healthData)
                }

                item {
                    HourlyForecastSection(hourlyForecastList, tempUnit)
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(stringResource(R.string.forecast_7day_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        forecastList.forEachIndexed { index, forecast ->
                            ForecastItemRow(
                                forecast = forecast,
                                tempUnit = tempUnit,
                                isSelected = selectedDayIndex == index,
                                onClick = { selectedDayIndex = if (selectedDayIndex == index) -1 else index }
                            )
                        }
                    }
                }

                if (healthData != null) {
                    item { HealthDetailSection(healthData!!) }
                }

                if (moonPhaseName != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = getMoonPhaseEmoji(moonPhaseName ?: ""), fontSize = 24.sp)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(text = stringResource(R.string.MoonPhaseTXT), style = MaterialTheme.typography.labelMedium)
                                    Text(text = moonPhaseName ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.weight(1f))
                                MoonPhaseIcon(phaseName = moonPhaseName ?: "")
                            }
                        }
                    }
                }

                if (onThisDayData != null) {
                    item {
                        OnThisDaySection(onThisDayData!!, tempUnit)
                    }
                }

                if (historicalData.isNotEmpty()) {
                    item { HistoricalTrendsSection(historicalData, tempUnit) }
                }
                
                item { Spacer(Modifier.height(32.dp)) }
            }

            if (weatherJson == null && !isRefreshing) {
                item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            }
        }
    }
}

@Composable
fun WeatherAlertsSection(weatherCode: Int) {
    val alert = when (weatherCode) {
        in 95..99 -> stringResource(R.string.alert_thunderstorm)
        in 71..86 -> stringResource(R.string.alert_snow)
        else -> null
    }

    if (alert != null) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.weather_alerts_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(alert, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
fun AnimatedWeatherBackground(weatherCode: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_bg")
    val bgColor = when {
        weatherCode == 0 -> Color(0xFF87CEEB)
        weatherCode in 1..3 -> Color(0xFFB0C4DE)
        weatherCode in 51..67 -> Color(0xFF708090)
        weatherCode in 71..86 -> Color(0xFFE6E6FA)
        else -> MaterialTheme.colorScheme.background
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor.copy(alpha = 0.4f))) {
        if (weatherCode in 51..67 || weatherCode in 80..82) RainEffect(infiniteTransition)
        else if (weatherCode in 71..86 || weatherCode in 85..86) SnowEffect(infiniteTransition)
    }
}

@Composable
fun RainEffect(transition: InfiniteTransition) {
    val rainY = transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rain"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        for (i in 0 until 50) {
            val x = (i * 20f) % width
            val y = (rainY.value + (i * 100f)) % size.height
            drawLine(color = Color.Blue.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(x, y), end = androidx.compose.ui.geometry.Offset(x, y + 20f), strokeWidth = 2f)
        }
    }
}

@Composable
fun SnowEffect(transition: InfiniteTransition) {
    val snowY = transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "snow"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        for (i in 0 until 30) {
            val x = (i * 40f) % width
            val y = (snowY.value + (i * 150f)) % size.height
            drawCircle(color = Color.White.copy(alpha = 0.6f), radius = 5f, center = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}

@Composable
fun ForecastItemRow(forecast: DailyForecast, tempUnit: String, isSelected: Boolean, onClick: () -> Unit) {
    val tempSuffix = stringResource(R.string.temp_deg_suffix)
    val displayMax = if (tempUnit == "fahrenheit") (forecast.tempMax * 9/5 + 32).toInt() else forecast.tempMax.toInt()
    val displayMin = if (tempUnit == "fahrenheit") (forecast.tempMin * 9/5 + 32).toInt() else forecast.tempMin.toInt()
    val rainText = formatRainAmount(forecast.precipitationMm)
    val precipitationText = if (forecast.precipitationChance > 0) "$rainText · ${forecast.precipitationChance}%" else rainText

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
    ) {
        Column {
            ListItem(
                headlineContent = { Text(forecast.date) },
                supportingContent = { Text(WeatherCodes.getDescription(forecast.weatherCode, LocalContext.current)) },
                trailingContent = { Text("$displayMax$tempSuffix / $displayMin$tempSuffix", fontWeight = FontWeight.Bold) },
                leadingContent = { 
                    Icon(painter = painterResource(WeatherIconMapper.getWeatherIcon(forecast.weatherCode, "google")), contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Unspecified)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            AnimatedVisibility(visible = isSelected) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 64.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailItemSmall(label = stringResource(R.string.trend_max), value = "$displayMax$tempSuffix")
                    DetailItemSmall(label = stringResource(R.string.trend_min), value = "$displayMin$tempSuffix")
                    DetailItemSmall(label = stringResource(R.string.rain_label), value = precipitationText)
                }
            }
        }
    }
}

@Composable
fun OnThisDaySection(data: DailyForecast, tempUnit: String) {
    val tempSuffix = stringResource(R.string.temp_deg_suffix)
    val displayMax = if (tempUnit == "fahrenheit") (data.tempMax * 9/5 + 32).toInt() else data.tempMax.toInt()
    val displayMin = if (tempUnit == "fahrenheit") (data.tempMin * 9/5 + 32).toInt() else data.tempMin.toInt()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.on_this_day_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            ListItem(
                headlineContent = { Text(data.date) },
                supportingContent = { Text(WeatherCodes.getDescription(data.weatherCode, LocalContext.current)) },
                trailingContent = { Text("$displayMax$tempSuffix / $displayMin$tempSuffix", fontWeight = FontWeight.Bold) },
                leadingContent = { 
                    Icon(
                        painter = painterResource(WeatherIconMapper.getWeatherIcon(data.weatherCode, "google")),
                        contentDescription = null, 
                        modifier = Modifier.size(32.dp), 
                        tint = Color.Unspecified
                    ) 
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

fun getOneYearAgoDate(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.YEAR, -1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
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
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
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
                            if (index == 0) { pathMax.moveTo(x, yMax); pathMin.moveTo(x, yMin) } else { pathMax.lineTo(x, yMax); pathMin.lineTo(x, yMin) }
                        }
                        drawPath(pathMax, color = Color.Red, style = Stroke(width = 3.dp.toPx()))
                        drawPath(pathMin, color = Color.Blue, style = Stroke(width = 3.dp.toPx()))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.trend_min), color = Color.Blue, style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.trend_max), color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun WeatherDetailsGrid(weatherObj: JSONObject, healthData: HealthData?, tempUnit: String, windUnit: String, lat: Double, lon: Double) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    
    val current = weatherObj.optJSONObject("current") ?: weatherObj.optJSONObject("current_weather")
    val legacyCurrent = weatherObj.optJSONObject("current_weather")
    val hourly = weatherObj.optJSONObject("hourly")
    val currentIndex = if (hourly != null) getCurrentHourIndex(hourly.getJSONArray("time")) else -1
    
    val currentTemp = current?.optDouble("temperature_2m") ?: legacyCurrent?.optDouble("temperature", 0.0) ?: 0.0
    val windVal = current?.optDouble("wind_speed_10m") ?: legacyCurrent?.optDouble("windspeed", 0.0) ?: 0.0
    val feelsVal = if (currentIndex >= 0) hourly?.optJSONArray("apparent_temperature")?.optDouble(currentIndex, currentTemp) ?: currentTemp else currentTemp
    val humVal = if (currentIndex >= 0) (hourly?.optJSONArray("relative_humidity_2m") ?: hourly?.optJSONArray("relativehumidity_2m"))?.optInt(currentIndex, 0) ?: 0 else 0
    val dirVal = current?.optDouble("wind_direction_10m") ?: legacyCurrent?.optDouble("winddirection", 0.0) ?: 0.0
    val gustsVal = current?.optDouble("wind_gusts_10m") ?: 0.0
    val pressureVal = current?.optDouble("pressure_msl") ?: legacyCurrent?.optDouble("pressure", 1013.25) ?: 1013.25
    
    val visibilityVal = hourly?.optJSONArray("visibility")?.optDouble(currentIndex, 10.0) ?: 10.0
    val cloudBaseVal = hourly?.optJSONArray("cloud_base")?.optDouble(currentIndex, 0.0) ?: 0.0
    
    val pressureTrend = calculatePressureTrend(hourly, currentIndex, context)

    val altitude = weatherObj.optDouble("elevation", 0.0)
    val timezone = weatherObj.optString("timezone", "UTC")
    val timezoneAbbr = weatherObj.optString("timezone_abbreviation", "")

    val windSuffix = if (windUnit == "mph") stringResource(R.string.wind_mph_suffix) else stringResource(R.string.wind_kmh_suffix)
    val displayWind = if (windUnit == "mph") (windVal * 0.621371).toInt() else windVal.toInt()
    val displayFeelsLike = if (tempUnit == "fahrenheit") (feelsVal * 9/5 + 32).toInt() else feelsVal.toInt()
    val tempSuffix = if (tempUnit == "fahrenheit") stringResource(R.string.temp_f_suffix) else stringResource(R.string.temp_c_suffix)
    val displayGusts = if (windUnit == "mph") (gustsVal * 0.621371).toInt() else gustsVal.toInt()

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DetailItem(label = stringResource(R.string.wind_label), value = "$displayWind$windSuffix")
                DetailItem(label = stringResource(R.string.feels_like_label), value = "$displayFeelsLike$tempSuffix")
                DetailItem(label = stringResource(R.string.humidity_label), value = "$humVal%")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DetailItem(label = stringResource(R.string.altitude_label), value = "${altitude.toInt()} m")
                DetailItem(label = stringResource(R.string.timezone_label), value = "$timezone ($timezoneAbbr)")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DetailItem(label = stringResource(R.string.visibility_label), value = String.format(locale, "%.1f km", visibilityVal / 1000.0))
                DetailItem(label = stringResource(R.string.cloud_base_label), value = String.format(locale, "%.0f m", cloudBaseVal))
                DetailItem(label = stringResource(R.string.pressure_trend_label), value = pressureTrend)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.wind_direction_label), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    WindCompass(direction = dirVal.toFloat())
                }
                DetailItem(label = stringResource(R.string.gusts_label), value = "$displayGusts$windSuffix")
                if (healthData?.uvIndex != null) {
                    DetailItem(label = "UV", value = String.format(locale, "%.1f", healthData.uvIndex), valueColor = getUvIndexColor(healthData.uvIndex))
                }
            }
            if (weatherObj.has("daily") || healthData?.europeanAqi != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (weatherObj.has("daily")) {
                        val daily = weatherObj.getJSONObject("daily")
                        val sunrise = formatTime(daily.getJSONArray("sunrise").getString(0))
                        val sunset = formatTime(daily.getJSONArray("sunset").getString(0))
                        DetailItem(label = stringResource(R.string.sunrise_label), value = sunrise)
                        DetailItem(label = stringResource(R.string.sunset_label), value = sunset)
                    }
                    healthData?.europeanAqi?.let { aqi ->
                        DetailItem(label = stringResource(R.string.air_label), value = aqi.toInt().toString(), valueColor = getEuropeanAqiColor(aqi))
                    }
                }
            }
            if (weatherObj.has("daily")) {
                val daily = weatherObj.getJSONObject("daily")
                val sunrise = daily.getJSONArray("sunrise").getString(0)
                val sunset = daily.getJSONArray("sunset").getString(0)
                val goldenHour = calculateGoldenHour(sunrise, sunset)
                val blueHour = calculateBlueHour(sunrise, sunset)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DetailItem(label = stringResource(R.string.golden_hour_label), value = goldenHour)
                    DetailItem(label = stringResource(R.string.blue_hour_label), value = blueHour)
                }
            }
            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.windy.com/?$lat,$lon,8"))) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                Text(stringResource(R.string.open_weather_radar))
            }
        }
    }
}

fun calculatePressureTrend(hourly: JSONObject?, index: Int, context: Context): String {
    if (hourly == null || index < 3) return context.getString(R.string.pressure_stable)
    return try {
        val array = hourly.getJSONArray("pressure_msl")
        val current = array.getDouble(index)
        val past = array.getDouble(index - 3)
        val diff = current - past
        when {
            diff > 1.0 -> "↑ " + context.getString(R.string.pressure_rising)
            diff < -1.0 -> "↓ " + context.getString(R.string.pressure_falling)
            else -> "→ " + context.getString(R.string.pressure_stable)
        }
    } catch (_: Exception) { context.getString(R.string.pressure_stable) }
}

@Composable
fun EarthquakeSection(list: List<Earthquake>) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = stringResource(R.string.earthquake_monitor_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                list.forEach { eq ->
                    ListItem(
                        headlineContent = { Text(eq.place) },
                        trailingContent = { Text(String.format(locale, "Mag %.1f", eq.mag), fontWeight = FontWeight.Bold, color = if (eq.mag > 4.0) Color.Red else MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

data class Earthquake(val mag: Double, val place: String, val time: Long)

fun parseEarthquakeData(json: String): List<Earthquake> {
    val list = mutableListOf<Earthquake>()
    try {
        val obj = JSONObject(json)
        val features = obj.getJSONArray("features")
        for (i in 0 until features.length()) {
            val prop = features.getJSONObject(i).getJSONObject("properties")
            list.add(Earthquake(mag = prop.getDouble("mag"), place = prop.getString("place"), time = prop.getLong("time")))
        }
    } catch (_: Exception) {}
    return list
}

@Composable
fun WindCompass(direction: Float) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        drawCircle(color = Color.Gray.copy(alpha = 0.3f), radius = radius)
        val angleRad = Math.toRadians((direction - 90).toDouble())
        val arrowLength = radius * 0.8f
        val end = androidx.compose.ui.geometry.Offset((center.x + arrowLength * Math.cos(angleRad)).toFloat(), (center.y + arrowLength * Math.sin(angleRad)).toFloat())
        drawLine(color = Color.Red, start = center, end = end, strokeWidth = 3.dp.toPx())
    }
}

@Composable
fun ActivityScoreSection(weatherObj: JSONObject, healthData: HealthData?) {
    val current = weatherObj.optJSONObject("current") ?: weatherObj.optJSONObject("current_weather")
    val temp = current?.optDouble("temperature_2m") ?: current?.optDouble("temperature") ?: 20.0
    val wind = current?.optDouble("wind_speed_10m") ?: current?.optDouble("windspeed") ?: 0.0
    val rain = weatherObj.optJSONObject("hourly")?.optJSONArray("precipitation_probability")?.optInt(0, 0) ?: 0
    val pollen = (healthData?.alnusPollen ?: 0.0) + (healthData?.betulaPollen ?: 0.0) + (healthData?.grassPollen ?: 0.0)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = stringResource(R.string.activity_score_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ActivityItem(stringResource(R.string.score_jogging), calculateJoggingScore(temp, rain, pollen))
                    ActivityItem(stringResource(R.string.score_laundry), calculateLaundryScore(temp, wind, rain))
                    ActivityItem(stringResource(R.string.score_gardening), calculateGardeningScore(temp, rain))
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.jogging_score_desc), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.laundry_score_desc), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.gardening_score_desc), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ActivityItem(label: String, score: Int) {
    val (text, color) = when {
        score >= 8 -> stringResource(R.string.score_perfect) to Color(0xFF2E7D32)
        score >= 5 -> stringResource(R.string.score_good) to Color(0xFFF9A825)
        score >= 3 -> stringResource(R.string.score_poor) to Color(0xFFEF6C00)
        else -> stringResource(R.string.score_avoid) to Color(0xFFC62828)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

fun calculateJoggingScore(temp: Double, rain: Int, pollen: Double): Int {
    var score = 10
    if (temp > 30 || temp < 0) score -= 3
    if (rain > 20) score -= 4
    if (pollen > 50) score -= 3
    return score.coerceIn(0, 10)
}

fun calculateLaundryScore(temp: Double, wind: Double, rain: Int): Int {
    var score = 5
    if (temp > 20) score += 2
    if (wind > 15) score += 3
    if (rain > 10) score -= 10
    return score.coerceIn(0, 10)
}

fun calculateGardeningScore(temp: Double, rain: Int): Int {
    var score = 10
    if (temp < 5) score -= 5 // Frost danger
    if (rain > 50) score -= 5 // Too wet
    return score.coerceIn(0, 10)
}

@Composable
fun HourlyWeatherChart(list: List<HourlyForecast>, tempUnit: String, windUnit: String) {
    if (list.isEmpty()) return
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.chart_24h_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val width = size.width
                val height = size.height
                val points = list.size.coerceAtMost(24)
                val temps = list.map { it.temp }.take(points)
                val rain = list.map { it.precipitationChance.toFloat() }.take(points)
                val maxTemp = temps.maxOrNull()?.toFloat() ?: 0f
                val minTemp = temps.minOrNull()?.toFloat() ?: 0f
                val tempRange = (maxTemp - minTemp).coerceAtLeast(1f)
                rain.forEachIndexed { index, chance ->
                    val x = index * (width / (points - 1))
                    val barHeight = (chance / 100f) * height
                    drawRect(color = Color.Blue.copy(alpha = 0.2f), topLeft = androidx.compose.ui.geometry.Offset(x - 5f, height - barHeight), size = androidx.compose.ui.geometry.Size(10f, barHeight))
                }
                val path = Path()
                temps.forEachIndexed { index, temp ->
                    val x = index * (width / (points - 1))
                    val y = height - ((temp.toFloat() - minTemp) / tempRange * height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = Color.Red, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun HourlyForecastSection(list: List<HourlyForecast>, tempUnit: String) {
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
                        Icon(painter = painterResource(WeatherIconMapper.getWeatherIcon(forecast.weatherCode, "google")), contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Unspecified)
                        Text("$displayTemp$tempSuffix", style = MaterialTheme.typography.titleSmall)
                        Text(text = stringResource(R.string.rain_amount_label, formatRainAmount(forecast.precipitationMm)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

private fun httpGet(urlString: String): String {
    val url = URL(urlString)
    val c = url.openConnection() as HttpURLConnection
    c.setRequestProperty("User-Agent", "GeoWeatherApp")
    c.connectTimeout = 12000
    c.readTimeout = 12000
    BufferedReader(InputStreamReader(c.inputStream, StandardCharsets.UTF_8)).use { reader ->
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) sb.append(line)
        return sb.toString()
    }
}

fun hasPrecipitationData(json: String): Boolean {
    return try {
        val obj = JSONObject(json)
        val hourly = obj.optJSONObject("hourly")
        val daily = obj.optJSONObject("daily")
        hourly?.has("precipitation") == true && daily?.has("precipitation_sum") == true
    } catch (_: Exception) { false }
}

fun getYesterdayDate(daysAgo: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
}

data class DailyForecast(val date: String, val tempMax: Double, val tempMin: Double, val weatherCode: Int, val precipitationMm: Double = 0.0, val precipitationChance: Int = 0)
data class HourlyForecast(val time: String, val temp: Double, val weatherCode: Int, val precipitationMm: Double = 0.0, val precipitationChance: Int = 0)

fun parseForecastData(json: String): List<DailyForecast> {
    val list = mutableListOf<DailyForecast>()
    try {
        val obj = JSONObject(json)
        val daily = obj.getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val tMax = daily.getJSONArray("temperature_2m_max")
        val tMin = daily.getJSONArray("temperature_2m_min")
        val codes = daily.optJSONArray("weather_code") ?: daily.optJSONArray("weathercode")
        val precipitation = if (daily.has("precipitation_sum")) daily.getJSONArray("precipitation_sum") else null
        val precipitationChance = if (daily.has("precipitation_probability_max")) daily.getJSONArray("precipitation_probability_max") else null
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())
        for (i in 0 until times.length()) {
            val date = df.parse(times.getString(i))
            list.add(DailyForecast(date = outF.format(date ?: Date()), tempMax = tMax.getDouble(i), tempMin = tMin.getDouble(i), weatherCode = codes?.getInt(i) ?: 0, precipitationMm = precipitation?.optDouble(i, 0.0) ?: 0.0, precipitationChance = precipitationChance?.optInt(i, 0) ?: 0))
        }
    } catch (_: Exception) {}
    return list
}

fun parseHourlyForecastData(json: String): List<HourlyForecast> {
    val list = mutableListOf<HourlyForecast>()
    try {
        val hourly = JSONObject(json).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.optJSONArray("weather_code") ?: hourly.getJSONArray("weathercode")
        val precipitation = if (hourly.has("precipitation")) hourly.getJSONArray("precipitation") else null
        val precipitationChance = if (hourly.has("precipitation_probability")) hourly.getJSONArray("precipitation_probability") else null
        val inF = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outF = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        for (i in 0 until times.length()) {
            val date = inF.parse(times.getString(i)) ?: continue
            if (date.after(now.time) && list.size < 24) {
                list.add(HourlyForecast(time = outF.format(date), temp = temps.getDouble(i), weatherCode = codes.getInt(i), precipitationMm = precipitation?.optDouble(i, 0.0) ?: 0.0, precipitationChance = precipitationChance?.optInt(i, 0) ?: 0))
            }
        }
    } catch (_: Exception) {}
    return list
}

fun parseWeatherApiData(json: String): Pair<List<DailyForecast>, List<HourlyForecast>> {
    val dailyList = mutableListOf<DailyForecast>()
    val hourlyList = mutableListOf<HourlyForecast>()
    try {
        val obj = JSONObject(json)
        val forecast = obj.getJSONObject("forecast").getJSONArray("forecastday")
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (i in 0 until forecast.length()) {
            val day = forecast.getJSONObject(i)
            val astro = day.getJSONObject("day")
            val date = df.parse(day.getString("date"))
            dailyList.add(DailyForecast(date = outF.format(date ?: Date()), tempMax = astro.getDouble("maxtemp_c"), tempMin = astro.getDouble("mintemp_c"), weatherCode = 0, precipitationMm = astro.optDouble("totalprecip_mm", 0.0), precipitationChance = astro.optInt("daily_chance_of_rain", 0)))
            if (i == 0) {
                val hours = day.getJSONArray("hour")
                val now = System.currentTimeMillis()
                for (j in 0 until hours.length()) {
                    val h = hours.getJSONObject(j)
                    if (h.getLong("time_epoch") * 1000 > now && hourlyList.size < 24) {
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(h.getLong("time_epoch") * 1000))
                        hourlyList.add(HourlyForecast(time = time, temp = h.getDouble("temp_c"), weatherCode = 0, precipitationMm = h.optDouble("precip_mm", 0.0), precipitationChance = h.optInt("chance_of_rain", 0)))
                    }
                }
            }
        }
    } catch (_: Exception) {}
    return dailyList to hourlyList
}

fun getCurrentHourIndex(timesArray: JSONArray): Int {
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    for (i in 0 until timesArray.length()) {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(timesArray.getString(i)) ?: continue
        val cal = Calendar.getInstance().apply { time = date }
        if (cal.get(Calendar.HOUR_OF_DAY) == currentHour && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) return i
    }
    return 0
}

@Composable
fun MoonPhaseIcon(phaseName: String) {
    Canvas(modifier = Modifier.size(40.dp)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        
        // Draw moon background (dark side)
        drawCircle(color = Color.DarkGray, radius = radius)
        
        // Simplified moon phase drawing logic
        val normalizedPhase = when (phaseName.lowercase()) {
            "new moon" -> 0.0
            "waxing crescent" -> 0.25
            "first quarter" -> 0.5
            "waxing gibbous" -> 0.75
            "full moon" -> 1.0
            "waning gibbous" -> 0.75
            "last quarter" -> 0.5
            "waning crescent" -> 0.25
            else -> 0.5
        }
        
        if (normalizedPhase > 0) {
            val path = Path()
            // This is a simplified artistic representation
            if (normalizedPhase >= 0.5) {
                drawCircle(color = Color(0xFFFFF176), radius = radius)
                if (normalizedPhase < 1.0) {
                    // Draw shadow for gibbous
                    // (Omitted for complexity, just drawing full/half for now)
                }
            } else {
                // Crescent
                // (Omitted for complexity)
            }
        }
    }
}

fun getMoonPhaseEmoji(phase: String): String {
    return when (phase.lowercase()) {
        "new moon" -> "🌑"
        "waxing crescent" -> "🌒"
        "first quarter" -> "🌓"
        "waxing gibbous" -> "🌔"
        "full moon" -> "🌕"
        "waning gibbous" -> "🌖"
        "last quarter" -> "🌗"
        "waning crescent" -> "🌘"
        else -> "🌙"
    }
}

@Composable
fun HealthDetailSection(healthData: HealthData) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = stringResource(R.string.pollutants_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    healthData.alnusPollen?.let { PollenItem(stringResource(R.string.pollen_alnus), it) }
                    healthData.betulaPollen?.let { PollenItem(stringResource(R.string.pollen_betula), it) }
                    healthData.grassPollen?.let { PollenItem(stringResource(R.string.pollen_grass), it) }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    healthData.pm2_5?.let { PollenItem(stringResource(R.string.pm2_5_label), it) }
                    healthData.pm10?.let { PollenItem(stringResource(R.string.pm10_label), it) }
                    healthData.no2?.let { PollenItem(stringResource(R.string.no2_label), it) }
                    healthData.o3?.let { PollenItem(stringResource(R.string.o3_label), it) }
                }
            }
        }
    }
}

@Composable
fun PollenItem(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value.toInt().toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

data class HealthData(
    val europeanAqi: Double? = null,
    val uvIndex: Double? = null,
    val alnusPollen: Double? = null,
    val betulaPollen: Double? = null,
    val grassPollen: Double? = null,
    val pm2_5: Double? = null,
    val pm10: Double? = null,
    val no2: Double? = null,
    val o3: Double? = null
)

fun parseHealthData(json: String?): HealthData? {
    if (json == null) return null
    return try {
        val hourly = JSONObject(json).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val currentIndex = getCurrentHourIndex(times)

        HealthData(
            europeanAqi = hourly.optJSONArray("european_aqi")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() },
            uvIndex = hourly.optJSONArray("uv_index")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() },
            alnusPollen = hourly.optJSONArray("alnus_pollen")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() },
            betulaPollen = hourly.optJSONArray("betula_pollen")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() },
            grassPollen = hourly.optJSONArray("grass_pollen")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() },
            pm2_5 = hourly.optJSONArray("pm2_5")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() },
            pm10 = hourly.optJSONArray("pm10")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() },
            no2 = hourly.optJSONArray("nitrogen_dioxide")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() },
            o3 = hourly.optJSONArray("ozone")?.optDouble(currentIndex).takeIf { it != null && !it.isNaN() }
        )
    } catch (_: Exception) {
        null
    }
}

fun getUvIndexColor(uv: Double): Color {
    return when {
        uv < 3 -> Color(0xFF2E7D32)
        uv < 6 -> Color(0xFFF9A825)
        uv < 8 -> Color(0xFFEF6C00)
        uv < 11 -> Color(0xFFC62828)
        else -> Color(0xFF6A1B9A)
    }
}

fun calculateGoldenHour(sunrise: String, sunset: String): String {
    return try {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outF = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sunDate = df.parse(sunrise)
        val setDate = df.parse(sunset)
        val cal = Calendar.getInstance()
        cal.time = sunDate ?: Date()
        cal.add(Calendar.MINUTE, 30)
        val start = outF.format(cal.time)
        cal.time = setDate ?: Date()
        cal.add(Calendar.MINUTE, -60)
        val end = outF.format(cal.time)
        "$start - $end"
    } catch (_: Exception) { "--:--" }
}

fun calculateBlueHour(sunrise: String, sunset: String): String {
    return try {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outF = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sunDate = df.parse(sunrise)
        val setDate = df.parse(sunset)
        val cal = Calendar.getInstance()
        cal.time = sunDate ?: Date()
        cal.add(Calendar.MINUTE, -30)
        val morning = outF.format(cal.time)
        cal.time = setDate ?: Date()
        cal.add(Calendar.MINUTE, 30)
        val evening = outF.format(cal.time)
        "$morning / $evening"
    } catch (_: Exception) { "--:--" }
}

fun parseCurrentEuropeanAqi(aqiJson: String?): Double? {
    if (aqiJson == null) return null
    return try {
        val hourly = JSONObject(aqiJson).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val values = hourly.getJSONArray("european_aqi")
        val currentIndex = getCurrentHourIndex(times)
        values.optDouble(currentIndex).takeIf { !it.isNaN() }
    } catch (_: Exception) { null }
}

fun getEuropeanAqiColor(aqi: Double): Color {
    if (aqi <= 20.0) return Color(0xFF2E7D32)
    if (aqi <= 40.0) return Color(0xFF7CB342)
    if (aqi <= 60.0) return Color(0xFFF9A825)
    if (aqi <= 80.0) return Color(0xFFEF6C00)
    if (aqi <= 100.0) return Color(0xFFC62828)
    return Color(0xFF6A1B9A)
}

fun formatTime(timeString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(timeString)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())
    } catch (_: Exception) { timeString.takeLast(5) }
}

fun formatRainAmount(amountMm: Double): String {
    return if (amountMm < 0.1) "0 mm" else String.format(Locale.getDefault(), "%.1f mm", amountMm)
}
