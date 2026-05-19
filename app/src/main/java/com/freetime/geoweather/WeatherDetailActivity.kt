package com.freetime.geoweather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.net.URLEncoder
import java.util.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc)) }
                },
                actions = {
                    IconButton(onClick = { /* optional: local action */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_nav_desc))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.coordinates_label, lat, lon), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = name, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = stringResource(R.string.no_weather_available), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// --- Inlined repositories (moved from WeatherRepository.kt and GeocodingRepository.kt) ---

class InlinedWeatherRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)

    suspend fun getWeatherData(lat: Double, lon: Double, days: Int = 7, context: Context? = null): WeatherDataResult {
        val provider = sharedPrefs.getString("weather_provider", "open_meteo") ?: "open_meteo"
        val apiKey = sharedPrefs.getString("weather_api_key", "") ?: ""
        val tomorrowKey = sharedPrefs.getString("tomorrow_io_key", "") ?: ""
        val visualCrossingKey = sharedPrefs.getString("visual_crossing_key", "") ?: ""

        return try {
            when (provider) {
                "weatherapi" -> if (apiKey.isNotEmpty()) fetchFromWeatherApi(lat, lon, apiKey, days) else fetchFromOpenMeteo(lat, lon, days)
                "tomorrow" -> if (tomorrowKey.isNotEmpty()) fetchFromTomorrowIo(lat, lon, tomorrowKey) else fetchFromOpenMeteo(lat, lon, days)
                "visualcrossing" -> if (visualCrossingKey.isNotEmpty()) fetchFromVisualCrossing(lat, lon, visualCrossingKey) else fetchFromOpenMeteo(lat, lon, days)
                "accuweather" -> fetchFromAccuWeather()
                else -> fetchFromOpenMeteo(lat, lon, days)
            }
        } catch (e: Exception) {
            if (provider != "open_meteo" && context != null) {
                try {
                    fetchFromOpenMeteo(lat, lon, days)
                } catch (inner: Exception) {
                    WeatherDataResult.Error(context.getString(R.string.fallback_failed_prefix) + (inner.message ?: ""))
                }
            } else {
                WeatherDataResult.Error(context?.getString(R.string.request_failed_prefix) ?: "Error: " + (e.message ?: ""))
            }
        }
    }

    private fun fetchFromOpenMeteo(lat: Double, lon: Double, days: Int): WeatherDataResult {
        val url = "${ApiConstants.OPEN_METEO_FORECAST}?latitude=$lat&longitude=$lon&current=temperature_2m,windspeed_10m,weathercode,is_day&current_weather=true&hourly=temperature_2m,weathercode,relativehumidity_2m,pressure_msl,apparent_temperature,precipitation_probability,windspeed_10m&daily=weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,windspeed_10m_max&forecast_days=$days&timezone=auto"
        val response = NetworkUtils.httpGet(url)
        return parseOpenMeteoData(JSONObject(response), response, lat, lon)
    }

    private fun fetchFromWeatherApi(lat: Double, lon: Double, apiKey: String, days: Int): WeatherDataResult {
        val url = "https://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$lat,$lon&days=$days&aqi=yes"
        val response = NetworkUtils.httpGet(url)
        return parseWeatherApiData(JSONObject(response), response)
    }

    private fun fetchFromTomorrowIo(lat: Double, lon: Double, apiKey: String): WeatherDataResult {
        val url = "https://api.tomorrow.io/v4/weather/forecast?location=$lat,$lon&apikey=$apiKey"
        val response = NetworkUtils.httpGet(url)
        return parseTomorrowIoData(JSONObject(response), response)
    }

    private fun fetchFromVisualCrossing(lat: Double, lon: Double, apiKey: String): WeatherDataResult {
        val url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/$lat,$lon?unitGroup=metric&key=$apiKey&contentType=json"
        val response = NetworkUtils.httpGet(url)
        return parseVisualCrossingData(JSONObject(response), response)
    }

    private fun fetchFromAccuWeather(): WeatherDataResult {
        return WeatherDataResult.Error(sharedPrefs.getString("accuweather_not_supported", "AccuWeather not supported") ?: "AccuWeather not supported")
    }

    // --- Parser Functions ---

    private fun parseOpenMeteoData(json: JSONObject, rawResponse: String, lat: Double, lon: Double): WeatherDataResult {
        val current = if (json.has("current")) json.getJSONObject("current") else json.getJSONObject("current_weather")
        val daily = json.getJSONObject("daily")
        val hourly = json.getJSONObject("hourly")

        return WeatherDataResult.Success(
            provider = "open_meteo",
            temp = current.getDouble(if (current.has("temperature_2m")) "temperature_2m" else "temperature"),
            windSpeed = current.optDouble(if (current.has("windspeed_10m")) "windspeed_10m" else "windspeed", 0.0),
            precipitation = 0.0,
            weatherCode = current.getInt("weathercode"),
            isDay = current.optInt("is_day", 1) == 1,
            dailyForecast = parseOpenMeteoDaily(daily),
            hourlyForecast = parseOpenMeteoHourly(hourly),
            rawJson = rawResponse
        )
    }

    private fun parseWeatherApiData(json: JSONObject, rawResponse: String): WeatherDataResult {
        val current = json.getJSONObject("current")
        val forecast = json.getJSONObject("forecast").getJSONArray("forecastday")
        val dailyList = mutableListOf<DailyForecast>()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())

        for (i in 0 until forecast.length()) {
            val day = forecast.getJSONObject(i)
            val info = day.getJSONObject("day")
            val date = df.parse(day.getString("date")) ?: Date()
            dailyList.add(DailyForecast(outF.format(date), info.getDouble("maxtemp_c"), info.getDouble("mintemp_c"), info.getJSONObject("condition").getInt("code")))
        }

        return WeatherDataResult.Success(
            provider = "weatherapi",
            temp = current.getDouble("temp_c"),
            windSpeed = current.getDouble("wind_kph"),
            precipitation = current.getDouble("precip_mm"),
            weatherCode = current.getJSONObject("condition").getInt("code"),
            isDay = current.getInt("is_day") == 1,
            dailyForecast = dailyList,
            hourlyForecast = emptyList(),
            rawJson = rawResponse
        )
    }

    private fun parseTomorrowIoData(json: JSONObject, rawResponse: String): WeatherDataResult {
        val values = json.getJSONObject("timelines").getJSONArray("daily").getJSONObject(0).getJSONObject("values")
        return WeatherDataResult.Success(
            provider = "tomorrow.io",
            temp = values.getDouble("temperatureAvg"),
            weatherCode = values.getInt("weatherCodeMax"),
            isDay = true,
            dailyForecast = emptyList(),
            hourlyForecast = emptyList(),
            rawJson = rawResponse
        )
    }

    private fun parseVisualCrossingData(json: JSONObject, rawResponse: String): WeatherDataResult {
        val current = json.getJSONObject("currentConditions")
        return WeatherDataResult.Success(
            provider = "visualcrossing",
            temp = current.getDouble("temp"),
            weatherCode = 0,
            isDay = true,
            dailyForecast = emptyList(),
            hourlyForecast = emptyList(),
            rawJson = rawResponse
        )
    }

    fun parseOpenMeteoDaily(daily: JSONObject): List<DailyForecast> {
        val list = mutableListOf<DailyForecast>()
        val times = daily.getJSONArray("time")
        val tMax = daily.getJSONArray("temperature_2m_max")
        val tMin = daily.getJSONArray("temperature_2m_min")
        val codes = daily.optJSONArray("weathercode")
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outF = SimpleDateFormat("EEE, dd. MMM", Locale.getDefault())

        for (i in 0 until times.length()) {
            val date = df.parse(times.getString(i)) ?: Date()
            list.add(DailyForecast(outF.format(date), tMax.getDouble(i), tMin.getDouble(i), codes?.optInt(i) ?: 0))
        }
        return list
    }

    fun parseOpenMeteoHourly(hourly: JSONObject): List<HourlyForecast> {
        val list = mutableListOf<HourlyForecast>()
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weathercode")
        val inF = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val now = Calendar.getInstance()

        for (i in 0 until times.length()) {
            val date = inF.parse(times.getString(i)) ?: continue
            if (date.after(now.time) && list.size < 24) {
                list.add(HourlyForecast(SimpleDateFormat("HH:mm", Locale.getDefault()).format(date), temps.getDouble(i), codes.getInt(i)))
            }
        }
        return list
    }

    suspend fun getHistoricalData(lat: Double, lon: Double, daysAgo: Int = 7): List<DailyForecast> {
        val cal = Calendar.getInstance()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val endDate = df.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -(daysAgo - 1))
        val startDate = df.format(cal.time)

        val url = "${ApiConstants.OPEN_METEO_ARCHIVE}?latitude=$lat&longitude=$lon&start_date=$startDate&end_date=$endDate&daily=temperature_2m_max,temperature_2m_min,weathercode&timezone=auto"
        val response = NetworkUtils.httpGet(url)
        return parseOpenMeteoDaily(JSONObject(response).getJSONObject("daily"))
    }

    sealed class WeatherDataResult {
        data class Success(
            val provider: String,
            val temp: Double,
            val windSpeed: Double = 0.0,
            val precipitation: Double = 0.0,
            val weatherCode: Int,
            val isDay: Boolean,
            val dailyForecast: List<DailyForecast>,
            val hourlyForecast: List<HourlyForecast>,
            val rawJson: String,
            val aqiJson: String? = null,
            val moonPhase: String? = null
        ) : WeatherDataResult()
        data class Error(val message: String) : WeatherDataResult()
    }
}

