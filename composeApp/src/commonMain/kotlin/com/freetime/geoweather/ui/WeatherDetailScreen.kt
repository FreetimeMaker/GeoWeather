package com.freetime.geoweather.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freetime.geoweather.SettingsManager
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.WeatherIconMapper
import com.freetime.geoweather.currentInstant
import com.freetime.geoweather.network.models.WeatherResponse
import geoweather.composeapp.generated.resources.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    name: String,
    lat: Double,
    lon: Double,
    viewModel: WeatherViewModel,
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val uiState by viewModel.uiState.collectAsState()
    
    val tempUnit by settingsManager.tempUnit.collectAsState()
    val windUnit by settingsManager.windUnit.collectAsState()

    LaunchedEffect(lat, lon) {
        viewModel.loadWeather(lat, lon)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(name) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc)) 
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadWeather(lat, lon, forceRefresh = true) }, enabled = !uiState.isLoading) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh_nav_desc))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        uiState.error?.let { error ->
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        } ?: uiState.weather?.let { weather ->
            WeatherDetailContent(weather, uiState.historicalWeather, innerPadding, tempUnit, windUnit)
        } ?: run {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun WeatherDetailContent(
    weather: WeatherResponse,
    historicalWeather: com.freetime.geoweather.network.models.WeatherResponse?,
    innerPadding: PaddingValues,
    tempUnit: String,
    windUnit: String
) {
    val current = weather.currentWeather
    val daily = weather.daily
    val hourly = weather.hourly

    var selectedDayIndex by remember { mutableStateOf(-1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        current?.let {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(WeatherIconMapper.getWeatherIcon(it.weathercode)),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = Color.Unspecified
                    )
                    val displayTemp = formatTemp(it.temperature, tempUnit)
                    Text(displayTemp, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(WeatherCodes.getDescriptionResource(it.weathercode)),
                        style = MaterialTheme.typography.headlineSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            WeatherDetailsGrid(weather, tempUnit, windUnit)
        }

        hourly?.let {
            item {
                HourlyForecastSection(it, tempUnit)
            }
        }

        daily?.let {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(Res.string.forecast_7day_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    for (i in it.time.indices) {
                        ForecastItemRow(
                            date = it.time[i],
                            tempMax = it.temperature2mMax[i],
                            tempMin = it.temperature2mMin[i],
                            weatherCode = it.weathercode[i],
                            tempUnit = tempUnit,
                            isSelected = selectedDayIndex == i,
                            onClick = { selectedDayIndex = if (selectedDayIndex == i) -1 else i }
                        )
                    }
                }
            }
        }

        historicalWeather?.daily?.let { historicalDaily ->
            item {
                var showHistoricalList by remember { mutableStateOf(false) }
                var selectedHistoricalIndex by remember { mutableStateOf(-1) }
                
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(Res.string.last_7_days_trend), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showHistoricalList = !showHistoricalList }) {
                            Text(if (showHistoricalList) stringResource(Res.string.show_chart) else stringResource(Res.string.show_list))
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    if (showHistoricalList) {
                        for (i in historicalDaily.time.indices.reversed()) {
                            ForecastItemRow(
                                date = historicalDaily.time[i],
                                tempMax = historicalDaily.temperature2mMax[i],
                                tempMin = historicalDaily.temperature2mMin[i],
                                weatherCode = historicalDaily.weathercode.getOrNull(i) ?: 0,
                                tempUnit = tempUnit,
                                isSelected = selectedHistoricalIndex == i,
                                onClick = { selectedHistoricalIndex = if (selectedHistoricalIndex == i) -1 else i }
                            )
                        }
                    } else {
                        HistoricalTrendsChart(historicalDaily, tempUnit)
                    }
                }
            }
        }

        weather.daily?.let { dailyData ->
            item {
                MoonPhaseSection(dailyData.time.firstOrNull() ?: "")
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun WeatherDetailsGrid(weather: WeatherResponse, tempUnit: String, windUnit: String) {
    val current = weather.currentWeather
    val hourly = weather.hourly
    val daily = weather.daily

    val wind = current?.windspeed ?: 0.0
    val feelsLike = current?.temperature ?: 0.0 // Simplified for now
    val humidity = 0 // Need to extract from hourly

    val displayWind = formatWind(wind, windUnit)
    val displayFeelsLike = formatTemp(feelsLike, tempUnit)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DetailItem(label = stringResource(Res.string.wind_label), value = displayWind)
                DetailItem(label = stringResource(Res.string.feels_like_label), value = displayFeelsLike)
                DetailItem(label = stringResource(Res.string.humidity_label), value = "$humidity%")
            }
            daily?.let {
                val sunrise = it.sunrise.firstOrNull()?.let { s -> formatTime(s) } ?: "--:--"
                val sunset = it.sunset.firstOrNull()?.let { s -> formatTime(s) } ?: "--:--"
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DetailItem(label = stringResource(Res.string.sunrise_label), value = sunrise)
                    DetailItem(label = stringResource(Res.string.sunset_label), value = sunset)
                }
            }
        }
    }
}

