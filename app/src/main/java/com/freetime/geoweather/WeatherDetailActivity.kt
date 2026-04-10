package com.freetime.geoweather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkNotificationPermission()
        
        val name = intent.getStringExtra("name") ?: "Unknown"
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
    
    val tempUnit by sharedPreferences.collectStringAsState("temp_unit", "celsius")
    val windUnit by sharedPreferences.collectStringAsState("wind_unit", "kmh")

    var weatherJson by remember { mutableStateOf<String?>(null) }
    var aqiJson by remember { mutableStateOf<String?>(null) }
    var moonPhaseName by remember { mutableStateOf<String?>(null) }
    var moonIconCode by remember { mutableStateOf<String?>(null) }
    var forecastList by remember { mutableStateOf<List<DailyForecast>>(emptyList()) }
    var hourlyForecastList by remember { mutableStateOf<List<HourlyForecast>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun refreshWeatherData(forceRefresh: Boolean = false) {
        try {
            isRefreshing = true
            val entity = withContext(Dispatchers.IO) { db.locationDao().findByCoordinates(lat, lon) }
            val currentTime = System.currentTimeMillis()
            
            val lastUpdated = entity?.lastUpdated
            val dataAgeMinutes = if (lastUpdated != null) (currentTime - lastUpdated) / (1000 * 60) else Long.MAX_VALUE

            var json: String? = null
            var aqiJsonResponse: String? = null

            if (!forceRefresh && entity?.weatherData != null && dataAgeMinutes < 30) {
                json = entity.weatherData
                weatherJson = json
            } else {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=temperature_2m,weathercode,relativehumidity_2m,pressure_msl,apparent_temperature&daily=weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,windspeed_10m_max&timezone=auto"
                val aqiUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&hourly=pm10,pm2_5&timezone=auto"
                
                json = withContext(Dispatchers.IO) { httpGet(url) }
                aqiJsonResponse = withContext(Dispatchers.IO) { try { httpGet(aqiUrl) } catch (e: Exception) { null } }

                try {
                    val moonUrl = "https://devapi.qweather.com/v7/astronomy/moon?location=$lon,$lat&key=d5184299458c441b92ab98075c4a7928"
                    val mq = withContext(Dispatchers.IO) { httpGet(moonUrl) }
                    val obj = JSONObject(mq).getJSONArray("moonPhase").getJSONObject(0)
                    moonPhaseName = obj.optString("name", null)
                    moonIconCode = obj.optString("icon", null)
                } catch (_: Exception) {}
            }

            entity?.copy(weatherData = json, lastUpdated = currentTime)?.let {
                withContext(Dispatchers.IO) { db.locationDao().updateLocation(it) }
            }

            weatherJson = json
            aqiJson = aqiJsonResponse
            if (json != null) {
                forecastList = parseForecastData(json)
                hourlyForecastList = parseHourlyForecastData(json)
            }
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error loading weather"
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
                val obj = JSONObject(json)
                val current = obj.getJSONObject("current_weather")
                val temp = current.getDouble("temperature")
                val weatherCode = current.getInt("weathercode")
                
                val displayTemp = if (tempUnit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()
                val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = WeatherIconMapper.getWeatherIcon(weatherCode)),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = Color.Unspecified
                        )
                        Text("$displayTemp$tempSuffix", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                        Text(WeatherCodes.getDescription(weatherCode), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    WeatherDetailsGrid(obj, tempUnit, windUnit)
                }

                item {
                    HourlyForecastSection(hourlyForecastList, tempUnit)
                }

                item {
                    DailyForecastSection(forecastList, tempUnit)
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
fun WeatherDetailsGrid(weatherObj: JSONObject, tempUnit: String, windUnit: String) {
    val current = weatherObj.getJSONObject("current_weather")
    val hourly = weatherObj.optJSONObject("hourly")
    val daily = weatherObj.optJSONObject("daily")
    val currentIndex = if (hourly != null) getCurrentHourIndex(hourly.getJSONArray("time")) else -1

    val wind = current.getDouble("windspeed")
    val displayWind = if (windUnit == "mph") (wind * 0.621371).toInt() else wind.toInt()
    val windSuffix = if (windUnit == "mph") " mph" else " km/h"

    val feelsLike = if (currentIndex >= 0) hourly?.getJSONArray("apparent_temperature")?.getDouble(currentIndex) ?: current.getDouble("temperature") else current.getDouble("temperature")
    val displayFeelsLike = if (tempUnit == "fahrenheit") (feelsLike * 9/5 + 32).toInt() else feelsLike.toInt()
    val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DetailItem(label = stringResource(R.string.wind_label), value = "$displayWind$windSuffix")
                DetailItem(label = stringResource(R.string.feels_like_label), value = "$displayFeelsLike$tempSuffix")
                DetailItem(label = stringResource(R.string.humidity_label), value = "${if (currentIndex >= 0) hourly?.getJSONArray("relativehumidity_2m")?.getInt(currentIndex) else 0}%")
            }
            if (daily != null) {
                val sunrise = formatTime(daily.getJSONArray("sunrise").getString(0))
                val sunset = formatTime(daily.getJSONArray("sunset").getString(0))
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
fun HourlyForecastSection(list: List<HourlyForecast>, tempUnit: String) {
    val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.hourly_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { forecast ->
                val displayTemp = if (tempUnit == "fahrenheit") (forecast.temp * 9/5 + 32).toInt() else forecast.temp.toInt()
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(forecast.time, style = MaterialTheme.typography.labelMedium)
                        Icon(painter = painterResource(WeatherIconMapper.getWeatherIcon(forecast.weatherCode)), contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Unspecified)
                        Text("$displayTemp$tempSuffix", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyForecastSection(list: List<DailyForecast>, tempUnit: String) {
    val tempSuffix = if (tempUnit == "fahrenheit") "°" else "°" // Simplified for list
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(R.string.forecast_7day_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        list.forEach { forecast ->
            val displayMax = if (tempUnit == "fahrenheit") (forecast.tempMax * 9/5 + 32).toInt() else forecast.tempMax.toInt()
            val displayMin = if (tempUnit == "fahrenheit") (forecast.tempMin * 9/5 + 32).toInt() else forecast.tempMin.toInt()
            ListItem(
                headlineContent = { Text(forecast.date) },
                supportingContent = { Text(WeatherCodes.getDescription(forecast.weatherCode)) },
                trailingContent = { Text("$displayMax$tempSuffix / $displayMin$tempSuffix", fontWeight = FontWeight.Bold) },
                leadingContent = { Icon(painter = painterResource(WeatherIconMapper.getWeatherIcon(forecast.weatherCode)), contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Unspecified) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
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

data class DailyForecast(val date: String, val tempMax: Double, val tempMin: Double, val weatherCode: Int)
data class HourlyForecast(val time: String, val temp: Double, val weatherCode: Int)

fun parseForecastData(json: String): List<DailyForecast> {
    val list = mutableListOf<DailyForecast>()
    try {
        val daily = JSONObject(json).getJSONObject("daily")
        val times = daily.getJSONArray("time")
        val tMax = daily.getJSONArray("temperature_2m_max")
        val tMin = daily.getJSONArray("temperature_2m_min")
        val codes = daily.getJSONArray("weathercode")
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())
        for (i in 0 until times.length()) {
            val date = df.parse(times.getString(i))
            list.add(DailyForecast(outF.format(date ?: Date()), tMax.getDouble(i), tMin.getDouble(i), codes.getInt(i)))
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
        val codes = hourly.getJSONArray("weathercode")
        val inF = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outF = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()
        for (i in 0 until times.length()) {
            val date = inF.parse(times.getString(i)) ?: continue
            if (date.after(now.time) && list.size < 24) {
                list.add(HourlyForecast(outF.format(date), temps.getDouble(i), codes.getInt(i)))
            }
        }
    } catch (_: Exception) {}
    return list
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

fun formatTime(timeString: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(timeString)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())
    } catch (_: Exception) { timeString.takeLast(5) }
}
