package com.freetime.geoweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.work.*
import com.freetime.geoweather.R
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class WeatherDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
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
    var aqiJson by remember { mutableStateOf<String?>(null) }
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
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&hourly=temperature_2m,weathercode,relativehumidity_2m,pressure_msl,apparent_temperature&daily=weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max,windspeed_10m_max&timezone=auto"
                val aqiUrl = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&hourly=pm10,pm2_5&timezone=auto"
                try {
                    val json = httpGet(url)
                    val aqiJsonResponse = try { httpGet(aqiUrl) } catch (e: Exception) { null }
                    val updatedEntity = entity?.copy(weatherData = json)
                    if (updatedEntity != null) {
                        db.locationDao().updateLocation(updatedEntity)
                    }
                    withContext(Dispatchers.Main) {
                        weatherJson = json
                        aqiJson = aqiJsonResponse
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Blue,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.BackBTNTXT))
                }
                Text(
                    text = name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(80.dp)) // Balance the back button
            }
            Spacer(Modifier.height(16.dp))
            if (weatherJson != null) {
                val obj = JSONObject(weatherJson!!)
                val current = obj.getJSONObject("current_weather")
                val temp = current.getDouble("temperature")
                val wind = current.getDouble("windspeed")
                val weatherCode = current.getInt("weathercode")
                
                // Get hourly data for current conditions with error handling
                var humidity: Double
                var pressure: Double
                var feelsLike: Double
                var precipitation: Double
                
                try {
                    val hourly = obj.getJSONObject("hourly")
                    val currentHourIndex = getCurrentHourIndex(hourly.getJSONArray("time"))
                    humidity = if (currentHourIndex >= 0) hourly.getJSONArray("relativehumidity_2m").getDouble(currentHourIndex) else 0.0
                    pressure = if (currentHourIndex >= 0) hourly.getJSONArray("pressure_msl").getDouble(currentHourIndex) else 0.0
                    feelsLike = if (currentHourIndex >= 0) hourly.getJSONArray("apparent_temperature").getDouble(currentHourIndex) else temp
                    precipitation = if (currentHourIndex >= 0) hourly.getJSONArray("precipitation_probability_max").getDouble(currentHourIndex) else 0.0
                } catch (e: Exception) {
                    // Fallback values if hourly data is missing
                    humidity = 0.0
                    pressure = 0.0
                    feelsLike = temp
                    precipitation = 0.0
                }
                
                // Get daily data with error handling
                var sunrise: String
                var sunset: String
                var maxWind: Double
                
                try {
                    val daily = obj.getJSONObject("daily")
                    sunrise = daily.getJSONArray("sunrise").getString(0)
                    sunset = daily.getJSONArray("sunset").getString(0)
                    maxWind = daily.getJSONArray("windspeed_10m_max").getDouble(0)
                } catch (e: Exception) {
                    // Fallback values if daily data is missing
                    sunrise = "N/A"
                    sunset = "N/A"
                    maxWind = 0.0
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = WeatherIconMapper.getWeatherIcon(weatherCode)),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(String.format(Locale.getDefault(), stringResource(R.string.TempTXT), temp.toString()), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(String.format(Locale.getDefault(), stringResource(R.string.WindSpeedTXT), wind.toString()), fontSize = 16.sp)
                Text(String.format(Locale.getDefault(), stringResource(R.string.FeelsLikeTXT), feelsLike.toString()), fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                
                // Weather Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.WeatherDetailsTXT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(String.format(Locale.getDefault(), stringResource(R.string.HumidityTXT), humidity.toInt()), fontSize = 14.sp)
                                Text(String.format(Locale.getDefault(), stringResource(R.string.PressureTXT), pressure.toInt()), fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(String.format(Locale.getDefault(), stringResource(R.string.PrecipitationTXT), precipitation.toInt()), fontSize = 14.sp)
                                Text(String.format(Locale.getDefault(), stringResource(R.string.MaxWindTXT), maxWind.toInt()), fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(sunrise), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.SunriseTXT), fontSize = 14.sp, color = Color(0xFFFFA500))
                            Text(stringResource(R.string.SunsetText), fontSize = 14.sp, color = Color(0xFFFF6B35))
                            Text(formatTime(sunset), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Air Quality Index Card
                aqiJson?.let { aqiData ->
                    val aqiInfo = calculateAQI(aqiData, LocalContext.current)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.AirQualityTXT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AQI: ${aqiInfo.value}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = aqiInfo.color
                                )
                                Text(
                                    text = aqiInfo.description,
                                    fontSize = 16.sp,
                                    color = aqiInfo.color
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Weather Alerts Card
                val weatherAlerts = calculateWeatherAlerts(temp, wind, precipitation, weatherCode, feelsLike, context = LocalContext.current)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.WeatherAlertsTXT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        if (weatherAlerts.isEmpty()) {
                            Text(
                                text = stringResource(R.string.NoAlertsTXT),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        } else {
                            weatherAlerts.forEach { alert ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(alert.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = alert.message,
                                        fontSize = 14.sp,
                                        color = alert.color,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Get context for notifications
                val context = LocalContext.current

                // Weather Map Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.WeatherMapTXT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.windy.com/?${lat},${lon},5")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.OpenMapTXT))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Weather Notifications Card
                val sharedPreferences = remember { context.getSharedPreferences("weather_notifications", Context.MODE_PRIVATE) }
                var notificationsEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("notifications_enabled", false)) }
                var notificationTime by remember { mutableStateOf(sharedPreferences.getString("notification_time", "08:00") ?: "08:00") }
                var showTimePicker by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                
                // Weather Change Alerts
                var changeAlertsEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("change_alerts_enabled", false)) }
                var selectedInterval by remember { mutableStateOf(sharedPreferences.getString("change_interval", "3") ?: "3") }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.NotificationsTXT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        
                        // Daily Weather Updates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.EnableNotificationsTXT), fontSize = 14.sp)
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { 
                                    notificationsEnabled = it
                                    sharedPreferences.edit()
                                        .putBoolean("notifications_enabled", it)
                                        .apply()
                                    
                                    if (it) {
                                        scheduleDailyWeatherNotification(context, name, lat, lon, notificationTime)
                                    } else {
                                        cancelWeatherNotifications(context)
                                    }
                                }
                            )
                        }
                        
                        if (notificationsEnabled) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.NotificationTimeTXT), fontSize = 14.sp)
                                Button(
                                    onClick = { showTimePicker = true },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(notificationTime, fontSize = 14.sp)
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Weather Change Alerts
                        Text(stringResource(R.string.WeatherChangeAlertsTXT), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.EnableChangeAlertsTXT), fontSize = 14.sp)
                            Switch(
                                checked = changeAlertsEnabled,
                                onCheckedChange = { 
                                    changeAlertsEnabled = it
                                    sharedPreferences.edit()
                                        .putBoolean("change_alerts_enabled", it)
                                        .apply()
                                    
                                    if (it) {
                                        scheduleWeatherChangeAlerts(context, selectedInterval)
                                    } else {
                                        cancelWeatherChangeAlerts(context)
                                    }
                                }
                            )
                        }
                        
                        if (changeAlertsEnabled) {
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.ChangeAlertIntervalTXT), fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("2", "3", "6").forEach { hours ->
                                    Button(
                                        onClick = {
                                            selectedInterval = hours
                                            sharedPreferences.edit()
                                                .putString("change_interval", hours)
                                                .apply()
                                            scheduleWeatherChangeAlerts(context, hours)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedInterval == hours) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Text(
                                            text = when (hours) {
                                                "2" -> stringResource(R.string.Interval2HoursTXT)
                                                "3" -> stringResource(R.string.Interval3HoursTXT)
                                                "6" -> stringResource(R.string.Interval6HoursTXT)
                                                else -> "$hours hours"
                                            },
                                            fontSize = 12.sp,
                                            color = if (selectedInterval == hours) 
                                                MaterialTheme.colorScheme.onPrimary 
                                            else 
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (notificationsEnabled || changeAlertsEnabled) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    // Save all notification settings
                                    if (notificationsEnabled) {
                                        scheduleDailyWeatherNotification(context, name, lat, lon, notificationTime)
                                    }
                                    if (changeAlertsEnabled) {
                                        scheduleWeatherChangeAlerts(context, selectedInterval)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.SaveSettingsTXT))
                            }
                        }
                    }
                }
                
                if (showTimePicker) {
                    TimePickerDialog(
                        onTimeSelected = { time ->
                            notificationTime = time
                            sharedPreferences.edit()
                                .putString("notification_time", time)
                                .apply()
                            showTimePicker = false
                        },
                        onDismiss = { showTimePicker = false },
                        initialTime = notificationTime
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(stringResource(R.string.HourlyForecastTXT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hourlyForecastList) { forecast ->
                        HourlyForecastItem(forecast = forecast)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(stringResource(R.string.SevenDayForecastTXT), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(forecastList) { forecast ->
                        ForecastItem(forecast = forecast)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(100.dp))
                }
            }
        }
    }
}