@Composable
fun HourlyForecastSection(hourly: com.freetime.geoweather.network.models.HourlyData, tempUnit: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(Res.string.hourly_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Filter currentInstant
            val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val filteredIndices = hourly.time.indices.filter { i ->
                val time = LocalDateTime.parse(hourly.time[i])
                time >= now
            }.take(24)

            items(filteredIndices) { i ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatTime(hourly.time[i]), style = MaterialTheme.typography.labelMedium)
                        Icon(
                            painter = painterResource(WeatherIconMapper.getWeatherIcon(hourly.weathercode[i])),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified
                        )
                        Text(formatTemp(hourly.temperature2m[i], tempUnit), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastItemRow(
    date: String,
    tempMax: Double,
    tempMin: Double,
    weatherCode: Int,
    tempUnit: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
                headlineContent = { Text(formatDate(date)) },
                supportingContent = { Text(stringResource(WeatherCodes.getDescriptionResource(weatherCode))) },
                trailingContent = { 
                    Text("${formatTemp(tempMax, tempUnit)} / ${formatTemp(tempMin, tempUnit)}", fontWeight = FontWeight.Bold) 
                },
                leadingContent = { 
                    Icon(
                        painter = painterResource(WeatherIconMapper.getWeatherIcon(weatherCode)), 
                        contentDescription = null, 
                        modifier = Modifier.size(32.dp), 
                        tint = Color.Unspecified
                    ) 
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            AnimatedVisibility(visible = isSelected) {
                // Additional details can go here
            }
        }
    }
}

@Composable
fun HistoricalTrendsChart(daily: com.freetime.geoweather.network.models.DailyData, tempUnit: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(Res.string.last_7_days_trend), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            val maxTemps = daily.temperature2mMax
            val minTemps = daily.temperature2mMin
            
            if (maxTemps.isNotEmpty() && minTemps.isNotEmpty()) {
                val allTemps = maxTemps + minTemps
                val minVal = allTemps.minOrNull() ?: 0.0
                val maxVal = allTemps.maxOrNull() ?: 0.0
                val range = (maxVal - minVal).coerceAtLeast(1.0)

                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (maxTemps.size - 1)

                    val maxPath = Path()
                    val minPath = Path()

                    for (i in maxTemps.indices) {
                        val x = i * stepX
                        val yMax = height - ((maxTemps[i] - minVal) / range * height).toFloat()
                        val yMin = height - ((minTemps[i] - minVal) / range * height).toFloat()

                        if (i == 0) {
                            maxPath.moveTo(x, yMax)
                            minPath.moveTo(x, yMin)
                        } else {
                            maxPath.lineTo(x, yMax)
                            minPath.lineTo(x, yMin)
                        }
                    }

                    drawPath(maxPath, primaryColor, style = Stroke(width = 3.dp.toPx()))
                    drawPath(minPath, secondaryColor, style = Stroke(width = 3.dp.toPx()))
                }
            }
        }
    }
}

@Composable
fun MoonPhaseSection(dateStr: String) {
    val phase = calculateMoonPhase(dateStr)
    val phaseName = getMoonPhaseName(phase)
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(Res.string.moon_phase_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Simplified Moon representation
                Canvas(modifier = Modifier.size(48.dp)) {
                    drawCircle(color = Color.LightGray, radius = size.minDimension / 2)
                    // Simplified shadow based on phase
                    if (phase < 0.5) {
                        drawArc(
                            color = Color.DarkGray,
                            startAngle = 90f,
                            sweepAngle = 180f,
                            useCenter = true,
                            size = size
                        )
                    }
                }
                Column {
                    Text(phaseName, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(Res.string.illumination_label, (phase * 100).toInt()), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// Simple moon phase calculation (approximation)
fun calculateMoonPhase(dateStr: String): Double {
    return try {
        val date = LocalDate.parse(dateStr)
        val epoch = LocalDate(1970, 1, 1)
        val daysSinceEpoch = date.toEpochDays() - epoch.toEpochDays()
        val phase = (daysSinceEpoch % 29.53) / 29.53
        phase
    } catch (e: Exception) {
        0.5
    }
}

@Composable
fun getMoonPhaseName(phase: Double): String = when {
    phase < 0.06 || phase > 0.94 -> stringResource(Res.string.moon_new)
    phase < 0.19 -> stringResource(Res.string.moon_waxing_crescent)
    phase < 0.31 -> stringResource(Res.string.moon_first_quarter)
    phase < 0.44 -> stringResource(Res.string.moon_waxing_gibbous)
    phase < 0.56 -> stringResource(Res.string.moon_full)
    phase < 0.69 -> stringResource(Res.string.moon_waning_gibbous)
    phase < 0.81 -> stringResource(Res.string.moon_last_quarter)
    else -> stringResource(Res.string.moon_waning_crescent)
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// Helper functions for formatting
fun formatTemp(temp: Double, unit: String): String {
    val value = if (unit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()
    return "$value°"
}

fun formatWind(speed: Double, unit: String): String {
    val value = if (unit == "mph") (speed * 0.621371).toInt() else speed.toInt()
    val suffix = if (unit == "mph") " mph" else " km/h"
    return "$value$suffix"
}

fun formatTime(isoString: String): String {
    return try {
        val dt = LocalDateTime.parse(isoString)
        val hour = dt.hour.toString().padStart(2, '0')
        val minute = dt.minute.toString().padStart(2, '0')
        "$hour:$minute"
    } catch (e: Exception) {
        isoString.split("T").lastOrNull()?.take(5) ?: isoString
    }
}

fun formatDate(isoString: String): String {
    return try {
        val date = LocalDate.parse(isoString)
        val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
        "$dayName, ${date.dayOfMonth}. $monthName"
    } catch (e: Exception) {
        isoString
    }
}
