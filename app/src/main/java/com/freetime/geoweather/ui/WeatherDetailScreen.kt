package com.freetime.geoweather.ui

import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh\nimport androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource\nimport androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.WeatherIconMapper
import com.freetime.geoweather.R as Res
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    locationId: Long,
    transientName: String? = null,
    transientLat: Double = 0.0,
    transientLon: Double = 0.0,
    viewModel: WeatherViewModel,
    appSettings: AppSettings,
    onBack: () -> Unit,
    onRadarClick: (Double, Double) -> Unit
) {
    val tempUnit by appSettings.tempUnit.collectAsState()
    val windUnit by appSettings.windUnit.collectAsState()
    val pressureUnit by appSettings.pressureUnit.collectAsState()
    val animationMode by appSettings.weatherAnimations.collectAsState()\n    val context = LocalContext.current
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

    LaunchedEffect(dbLocation?.weatherData, transientLocation?.weatherData) {
        val hasData = if (isTransient) transientLocation?.weatherData else dbLocation?.weatherData
        if (hasData == null && !isRefreshing) {
            doRefresh()
        }
    }

    val loc = if (isTransient) transientLocation else dbLocation
    val title = if (isTransient) transientName!! else loc?.name ?: ""

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back_nav_desc))
                    }
                },
                actions = {
                    if (loc?.currentTemp != null) {
                        IconButton(onClick = {
                            val shareText = "${loc.name}: ${formatTemp(loc.currentTemp!!, tempUnit)} · ${loc.currentWeatherCode?.let { WeatherCodes.getDescription(it) } ?: ""}"
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share weather"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share weather")
                        }
                    }
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
                val extras = remember(loc.weatherData) { viewModel.getCurrentHourExtras(loc) }\n                var airExtras by remember(loc.id, loc.weatherData) { mutableStateOf<com.freetime.geoweather.data.CurrentHourExtras?>(null) }\n                LaunchedEffect(loc.id, loc.weatherData) { airExtras = viewModel.getAirQualityExtras(loc) }
                var forecastExpanded by remember { mutableStateOf(false) }
                val visibleDaily = if (forecastExpanded) daily else daily.take(7)
                val code = loc.currentWeatherCode
                val rawTemp = loc.currentTemp
                val animationsEnabled = animationMode != "off"
                val reducedMotion = animationMode == "reduced"
                val rainIntensity = when (code) {
                    in 51..55 -> .65f
                    in 61..65, in 80..82 -> 1.35f
                    in 95..99 -> 1.65f
                    else -> 1f
                }
                val firstDayForLight = daily.firstOrNull()
                val nowHour = java.time.LocalTime.now().hour
                val sunriseHour = firstDayForLight?.sunrise?.takeLast(5)?.take(2)?.toIntOrNull() ?: 7
                val sunsetHour = firstDayForLight?.sunset?.takeLast(5)?.take(2)?.toIntOrNull() ?: 19
                val isNight = nowHour < sunriseHour || nowHour >= sunsetHour
                Box(Modifier.fillMaxSize()) {
                    if (animationsEnabled && code != null) {
                        FullScreenWeatherBackground(
                            code = code,
                            windSpeed = loc.currentWindSpeed ?: 0.0,
                            windDirection = loc.currentWindDirection ?: 0,
                            intensity = if (reducedMotion) .45f else rainIntensity,
                            night = isNight
                        )
                    }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        if (code != null) {
                            AnimatedWeatherGlass(
                                code = code,
                                windSpeed = loc.currentWindSpeed ?: 0.0,
                                windDirection = loc.currentWindDirection ?: 0,
                                intensity = if (reducedMotion) .45f else rainIntensity,
                                night = isNight,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                            Icon(
                                painter = painterResource(WeatherIconMapper.getWeatherIcon(code)),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp).weatherFloatAnimation(),
                                tint = Color.Unspecified
                            )
                        }
                        if (rawTemp != null) {
                            val displayTemp = if (tempUnit == "fahrenheit") (rawTemp * 9 / 5 + 32).toInt() else rawTemp.toInt()
                            val tempSuffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                            val animatedTemp by animateIntAsState(
                                targetValue = displayTemp,
                                animationSpec = spring(dampingRatio = .75f, stiffness = 120f),
                                label = "temperature"
                            )
                            Text(
                                text = "$animatedTemp$tempSuffix",
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

                    if (code != null) {
                        item { WeatherAlertsSection(code) }
                    }

                    airExtras?.let { air ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Air quality & pollen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        WeatherDetailItem("AQI", air.europeanAqi?.toString() ?: "--", modifier = Modifier.weight(1f))
                                        WeatherDetailItem("PM2.5", air.pm25?.let { "${it.roundToInt()} µg/m³" } ?: "--", modifier = Modifier.weight(1f))
                                        WeatherDetailItem("PM10", air.pm10?.let { "${it.roundToInt()} µg/m³" } ?: "--", modifier = Modifier.weight(1f))
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        WeatherDetailItem("Alder", air.alderPollen?.let { "${it.roundToInt()}" } ?: "--", modifier = Modifier.weight(1f))
                                        WeatherDetailItem("Birch", air.birchPollen?.let { "${it.roundToInt()}" } ?: "--", modifier = Modifier.weight(1f))
                                        WeatherDetailItem("Grass", air.grassPollen?.let { "${it.roundToInt()}" } ?: "--", modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (hourly.isNotEmpty()) {
                        item {
                            val nextRainIndex = hourly.indexOfFirst { it.precipProbability >= 40 || it.code in 51..67 || it.code in 80..82 || it.code in 95..99 }
                            val nextRainText = if (nextRainIndex >= 0) {
                                val next = hourly[nextRainIndex]
                                if (nextRainIndex == 0) "Rain possible now · ${next.precipProbability}%"
                                else "Next rain around ${next.time} · ${next.precipProbability}%"
                            } else "No rain expected in the next 24 hours"
                            Card(
                                modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Next rain", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(nextRainText, style = MaterialTheme.typography.bodyLarge)
                                    Text("24-hour timeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        items(hourly) { hour ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(hour.time, style = MaterialTheme.typography.labelSmall)
                                                Icon(
                                                    painter = painterResource(WeatherIconMapper.getWeatherIcon(hour.code)),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp),
                                                    tint = Color.Unspecified
                                                )
                                                Text("${hour.temp}°", fontWeight = FontWeight.Bold)
                                                if (hour.precipProbability > 0) {
                                                    Text("${hour.precipProbability}%", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val feelsLike = loc.currentFeelsLike ?: loc.currentTemp
                        Card(
                            modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.wind_label),
                                        value = formatWind(loc.currentWindSpeed, loc.currentWindDirection, windUnit),
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.feels_like_label),
                                        value = feelsLike?.let { formatTemp(it, tempUnit) } ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.humidity_label),
                                        value = loc.currentHumidity?.let {
                                            "$it${stringResource(Res.string.humidity_suffix)}"
                                        } ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(stringResource(Res.string.wind_direction_label), style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            loc.currentWindDirection?.let { WindCompass(direction = it.toFloat()) }
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = loc.currentWindDirection?.let { cardinal8(it.toFloat()) } ?: "--",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.gusts_label),
                                        value = formatWind(loc.currentWindGusts, null, windUnit),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.weight(1f))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.pressure_label),
                                        value = formatPressure(loc.currentPressure, pressureUnit),
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.visibility_label),
                                        value = extras?.visibilityKm?.let { v ->
                                            "${(v * 10).roundToInt() / 10.0} km"
                                        } ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = "UV",
                                        value = extras?.uvIndex?.let { uv ->
                                            "${(uv * 10).roundToInt() / 10.0} · " + when {
                                                uv < 3 -> "Low"
                                                uv < 6 -> "Moderate"
                                                uv < 8 -> "High"
                                                uv < 11 -> "Very high"
                                                else -> "Extreme"
                                            }
                                        } ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.cloud_base_label),
                                        value = extras?.cloudBaseM?.let { "${it.toInt()} m" } ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    val firstDay = daily.firstOrNull()
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.sunrise_label),
                                        value = firstDay?.sunrise?.takeLast(5) ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.sunset_label),
                                        value = firstDay?.sunset?.takeLast(5) ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.weight(1f))
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    val firstDay = daily.firstOrNull()
                                    val golden = if (firstDay != null && firstDay.sunrise != "--" && firstDay.sunset != "--") {
                                        "${shiftTime(firstDay.sunrise.takeLast(5), 30)} - ${shiftTime(firstDay.sunset.takeLast(5), -60)}"
                                    } else "--:--"
                                    val blue = if (firstDay != null && firstDay.sunrise != "--" && firstDay.sunset != "--") {
                                        "${shiftTime(firstDay.sunrise.takeLast(5), -30)} / ${shiftTime(firstDay.sunset.takeLast(5), 30)}"
                                    } else "--:--"
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.golden_hour_label),
                                        value = golden,
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.blue_hour_label),
                                        value = blue,
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.altitude_label),
                                        value = "${loc.elevation.toInt()} m",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    val trendText = when (extras?.pressureTrend) {
                                        1 -> "↗ ${stringResource(Res.string.pressure_rising)}"
                                        -1 -> "↘ ${stringResource(Res.string.pressure_falling)}"
                                        else -> "→ ${stringResource(Res.string.pressure_stable)}"
                                    }
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.pressure_trend_label),
                                        value = trendText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        label = stringResource(Res.string.timezone_label),
                                        value = "${loc.timezoneName} (${loc.timezoneAbbr})",
                                        modifier = Modifier.weight(2f)
                                    )
                                }
                                Button(
                                    onClick = { onRadarClick(loc.latitude, loc.longitude) },
                                    modifier = Modifier.fillMaxWidth().height(40.dp).geoWeatherGlass(RoundedCornerShape(20.dp)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        stringResource(Res.string.open_weather_radar),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
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
                                items(hourly, key = { it.time }) { hour ->
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
                        items(visibleDaily, key = { it.date }) { day ->
                            var expanded by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .animateContentSize(animationSpec = spring())
                                    .geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column {
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
                        if (daily.size > 7) {
                            item {
                                TextButton(
                                    onClick = { forecastExpanded = !forecastExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (forecastExpanded) {
                                            stringResource(Res.string.show_less_forecast)
                                        } else {
                                            stringResource(Res.string.show_more_forecast)
                                        },
                                        style = MaterialTheme.typography.labelLarge
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
fun WeatherDetailItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
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
    val speedText = when (windUnit) {
        "mph" -> "${(kmh * 0.621371).toInt()}"
        "ms" -> "${(kmh / 3.6 * 10).roundToInt() / 10.0}"
        else -> "${kmh.toInt()}"
    }
    val cardinal = degrees?.let {
        val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        dirs[(((it + 11.25) / 22.5).toInt()) % 16]
    }
    return if (cardinal != null) "$speedText $suffix $cardinal" else "$speedText $suffix"
}

@Composable
fun WeatherAlertsSection(code: Int) {
    val alertRes = when (code) {
        in 95..99 -> Res.string.alert_thunderstorm
        in 71..86 -> Res.string.alert_snow
        else -> null
    }

    if (alertRes != null) {
        Card(
            modifier = SevereWeatherPulse(Modifier.fillMaxWidth()).geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚠️", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(Res.string.weather_alerts_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        stringResource(alertRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun WindCompass(direction: Float) {
    val animatedDirection by animateFloatAsState(
        targetValue = direction,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 180f),
        label = "windDirection"
    )
    Canvas(modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = animatedDirection - direction }) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        drawCircle(color = Color.Gray.copy(alpha = 0.3f), radius = radius)
        val angleRad = (direction - 90) * kotlin.math.PI / 180.0
        val arrowLength = radius * 0.8f
        val end = Offset(
            (center.x + arrowLength * cos(angleRad)).toFloat(),
            (center.y + arrowLength * sin(angleRad)).toFloat()
        )
        drawLine(color = Color.Red, start = center, end = end, strokeWidth = 3.dp.toPx())
    }
}

@Composable
fun cardinal8(degrees: Float): String {
    val dirs = listOf(
        Res.string.dir_n, Res.string.dir_ne, Res.string.dir_e, Res.string.dir_se,
        Res.string.dir_s, Res.string.dir_sw, Res.string.dir_w, Res.string.dir_nw, Res.string.dir_n
    )
    val index = (((degrees + 22.5) / 45).toInt()).coerceIn(0, 8)
    return stringResource(dirs[index])
}

fun shiftTime(hhmm: String, minutes: Int): String {
    return try {
        val parts = hhmm.split(":")
        var total = parts[0].toInt() * 60 + parts[1].toInt() + minutes
        total = ((total % 1440) + 1440) % 1440
        "${(total / 60).toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}"
    } catch (e: Exception) {
        "--:--"
    }
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
