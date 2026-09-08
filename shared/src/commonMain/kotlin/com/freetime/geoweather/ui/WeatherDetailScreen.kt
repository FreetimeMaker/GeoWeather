package com.freetime.geoweather.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.WeatherIconMapper
import geoweather.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    locationId: Long,
    transientName: String? = null,
    transientLat: Double = 0.0,
    transientLon: Double = 0.0,
    viewModel: WeatherViewModel,
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val tempUnit by appSettings.tempUnit.collectAsState()
    val windUnit by appSettings.windUnit.collectAsState()
    val pressureUnit by appSettings.pressureUnit.collectAsState()
    val isTransient = transientName != null
    val dbLocation by viewModel.observeLocation(locationId).collectAsState(initial = null)
    var transientLocation by remember { mutableStateOf<LocationEntity?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun doRefresh() {
        isRefreshing = true
        if (isTransient) {
            viewModel.fetchTransientWeather(transientName!!, transientLat, transientLon) {
                transientLocation = it
                isRefreshing = false
            }
        } else {
            viewModel.refreshLocation(locationId) { isRefreshing = false }
        }
    }

    // Fetch on first open when there is no cached data yet
    LaunchedEffect(dbLocation?.weatherData, transientLocation?.weatherData) {
        val hasData = if (isTransient) transientLocation?.weatherData else dbLocation?.weatherData
        if (hasData == null && !isRefreshing) {
            doRefresh()
        }
    }

    val loc = if (isTransient) transientLocation else dbLocation
    val title = if (isTransient) transientName!! else loc?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                },
                actions = {
                    IconButton(onClick = { doRefresh() }, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.refresh_nav_desc))
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            loc == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            loc.weatherData == null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = stringResource(Res.string.error_loading_weather),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { doRefresh() }) {
                            Text(stringResource(Res.string.refresh_nav_desc))
                        }
                    }
                }
            }
            else -> {
                val hourly = remember(loc.weatherData) { viewModel.getHourlyForecasts(loc) }
                val daily = remember(loc.weatherData) { viewModel.getDailyForecasts(loc) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        val code = loc.currentWeatherCode
                        val rawTemp = loc.currentTemp
                        if (code != null) {
                            Icon(
                                painter = painterResource(WeatherIconMapper.getWeatherIcon(code)),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = Color.Unspecified
                            )
                        }
                        if (rawTemp != null) {
                            val displayTemp = if (tempUnit == "fahrenheit") (rawTemp * 9 / 5 + 32).toInt() else rawTemp.toInt()
                            val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                            Text(
                                text = "$displayTemp$tempSuffix",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (code != null) {
                            Text(
                                text = stringResource(WeatherCodes.getStringResource(code)),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                WeatherDetailItem(stringResource(Res.string.latitude_label), "${loc.latitude}")
                                WeatherDetailItem(stringResource(Res.string.longitude_label), "${loc.longitude}")
                            }
                        }
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                DetailInfoRow(
                                    label = stringResource(Res.string.wind_label),
                                    value = formatWind(loc.currentWindSpeed, loc.currentWindDirection, windUnit)
                                )
                                DetailInfoRow(
                                    label = stringResource(Res.string.humidity_label),
                                    value = loc.currentHumidity?.let {
                                        "$it${stringResource(Res.string.humidity_suffix)}"
                                    } ?: "--"
                                )
                                DetailInfoRow(
                                    label = stringResource(Res.string.pressure_label),
                                    value = formatPressure(loc.currentPressure, pressureUnit)
                                )
                            }
                        }
                    }

                    if (hourly.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.hourly_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(hourly) { hour ->
                                    Card {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(hour.time.take(5), style = MaterialTheme.typography.labelMedium)
                                            Icon(
                                                painter = painterResource(WeatherIconMapper.getWeatherIcon(hour.code)),
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = Color.Unspecified
                                            )
                                            Text(
                                                formatTemp(hour.temp.toDouble(), tempUnit),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (daily.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.forecast_3day_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        items(daily) { day ->
                            var expanded by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { expanded = !expanded }
                            ) {
                                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = day.date.takeLast(5),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            painter = painterResource(WeatherIconMapper.getWeatherIcon(day.code)),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = Color.Unspecified
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${formatTemp(day.minTemp.toDouble(), tempUnit)} / ${formatTemp(day.maxTemp.toDouble(), tempUnit)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (expanded) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                        DetailInfoRow(
                                            label = stringResource(Res.string.sunrise_label),
                                            value = day.sunrise.takeLast(5)
                                        )
                                        DetailInfoRow(
                                            label = stringResource(Res.string.sunset_label),
                                            value = day.sunset.takeLast(5)
                                        )
                                        DetailInfoRow(
                                            label = stringResource(Res.string.precipitation_label),
                                            value = "${day.precipSum} mm"
                                        )
                                        DetailInfoRow(
                                            label = stringResource(Res.string.precipitation_probability_label),
                                            value = "${day.precipProbMax} %"
                                        )
                                        DetailInfoRow(
                                            label = stringResource(Res.string.wind_max_label),
                                            value = formatWind(day.windMax, null, windUnit)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

fun formatTemp(celsius: Double, tempUnit: String): String {
    val display = if (tempUnit == "fahrenheit") (celsius * 9 / 5 + 32).toInt() else celsius.toInt()
    val suffix = if (tempUnit == "fahrenheit") "°F" else "°C"
    return "$display$suffix"
}

@Composable
fun formatWind(kmh: Double?, degrees: Int?, windUnit: String): String {
    if (kmh == null) return "--"
    val suffix = when (windUnit) {
        "mph" -> stringResource(Res.string.unit_mph)
        "ms" -> stringResource(Res.string.unit_ms)
        else -> stringResource(Res.string.unit_kmh)
    }
    val speed = when (windUnit) {
        "mph" -> kmh * 0.621371
        "ms" -> kmh / 3.6
        else -> kmh
    }
    val cardinal = degrees?.let {
        val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        dirs[(((it + 11.25) / 22.5).toInt()) % 16]
    }
    return if (cardinal != null) "${speed.toInt()} $suffix $cardinal" else "${speed.toInt()} $suffix"
}

@Composable
fun formatPressure(hpa: Double?, pressureUnit: String): String {
    if (hpa == null) return "--"
    return if (pressureUnit == "mmhg") {
        "${(hpa * 0.750062).toInt()} ${stringResource(Res.string.unit_mmhg)}"
    } else {
        "${hpa.toInt()} ${stringResource(Res.string.unit_hpa)}"
    }
}
