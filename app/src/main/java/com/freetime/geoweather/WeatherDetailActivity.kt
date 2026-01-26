package com.freetime.geoweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.freetime.geoweather.ui.hideSystemUI
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WeatherDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI(window)
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
    val db = LocationDatabase.getDatabase(LocalContext.current)

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val entity = db.locationDao().findByCoordinates(lat, lon)
            if (entity?.weatherData != null) {
                withContext(Dispatchers.Main) {
                    weatherJson = entity.weatherData
                    forecastList = parseForecastData(entity.weatherData)
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

                Text("Temperature: $temp°C", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Wind: $wind km/h", fontSize = 16.sp)
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
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center),
                strokeWidth = 6.dp
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
            androidx.compose.material3.Icon(
                painter = painterResource(id = WeatherIconMapper.getWeatherIcon(forecast.weatherCode)),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified
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