@Composable
fun CircularWavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    waveAmplitude: Dp = 4.dp,
    waveFrequency: Float = 2f
) {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2 - strokeWidth.toPx() / 2
        val path = Path()
        for (i in 0..360) {
            val angleRad = Math.toRadians(i.toDouble() + angle).toFloat()
            val currentRadius = radius + sin(Math.toRadians(i.toDouble() * waveFrequency).toFloat() * 10) * waveAmplitude.toPx()
            val x = center.x + currentRadius * cos(angleRad)
            val y = center.y + currentRadius * sin(angleRad)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth.toPx())
        )
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

data class AQIInfo(
    val value: Int,
    val description: String,
    val color: Color
)

data class WeatherAlert(
    val type: String,
    val message: String,
    val severity: String,
    val color: Color
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

            val calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone))
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

fun getCurrentHourIndex(timesArray: JSONArray): Int {
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)
    
    for (i in 0 until timesArray.length()) {
        val timeStr = timesArray.getString(i)
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val date = inputFormat.parse(timeStr)
        
        if (date != null) {
            val calendar = Calendar.getInstance()
            calendar.time = date
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            
            if (hour == currentHour && currentMinute < 30) return i
            if (hour == currentHour - 1 && currentMinute >= 30) return i
        }
    }
    return 0
}

