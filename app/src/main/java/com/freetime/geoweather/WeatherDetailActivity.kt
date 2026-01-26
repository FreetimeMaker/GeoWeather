package com.freetime.geoweather

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class WeatherDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("name") ?: "Unknown"
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)

        setContent {
            GeoWeatherTheme {
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
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
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

@Composable
fun WeatherDetailScreen(
    name: String,
    lat: Double,
    lon: Double,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var weatherJson by remember { mutableStateOf<String?>(null) }
    var forecastList by remember { mutableStateOf<List<DailyForecast>>(emptyList()) }
    var hourlyForecastList by remember { mutableStateOf<List<HourlyForecast>>(emptyList()) }
    val db = LocationDatabase.getDatabase(LocalContext.current)

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val entity = db.locationDao().findByCoordinates(lat, lon)
            if (entity?.weatherData != null) {
                withContext(Dispatchers.Main) {
                    weatherJson = entity.weatherData
                    forecastList = parseForecastData(entity.weatherData)
                    hourlyForecastList = parseHourlyForecastData(entity.weatherData)
                }
            } else {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=temperature_2m,weathercode&daily=weathercode,temperature_2m_max,temperature_2m_min&timezone=auto"
                try {
                    val json = httpGet(url)
                    val updatedEntity = entity?.copy(weatherData = json)
                    if (updatedEntity != null) {
                        db.locationDao().updateLocation(updatedEntity)
                    }
                    withContext(Dispatchers.Main) {
                        weatherJson = json
                        forecastList = parseForecastData(json)
                        hourlyForecastList = parseHourlyForecastData(json)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Blue,
                        contentColor = Color.White
                    )
                ) {
                    Text("← Back")
                }
            }
            Spacer(Modifier.height(16.dp))
            if (weatherJson != null) {
                val obj = JSONObject(weatherJson!!)
                val current = obj.getJSONObject("current_weather")
                val temp = current.getDouble("temperature")
                val wind = current.getDouble("windspeed")
                val weatherCode = current.getInt("weathercode")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = WeatherIconMapper.getWeatherIcon(weatherCode)),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Temperature: $temp°C", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("Wind: $wind km/h", fontSize = 16.sp)
                Spacer(Modifier.height(24.dp))

                Text("Hourly Forecast", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hourlyForecastList) { forecast ->
                        HourlyForecastItem(forecast = forecast)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text("7-Day Forecast", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(forecastList) { forecast ->
                        ForecastItem(forecast = forecast)
                    }
                }
            }
        }
        if (weatherJson == null) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

data class DailyForecast(
    val date: String,
    val tempMax: Double,
    val tempMin: Double,
    val weatherCode: Int
)

data class HourlyForecast(
    val time: String,
    val temp: Double,
    val weatherCode: Int
)

@Composable
fun ForecastItem(forecast: DailyForecast) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weather Icon
            Icon(
                painter = painterResource(id = WeatherIconMapper.getWeatherIcon(forecast.weatherCode)),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Date and Description
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = forecast.date,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = WeatherCodes.getDescription(forecast.weatherCode),
                    fontSize = 14.sp
                )
            }

            // Temperature
            Text(
                text = String.format(Locale.getDefault(), "%.1f° / %.1f°", forecast.tempMin, forecast.tempMax),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HourlyForecastItem(forecast: HourlyForecast) {
    Card(
        modifier = Modifier.width(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = forecast.time,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                painter = painterResource(id = WeatherIconMapper.getWeatherIcon(forecast.weatherCode)),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${forecast.temp}°C",
                fontSize = 14.sp
            )
        }
    }
}

fun parseForecastData(weatherJson: String): List<DailyForecast> {
    val forecastList = mutableListOf<DailyForecast>()

    try {
        val obj = JSONObject(weatherJson)
        val daily = obj.getJSONObject("daily")

        val times = daily.getJSONArray("time")
        val tempMax = daily.getJSONArray("temperature_2m_max")
        val tempMin = daily.getJSONArray("temperature_2m_min")
        val weatherCodes = daily.getJSONArray("weathercode")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        for (i in 0 until times.length()) {
            val dateStr = times.getString(i)
            val date = dateFormat.parse(dateStr)
            val formattedDate = outputFormat.format(date ?: Date())

            forecastList.add(
                DailyForecast(
                    date = formattedDate,
                    tempMax = tempMax.getDouble(i),
                    tempMin = tempMin.getDouble(i),
                    weatherCode = weatherCodes.getInt(i)
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return forecastList
}

fun parseHourlyForecastData(weatherJson: String): List<HourlyForecast> {
    val hourlyForecastList = mutableListOf<HourlyForecast>()

    try {
        val obj = JSONObject(weatherJson)
        val hourly = obj.getJSONObject("hourly")
        val timezone = obj.getString("timezone")

        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val weatherCodes = hourly.getJSONArray("weathercode")

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getTimeZone(timezone)

        val now = Calendar.getInstance(TimeZone.getTimeZone(timezone))
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        for (i in 0 until times.length()) {
            val timeStr = times.getString(i)
            val date = inputFormat.parse(timeStr)

            val calendar = Calendar.getInstance()
            if (date != null) {
                calendar.time = date
            }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)

            if (hour >= currentHour && hourlyForecastList.size < 24) {
                val formattedTime = outputFormat.format(date ?: Date())
                hourlyForecastList.add(
                    HourlyForecast(
                        time = formattedTime,
                        temp = temps.getDouble(i),
                        weatherCode = weatherCodes.getInt(i)
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return hourlyForecastList
}