data class DailyForecast(val date: String, val tempMax: Double, val tempMin: Double, val weatherCode: Int)
data class HourlyForecast(val time: String, val temp: Double, val weatherCode: Int)

class InlinedGeocodingRepository {

    suspend fun searchLocations(context: Context, query: String): List<Triple<String, Double, Double>> {
        // Direct local call to Open-Meteo Geocoding
        return fetchFromOpenMeteo(query)
    }

    private fun fetchFromOpenMeteo(query: String): List<Triple<String, Double, Double>> {
        val language = Locale.getDefault().language
        val coordinatePattern = Regex("""^(-?\\d+\\.?\\d*)\\s*,\\s*(-?\\d+\\.?\\d*)$""")
        val matchResult = coordinatePattern.matchEntire(query.trim())

        val url = if (matchResult != null) {
            val latitude = matchResult.groupValues[1].toDouble()
            val longitude = matchResult.groupValues[2].toDouble()
            "${ApiConstants.OPEN_METEO_REVERSE_GEOCODING}?latitude=$latitude&longitude=$longitude&language=$language&format=json"
        } else {
            "${ApiConstants.OPEN_METEO_GEOCODING}?name=" + URLEncoder.encode(query, "UTF-8") + "&count=20&language=$language&format=json"
        }

        return try {
            val json = NetworkUtils.httpGet(url) ?: return emptyList()
            parseResults(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseResults(json: String): List<Triple<String, Double, Double>> {
        return try {
            val obj = JSONObject(json)
            val list = mutableListOf<Triple<String, Double, Double>>()

            val arr = if (obj.has("results")) {
                obj.optJSONArray("results") ?: JSONArray()
            } else if (obj.has("name")) {
                JSONArray().apply { put(obj) }
            } else {
                JSONArray()
            }

            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val name = item.optString("name", "Unknown Location")
                val lat = item.optDouble("latitude", item.optDouble("lat", 0.0))
                val lon = item.optDouble("longitude", item.optDouble("lon", 0.0))

                var displayName = name
                val admin = item.optString("admin1", "")
                val country = item.optString("country", "")
                if (admin.isNotEmpty()) displayName += ", $admin"
                if (country.isNotEmpty()) displayName += ", $country"

                list.add(Triple(displayName, lat, lon))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}