fun formatTime(timeString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = inputFormat.parse(timeString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        // Safe fallback with bounds checking
        try {
            if (timeString.length >= 16) {
                timeString.substring(11, 16) // Extract HH:mm from format like "2024-01-01T12:30"
            } else if (timeString.length >= 5) {
                timeString.takeLast(5) // Take last 5 characters
            } else {
                timeString // Return as-is if very short
            }
        } catch (e: Exception) {
            "N/A" // Ultimate fallback
        }
    }
}

fun calculateAQI(aqiJson: String, context: android.content.Context): AQIInfo {
    return try {
        val obj = JSONObject(aqiJson)
        val hourly = obj.getJSONObject("hourly")
        val pm25Array = hourly.getJSONArray("pm2_5")
        val pm10Array = hourly.getJSONArray("pm10")
        
        // Get current hour index
        val currentIndex = getCurrentHourIndex(hourly.getJSONArray("time"))
        val pm25 = if (currentIndex >= 0 && currentIndex < pm25Array.length()) pm25Array.getDouble(currentIndex) else 0.0
        val pm10 = if (currentIndex >= 0 && currentIndex < pm10Array.length()) pm10Array.getDouble(currentIndex) else 0.0
        
        // Calculate AQI based on PM2.5 (primary pollutant)
        val aqiValue = calculateUSAQI(pm25)
        
        val resources = context.resources
        when {
            aqiValue <= 50 -> AQIInfo(aqiValue, resources.getString(R.string.AQIGoodTXT), Color(0xFF00E400))
            aqiValue <= 100 -> AQIInfo(aqiValue, resources.getString(R.string.AQIModerateTXT), Color(0xFFFFD700))
            aqiValue <= 150 -> AQIInfo(aqiValue, resources.getString(R.string.AQIUnhealthySensitiveTXT), Color(0xFFFF7E00))
            aqiValue <= 200 -> AQIInfo(aqiValue, resources.getString(R.string.AQIUnhealthyTXT), Color(0xFFFF0000))
            aqiValue <= 300 -> AQIInfo(aqiValue, resources.getString(R.string.AQIVeryUnhealthyTXT), Color(0xFF8F3F97))
            else -> AQIInfo(aqiValue, resources.getString(R.string.AQIHazardousTXT), Color(0xFF7E0023))
        }
    } catch (e: Exception) {
        AQIInfo(0, "Unknown", Color.Gray)
    }
}

fun calculateUSAQI(pm25: Double): Int {
    return when {
        pm25 <= 12.0 -> (pm25 / 12.0 * 50).toInt()
        pm25 <= 35.4 -> ((pm25 - 12.0) / (35.4 - 12.0) * 50 + 50).toInt()
        pm25 <= 55.4 -> ((pm25 - 35.4) / (55.4 - 35.4) * 50 + 100).toInt()
        pm25 <= 150.4 -> ((pm25 - 55.4) / (150.4 - 55.4) * 50 + 150).toInt()
        pm25 <= 250.4 -> ((pm25 - 150.4) / (250.4 - 150.4) * 100 + 200).toInt()
        else -> ((pm25 - 250.4) / (500.4 - 250.4) * 100 + 300).toInt()
    }
}

fun calculateWeatherAlerts(
    temp: Double,
    wind: Double,
    precipitation: Double,
    weatherCode: Int,
    feelsLike: Double,
    context: android.content.Context
): List<WeatherAlert> {
    val alerts = mutableListOf<WeatherAlert>()
    val resources = context.resources
    
    // Wind alerts
    if (wind > 50) {
        alerts.add(
            WeatherAlert(
                type = "wind",
                message = String.format(resources.getString(R.string.WindAlertTXT), wind.toInt()),
                severity = "high",
                color = Color(0xFFFF0000)
            )
        )
    } else if (wind > 35) {
        alerts.add(
            WeatherAlert(
                type = "wind",
                message = String.format(resources.getString(R.string.WindAlertTXT), wind.toInt()),
                severity = "medium",
                color = Color(0xFFFFA500)
            )
        )
    }
    
    // Precipitation alerts
    if (precipitation > 70) {
        alerts.add(
            WeatherAlert(
                type = "rain",
                message = String.format(resources.getString(R.string.RainAlertTXT), precipitation.toInt()),
                severity = "high",
                color = Color(0xFF0000FF)
            )
        )
    } else if (precipitation > 50) {
        alerts.add(
            WeatherAlert(
                type = "rain",
                message = String.format(resources.getString(R.string.RainAlertTXT), precipitation.toInt()),
                severity = "medium",
                color = Color(0xFF0080FF)
            )
        )
    }
    
    // Temperature alerts
    if (temp > 35 || temp < -10) {
        alerts.add(
            WeatherAlert(
                type = "temperature",
                message = String.format(resources.getString(R.string.TemperatureAlertTXT), temp.toInt(), feelsLike.toInt()),
                severity = "high",
                color = Color(0xFFFF00FF)
            )
        )
    } else if (temp > 30 || temp < -5) {
        alerts.add(
            WeatherAlert(
                type = "temperature",
                message = String.format(resources.getString(R.string.TemperatureAlertTXT), temp.toInt(), feelsLike.toInt()),
                severity = "medium",
                color = Color(0xFF800080)
            )
        )
    }
    
    // Storm alerts (based on weather codes)
    if (weatherCode in 95..99 || weatherCode in 45..48) {
        alerts.add(
            WeatherAlert(
                type = "storm",
                message = resources.getString(R.string.StormAlertTXT),
                severity = "high",
                color = Color(0xFF8B0000)
            )
        )
    }
    
    // Snow alerts
    if (weatherCode in 71..79) {
        alerts.add(
            WeatherAlert(
                type = "snow",
                message = resources.getString(R.string.SnowAlertTXT),
                severity = "medium",
                color = Color(0xFF87CEEB)
            )
        )
    }
    
    return alerts
}

@Composable
fun TimePickerDialog(
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialTime: String
) {
    val (hour, minute) = initialTime.split(":").map { it.toInt() }
    var selectedHour by remember { mutableStateOf(hour) }
    var selectedMinute by remember { mutableStateOf(minute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Column {
                Text("Hour: $selectedHour")
                Slider(
                    value = selectedHour.toFloat(),
                    onValueChange = { selectedHour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Minute: $selectedMinute")
                Slider(
                    value = selectedMinute.toFloat(),
                    onValueChange = { selectedMinute = it.toInt() },
                    valueRange = 0f..59f,
                    steps = 58
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                    onTimeSelected(time)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun scheduleDailyWeatherNotification(
    context: Context,
    locationName: String,
    lat: Double,
    lon: Double,
    time: String
) {
    try {
        val workManager = WorkManager.getInstance(context)
        
        // Save location data for worker
        val sharedPreferences = context.getSharedPreferences("weather_notifications", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("location_name", locationName)
            .putFloat("location_lat", lat.toFloat())
            .putFloat("location_lon", lon.toFloat())
            .apply()
        
        // Parse time
        val (hour, minute) = time.split(":").map { it.toInt() }
        
        // Calculate delay until next scheduled time
        val now = Calendar.getInstance()
        val scheduled = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        
        if (scheduled.before(now)) {
            scheduled.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val delay = scheduled.timeInMillis - now.timeInMillis
        
        // Create work request
        val workRequest = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("weather_notification")
            .build()
        
        // Schedule the work
        workManager.enqueueUniquePeriodicWork(
            "weather_notification",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun cancelWeatherNotifications(context: Context) {
    try {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("weather_notification")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun scheduleWeatherChangeAlerts(
    context: Context,
    intervalHours: String
) {
    try {
        val workManager = WorkManager.getInstance(context)
        
        // Parse interval
        val interval = intervalHours.toLong()
        
        // Create periodic work request
        val workRequest = PeriodicWorkRequestBuilder<WeatherChangeWorker>(interval, TimeUnit.HOURS)
            .addTag("weather_change_alert")
            .build()
        
        // Schedule the work
        workManager.enqueueUniquePeriodicWork(
            "weather_change_alert",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun cancelWeatherChangeAlerts(context: Context) {
    try {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("weather_change_alert")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
