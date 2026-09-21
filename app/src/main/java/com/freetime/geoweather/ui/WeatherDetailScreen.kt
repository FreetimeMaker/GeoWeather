package com.freetime.geoweather.ui
import me.free_time.design.FreetimeDesign
import me.free_time.design.FreetimeGlassTopBar
import me.free_time.design.FreetimeGlassAction
import me.free_time.design.FreetimeGlassPanel
import me.free_time.design.freetimeGlass
import me.free_time.design.FreetimeGlassDepth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.HourlyForecast
import com.freetime.geoweather.data.DailyForecast
import com.freetime.geoweather.WeatherCodes
import com.freetime.geoweather.WeatherIconMapper
import com.freetime.geoweather.isNetworkAvailable
import com.freetime.geoweather.R as Res
import me.free_time.design.freetimeGlass
import me.free_time.design.FreetimeGlassTopBar
import me.free_time.design.FreetimeGlassPanel
import me.free_time.design.FreetimeGlassAction
import com.freetime.geoweather.ui.glass.GeoWeatherGlassIconAction
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import me.free_time.design.FreetimeProgressIndicator
import me.free_time.design.FreetimeDialog

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
    onRadarClick: (Double, Double) -> Unit,
    onHourlyClick: (String, List<HourlyForecast>, Int) -> Unit,
    onDailyClick: (String, List<DailyForecast>, Int) -> Unit
) {
    val tempUnit by appSettings.tempUnit.collectAsState()
    val windUnit by appSettings.windUnit.collectAsState()
    val pressureUnit by appSettings.pressureUnit.collectAsState()
    val animationMode by appSettings.weatherAnimations.collectAsState()
    val oledBlack by appSettings.oledBlack.collectAsState()
    val haptics = LocalHapticFeedback.current
    val detailListState = rememberLazyListState()
    val detailTopBarCompact by remember { derivedStateOf { detailListState.firstVisibleItemIndex > 0 || detailListState.firstVisibleItemScrollOffset > 140 } }
    val context = LocalContext.current
    val isTransient = transientName != null
    val dbLocation by viewModel.observeLocation(locationId).collectAsState(initial = null)
    var transientLocation by remember { mutableStateOf<LocationEntity?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isWideLayout = configuration.screenWidthDp >= 700 ||
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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

    if (showDetailSheet && loc?.weatherData != null) {
        val sheetHourly = remember(loc.weatherData) { viewModel.getHourlyForecasts(loc) }
        val sheetExtras = remember(loc.weatherData) { viewModel.getCurrentHourExtras(loc) }
        FreetimeDialog(
            title = stringResource(Res.string.details_sheet_title),
            onDismissRequest = { showDetailSheet = false },
            confirmText = stringResource(android.R.string.ok),
            onConfirm = { showDetailSheet = false },
            text = buildString {
                val current = sheetHourly.firstOrNull()
                append(stringResource(Res.string.visibility_label) + ": " + (sheetExtras?.visibilityKm?.let { String.format(java.util.Locale.US, "%.1f km", it) } ?: "--"))
                append("\n" + stringResource(Res.string.gusts_label) + ": " + (current?.windGusts?.let { it.toInt().toString() + " km/h" } ?: "--"))
                append("\n" + stringResource(Res.string.uv_max_label) + ": " + (sheetExtras?.uvIndex?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "--"))
                append("\n" + stringResource(Res.string.cloud_base_label) + ": " + (sheetExtras?.cloudBaseM?.let { it.roundToInt().toString() + " m" } ?: "--"))
                append("\n" + stringResource(Res.string.humidity_label) + ": " + (loc.currentHumidity?.let { it.toString() + "%" } ?: "--"))
            }
        )
    }    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            FreetimeGlassTopBar(
                title = title,
                onBack = onBack,
                compact = detailTopBarCompact,
                compactSubtitle = if (detailTopBarCompact) loc?.currentTemp?.let { formatTemp(it, tempUnit) } else null,
                actions = {
                    if (loc?.currentTemp != null) {
                        GeoWeatherGlassIconAction(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val description = loc.currentWeatherCode?.let { context.getString(WeatherCodes.getStringResource(it)) } ?: ""
                            val bitmap = createWeatherShareCard(
                                title = loc.name,
                                temperature = formatTemp(loc.currentTemp!!, tempUnit),
                                description = description,
                                subtitle = loc.currentHumidity?.let { "Humidity " + it + "%" } ?: ""
                            )
                            shareWeatherCard(context, bitmap, loc.name)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.share_weather))
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
                    FreetimeProgressIndicator()
                }
            }
            loc.weatherData == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = if (isWideLayout) 28.dp else 16.dp, vertical = 16.dp)
                        .widthIn(max = if (isWideLayout) 1180.dp else 760.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isRefreshing) {
                        FreetimeProgressIndicator()
                    } else {
                        Text(
                            text = stringResource(Res.string.error_loading_weather),
                            textAlign = TextAlign.Center,
                            style = FreetimeDesign.typography.bodyLarge,
                            color = FreetimeDesign.palette.contentMuted
                        )
                        Spacer(Modifier.height(16.dp))
                        FreetimeGlassAction(onClick = { doRefresh() }) {
                            Text(stringResource(Res.string.refresh_nav_desc))
                        }
                    }
                }
            }
            else -> {
                val hourly = remember(loc.weatherData) { viewModel.getHourlyForecasts(loc) }
                val daily = remember(loc.weatherData) { viewModel.getDailyForecasts(loc) }
                val extras = remember(loc.weatherData) { viewModel.getCurrentHourExtras(loc) }
                val weatherHistory by viewModel.getWeatherHistory(loc.name).collectAsState(initial = emptyList())
                var airExtras by remember(loc.id, loc.weatherData) { mutableStateOf<com.freetime.geoweather.data.CurrentHourExtras?>(null) }
                LaunchedEffect(loc.id, loc.weatherData) { airExtras = viewModel.getAirQualityExtras(loc) }
                var forecastExpanded by remember { mutableStateOf(false) }
                val visibleDaily = if (forecastExpanded) daily else daily.take(7)
                val code = loc.currentWeatherCode
                val rawTemp = loc.currentTemp
                // Temporarily disable weather scene animations for GPU crash isolation.
                // Liquid Glass/Backdrop remains enabled.
                val animationsEnabled = animationMode != "off"
                val reducedMotion = animationMode == "reduced"
                val rainIntensity = when (code) {
                    in 51..55 -> .65f
                    in 61..65, in 80..82 -> 1.35f
                    in 95..99 -> 1.65f
                    else -> 1f
                }
                val firstDayForLight = daily.firstOrNull()
                val nowTime = java.time.LocalTime.now()
                val sunriseTime = runCatching { java.time.LocalTime.parse(firstDayForLight?.sunrise?.takeLast(5) ?: "07:00") }.getOrDefault(java.time.LocalTime.of(7, 0))
                val sunsetTime = runCatching { java.time.LocalTime.parse(firstDayForLight?.sunset?.takeLast(5) ?: "19:00") }.getOrDefault(java.time.LocalTime.of(19, 0))
                val isNight = nowTime.isBefore(sunriseTime) || !nowTime.isBefore(sunsetTime)
                val twilight = twilightAmount(nowTime, sunriseTime, sunsetTime)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (oledBlack && isNight) Brush.verticalGradient(listOf(Color.Black, Color.Black, Color(0xFF07162E).copy(alpha = .18f))) else weatherBackdropBrush(code, isNight, twilight))
                ) {
                    if (animationsEnabled && code != null) {
                        FullScreenWeatherBackground(
                            code = code,
                            windSpeed = loc.currentWindSpeed ?: 0.0,
                            windDirection = loc.currentWindDirection ?: 0,
                            intensity = if (reducedMotion) .45f else rainIntensity,
                            night = isNight,
                            reducedMotion = reducedMotion
                        )
                    }
                androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { doRefresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                LazyColumn(
                    state = detailListState,
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        val minutesAgo = ((System.currentTimeMillis() - loc.lastUpdated).coerceAtLeast(0L) / 60_000L).toInt()
                        if (!isNetworkAvailable() && loc.weatherData != null) {
                            Text(
                                stringResource(Res.string.offline_cached_weather),
                                modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(18.dp), interactive = false).padding(12.dp),
                                textAlign = TextAlign.Center,
                                style = FreetimeDesign.typography.labelLarge
                            )
                        } else if (loc.lastUpdated > 0L) {
                            Text(
                                stringResource(Res.string.last_updated_minutes, minutesAgo),
                                style = FreetimeDesign.typography.labelSmall,
                                color = FreetimeDesign.palette.contentMuted
                            )
                        }
                    }

                    item {
                        FreetimeGlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            depth = FreetimeGlassDepth.ELEVATED
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (code != null && animationsEnabled) {
                                    AnimatedWeatherGlass(
                                        code = code,
                                        windSpeed = loc.currentWindSpeed ?: 0.0,
                                        windDirection = loc.currentWindDirection ?: 0,
                                        intensity = if (reducedMotion) .45f else rainIntensity,
                                        night = isNight,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (code != null) {
                                    Icon(
                                        painter = painterResource(weatherIconForTime(code, isNight)),
                                        contentDescription = stringResource(WeatherCodes.getStringResource(code)),
                                        modifier = Modifier.size(104.dp),
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
                                        style = FreetimeDesign.typography.displayLarge,
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
                        }
                    }

                    if (code != null) {
                        item { WeatherAlertsSection(loc.name, code, hourly, extras) }
                    }

                    item {
                        val ageMinutes = ((System.currentTimeMillis() - loc.lastUpdated).coerceAtLeast(0L) / 60_000L).toInt()
                        val freshness = when {
                            !isNetworkAvailable() -> stringResource(Res.string.freshness_cached)
                            ageMinutes <= 15 -> stringResource(Res.string.freshness_live)
                            ageMinutes <= 60 -> stringResource(Res.string.freshness_recent)
                            else -> stringResource(Res.string.freshness_stale)
                        }
                        FreetimeGlassPanel(modifier = Modifier.fillMaxWidth(), depth = FreetimeGlassDepth.SUBTLE) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(freshness, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource(Res.string.updated_age_short, ageMinutes), style = FreetimeDesign.typography.labelMedium, color = FreetimeDesign.palette.contentMuted)
                            }
                        }
                    }

                    item {
                        FreetimeGlassPanel(modifier = Modifier.fillMaxWidth(), depth = FreetimeGlassDepth.STANDARD) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(Res.string.weather_history_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (weatherHistory.isEmpty()) {
                                    Text(stringResource(Res.string.weather_history_empty), color = FreetimeDesign.palette.contentMuted)
                                } else {
                                    val recent = weatherHistory.take(24)
                                    val low = recent.minOf { it.temperature }
                                    val high = recent.maxOf { it.temperature }
                                    Text(stringResource(Res.string.history_samples, weatherHistory.size), style = FreetimeDesign.typography.labelMedium)
                                    Text(
                                        stringResource(Res.string.history_range, formatTemp(low, tempUnit), formatTemp(high, tempUnit)),
                                        style = FreetimeDesign.typography.bodyLarge
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(72.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        val spread = (high - low).takeIf { it > 0.1 } ?: 1.0
                                        recent.reversed().forEach { sample ->
                                            val fraction = ((sample.temperature - low) / spread).toFloat().coerceIn(0f, 1f)
                                            Box(
                                                Modifier
                                                    .weight(1f)
                                                    .height((12 + 56 * fraction).dp)
                                                    .freetimeGlass(RoundedCornerShape(8.dp), interactive = false)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        val currentHour = hourly.firstOrNull()
                        val feelsReason = when {
                            (currentHour?.windSpeed ?: loc.currentWindSpeed ?: 0.0) >= 25.0 -> stringResource(Res.string.feels_like_wind)
                            (loc.currentHumidity ?: 0) >= 75 -> stringResource(Res.string.feels_like_humidity)
                            else -> stringResource(Res.string.feels_like_neutral)
                        }
                        val pressureTrend = when (extras?.pressureTrend ?: 0) {
                            1 -> stringResource(Res.string.pressure_rising)
                            -1 -> stringResource(Res.string.pressure_falling)
                            else -> stringResource(Res.string.pressure_steady)
                        }
                        FreetimeGlassPanel(modifier = Modifier.fillMaxWidth(), depth = FreetimeGlassDepth.STANDARD) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(Res.string.conditions_insights_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    WeatherDetailItem(
                                        stringResource(Res.string.visibility_label),
                                        extras?.visibilityKm?.let { String.format(java.util.Locale.US, "%.1f km", it) } ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(
                                        stringResource(Res.string.cloud_base_label),
                                        extras?.cloudBaseM?.let { it.roundToInt().toString() + " m" } ?: "--",
                                        modifier = Modifier.weight(1f)
                                    )
                                    WeatherDetailItem(stringResource(Res.string.pressure_trend_label), pressureTrend, modifier = Modifier.weight(1f))
                                }
                                Text(stringResource(Res.string.feels_like_reason_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(feelsReason, color = FreetimeDesign.palette.contentMuted)
                            }
                        }
                    }

                    item {
                        val smart = com.freetime.geoweather.WeatherIntelligence.smartHero(hourly, daily.firstOrNull())
                        val nowcast = com.freetime.geoweather.WeatherIntelligence.nowcast(hourly)
                        FreetimeGlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            depth = FreetimeGlassDepth.ELEVATED
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(stringResource(Res.string.smart_weather_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(smart.primary, style = MaterialTheme.typography.headlineSmall)
                                smart.secondary?.let { Text(it, color = FreetimeDesign.palette.contentMuted) }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f))
                                Text(stringResource(Res.string.nowcast_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (nowcast?.startsAt != null && nowcast.endsAt != null) {
                                    Text(
                                        stringResource(
                                            Res.string.nowcast_wet,
                                            nowcast.startsAt,
                                            nowcast.endsAt,
                                            nowcast.peakProbability,
                                            String.format(java.util.Locale.US, "%.1f", nowcast.peakAmountMm)
                                        )
                                    )
                                } else {
                                    Text(stringResource(Res.string.nowcast_dry))
                                }
                            }
                        }
                    }

                    if (hourly.isNotEmpty()) {
                        item {
                            FreetimeGlassPanel(modifier = Modifier.fillMaxWidth(), depth = FreetimeGlassDepth.STANDARD) {
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(Res.string.weather_timeline_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(hourly.take(12)) { hour ->
                                            FreetimeGlassPanel(
                                                modifier = Modifier.width(94.dp),
                                                depth = FreetimeGlassDepth.SUBTLE,
                                                interactive = false
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(hour.time, style = FreetimeDesign.typography.labelMedium)
                                                    Text(formatTemp(hour.temp.toDouble(), tempUnit), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        stringResource(
                                                            Res.string.precipitation_short,
                                                            hour.precipProbability,
                                                            String.format(java.util.Locale.US, "%.1f", hour.precipitation ?: 0.0)
                                                        ),
                                                        style = FreetimeDesign.typography.labelSmall
                                                    )
                                                    hour.windGusts?.let {
                                                        Text(stringResource(Res.string.gusts_label) + " " + it.toInt() + " km/h", style = FreetimeDesign.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    firstDayForLight?.let { day ->
                        item {
                            val sunrise = sunriseTime
                            val sunset = sunsetTime
                            val morningGoldenEnd = sunrise.plusHours(1)
                            val eveningGoldenStart = sunset.minusHours(1)
                            val morningBlueStart = sunrise.minusMinutes(40)
                            val eveningBlueEnd = sunset.plusMinutes(40)
                            FreetimeGlassPanel(modifier = Modifier.fillMaxWidth(), depth = FreetimeGlassDepth.STANDARD) {
                                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(Res.string.sun_path_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        WeatherDetailItem(stringResource(Res.string.sunrise_label), sunrise.toString(), modifier = Modifier.weight(1f))
                                        WeatherDetailItem(stringResource(Res.string.sunset_label), sunset.toString(), modifier = Modifier.weight(1f))
                                        WeatherDetailItem(stringResource(Res.string.uv_max_label), day.uvMax?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "--", modifier = Modifier.weight(1f))
                                    }
                                    Text(stringResource(Res.string.blue_hour_morning) + ": " + morningBlueStart + " – " + sunrise, style = FreetimeDesign.typography.bodySmall)
                                    Text(stringResource(Res.string.golden_hour_morning) + ": " + sunrise + " – " + morningGoldenEnd, style = FreetimeDesign.typography.bodySmall)
                                    Text(stringResource(Res.string.golden_hour_evening) + ": " + eveningGoldenStart + " – " + sunset, style = FreetimeDesign.typography.bodySmall)
                                    Text(stringResource(Res.string.blue_hour_evening) + ": " + sunset + " – " + eveningBlueEnd, style = FreetimeDesign.typography.bodySmall)
                                }
                            }
                        }
                    }

                    item {
                        val moon = moonPhaseDetails(java.time.LocalDate.now())
                        FreetimeGlassPanel(modifier = Modifier.fillMaxWidth(), depth = FreetimeGlassDepth.SUBTLE) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(moon.icon, style = MaterialTheme.typography.displaySmall)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(moon.name, style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${moon.illumination}% illuminated", style = FreetimeDesign.typography.bodyMedium)
                                    Text("Next full moon: ${moon.daysToFull} d · new moon: ${moon.daysToNew} d", style = FreetimeDesign.typography.labelSmall)
                                }
                            }
                        }
                    }

                                        // Daily astronomy and trip details live in the dedicated daily forecast screen.

                    airExtras?.let { air ->
                        item {
                            FreetimeGlassPanel(
                                modifier = Modifier.fillMaxWidth()
                                ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(Res.string.air_quality_pollen_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        WeatherDetailItem(stringResource(Res.string.aqi_label), air.europeanAqi?.toString() ?: "--", modifier = Modifier.weight(1f))
                                        WeatherDetailItem(stringResource(Res.string.pm25_label), air.pm25?.let { "${it.roundToInt()} µg/m³" } ?: "--", modifier = Modifier.weight(1f))
                                        WeatherDetailItem(stringResource(Res.string.pm10_label), air.pm10?.let { "${it.roundToInt()} µg/m³" } ?: "--", modifier = Modifier.weight(1f))
                                    }
                                    Spacer(Modifier.height(1.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        WeatherDetailItem(stringResource(Res.string.alder_pollen_label), air.alderPollen?.let { "${it.roundToInt()}" } ?: "--", modifier = Modifier.weight(1f))
                                        WeatherDetailItem(stringResource(Res.string.birch_pollen_label), air.birchPollen?.let { "${it.roundToInt()}" } ?: "--", modifier = Modifier.weight(1f))
                                        WeatherDetailItem(stringResource(Res.string.grass_pollen_label), air.grassPollen?.let { "${it.roundToInt()}" } ?: "--", modifier = Modifier.weight(1f))
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
                                if (nextRainIndex == 0) stringResource(Res.string.rain_possible_now, next.precipProbability)
                                else stringResource(Res.string.next_rain_around, next.time, next.precipProbability)
                            } else stringResource(Res.string.no_rain_24h)
                            FreetimeGlassPanel(
                                modifier = Modifier.fillMaxWidth()
                                ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(stringResource(Res.string.next_rain_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(nextRainText, style = FreetimeDesign.typography.bodyLarge)
                                    Text(stringResource(Res.string.timeline_24h), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        hourly.forEachIndexed { index, hour ->
                                            val previous = hourly.getOrNull(index - 1)
                                            val marksSunset = previous != null &&
                                                previous.time.takeLast(5) < sunsetTime.toString().take(5) &&
                                                hour.time.takeLast(5) >= sunsetTime.toString().take(5)
                                            if (marksSunset) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("🌇", style = FreetimeDesign.typography.titleMedium)
                                                    Text(sunsetTime.toString().take(5), style = FreetimeDesign.typography.labelSmall)
                                                }
                                            }
                                            FreetimeGlassPanel(depth = FreetimeGlassDepth.SUBTLE) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(hour.time.takeLast(5), style = FreetimeDesign.typography.labelSmall)
                                                    Icon(
                                                        painter = painterResource(WeatherIconMapper.getWeatherIcon(hour.code)),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp),
                                                        tint = Color.Unspecified
                                                    )
                                                    Text("${hour.temp}°", fontWeight = FontWeight.Bold)
                                                    if (hour.precipProbability > 0) {
                                                        Text("${hour.precipProbability}%", style = FreetimeDesign.typography.labelSmall)
                                                    }
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
                        FreetimeGlassPanel(
                            modifier = Modifier.fillMaxWidth()
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
                                Spacer(Modifier.height(1.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text(stringResource(Res.string.wind_direction_label), style = FreetimeDesign.typography.labelSmall)
                                        Spacer(Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            loc.currentWindDirection?.let { WindCompass(direction = it.toFloat()) }
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = loc.currentWindDirection?.let { cardinal8(it.toFloat()) } ?: "--",
                                                style = FreetimeDesign.typography.bodyMedium,
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
                                Spacer(Modifier.height(1.dp))
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
                                Spacer(Modifier.height(1.dp))
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
                                Spacer(Modifier.height(1.dp))
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
                                Spacer(Modifier.height(1.dp))
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
                                FreetimeGlassPanel(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onRadarClick(loc.latitude, loc.longitude)
                                    },
                                    interactive = true,
                                    depth = FreetimeGlassDepth.SUBTLE
                                ) {
                                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🌧️  ·  📡", style = FreetimeDesign.typography.headlineMedium)
                                        Text(stringResource(Res.string.open_weather_radar), style = FreetimeDesign.typography.labelLarge)
                                    }
                                }
                                /*
                                FreetimeGlassAction(
                                    onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onRadarClick(loc.latitude, loc.longitude) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        stringResource(Res.string.open_weather_radar),
                                        style = FreetimeDesign.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                */
                            }
                        }
                    }

                    if (hourly.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.hourly_label),
                                style = FreetimeDesign.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(hourly.size, key = { hourly[it].time }) { hourIndex ->
                                    val hour = hourly[hourIndex]
                                    FreetimeGlassPanel(
                                        modifier = Modifier.clickable { onHourlyClick(loc.name, hourly, hourIndex) },
                                        interactive = true,
                                        depth = FreetimeGlassDepth.SUBTLE
                                        ) {
                                        Column(
                                            modifier = Modifier,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(hour.time.take(5), style = FreetimeDesign.typography.labelMedium)
                                            Icon(
                                                painter = painterResource(WeatherIconMapper.getWeatherIcon(hour.code)),
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = Color.Unspecified
                                            )
                                            Text(
                                                formatTemp(hour.temp.toDouble(), tempUnit),
                                                style = FreetimeDesign.typography.bodyMedium
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
                                style = FreetimeDesign.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        items(visibleDaily.size, key = { visibleDaily[it].date }) { dayIndex ->
                            val day = visibleDaily[dayIndex]
                            var expanded by remember { mutableStateOf(false) }
                            FreetimeGlassPanel(
                                modifier = Modifier.fillMaxWidth()
                                    .animateContentSize(animationSpec = spring())
                                    .clickable { onDailyClick(loc.name, daily, daily.indexOf(day)) },
                                interactive = true,
                                depth = FreetimeGlassDepth.SUBTLE
                                ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = day.date.takeLast(5),
                                            style = FreetimeDesign.typography.bodyMedium,
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
                                            style = FreetimeDesign.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column {
                                        Spacer(Modifier.height(1.dp))
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
                                FreetimeGlassAction(
                                    onClick = { forecastExpanded = !forecastExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (forecastExpanded) {
                                            stringResource(Res.string.show_less_forecast)
                                        } else {
                                            stringResource(Res.string.show_more_forecast)
                                        },
                                        style = FreetimeDesign.typography.labelLarge
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
}


private fun weatherBackdropBrush(code: Int?, isNight: Boolean, twilight: Float): Brush {
    val colors = when {
        isNight -> listOf(Color(0xFF07162E).copy(alpha = .58f), Color(0xFF18345C).copy(alpha = .34f), Color.Transparent)
        code in 95..99 -> listOf(Color(0xFF252A3D).copy(alpha = .48f), Color(0xFF48536C).copy(alpha = .30f), Color.Transparent)
        code in 51..67 || code in 80..82 -> listOf(Color(0xFF355B78).copy(alpha = .38f), Color(0xFF7AA6C2).copy(alpha = .22f), Color.Transparent)
        code in 71..77 || code in 85..86 -> listOf(Color(0xFFB8C9D8).copy(alpha = .30f), Color(0xFFE4EDF4).copy(alpha = .18f), Color.Transparent)
        code == 0 || code == 1 -> listOf(Color(0xFFFFC86B).copy(alpha = .24f), Color(0xFF87C8F5).copy(alpha = .20f), Color.Transparent)
        else -> listOf(Color(0xFF8EA9BC).copy(alpha = .20f), Color(0xFFB7C6D0).copy(alpha = .14f), Color.Transparent)
    }
    val dusk = Color(0xFFFF8A65).copy(alpha = .30f * twilight)
    return Brush.verticalGradient(if (twilight > .01f) listOf(colors.first(), dusk, colors.last()) else colors)
}

private fun twilightAmount(now: java.time.LocalTime, sunrise: java.time.LocalTime, sunset: java.time.LocalTime): Float {
    fun minutes(t: java.time.LocalTime) = t.hour * 60 + t.minute
    val n = minutes(now)
    val rise = minutes(sunrise)
    val set = minutes(sunset)
    val distance = minOf(kotlin.math.abs(n - rise), kotlin.math.abs(n - set))
    return (1f - distance / 30f).coerceIn(0f, 1f)
}

@androidx.annotation.DrawableRes
private fun weatherIconForTime(code: Int, isNight: Boolean): Int = when (code) {
    0 -> if (isNight) Res.drawable.google_clear_night else Res.drawable.google_clear_day
    1 -> if (isNight) Res.drawable.google_mostly_clear_night else Res.drawable.google_mostly_clear_day
    2 -> if (isNight) Res.drawable.google_partly_cloudy_night else Res.drawable.google_partly_cloudy_day
    3 -> Res.drawable.google_cloudy
    45, 48 -> Res.drawable.google_fog
    51, 53, 55 -> Res.drawable.google_drizzle
    61, 63, 65 -> if (isNight) Res.drawable.google_rain_with_sunny_dark else Res.drawable.google_rain_with_sunny_light
    71, 73, 75 -> if (isNight) Res.drawable.google_snow_with_sunny_dark else Res.drawable.google_snow_with_sunny_light
    else -> if (isNight) Res.drawable.google_cloudy_with_sunny_dark else Res.drawable.google_cloudy_with_sunny_light
}

@Composable
fun WeatherDetailItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = FreetimeDesign.typography.labelSmall)
        Text(value, style = FreetimeDesign.typography.bodyMedium, color = valueColor)
    }
}

@Composable
fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = FreetimeDesign.typography.bodyMedium)
        Text(value, style = FreetimeDesign.typography.bodyMedium)
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
fun WeatherAlertsSection(
    locationName: String,
    code: Int,
    hourly: List<HourlyForecast>,
    extras: com.freetime.geoweather.data.CurrentHourExtras?
) {
    val alerts = buildList {
        when (code) {
            99 -> add(Res.string.alert_hail_thunderstorm)
            95, 96 -> add(Res.string.alert_thunderstorm)
            65 -> add(Res.string.alert_heavy_rain)
            66, 67 -> add(Res.string.alert_freezing_rain)
            75, 77 -> add(Res.string.alert_snow)
            82 -> add(Res.string.alert_rain_showers)
            86 -> add(Res.string.alert_snow_showers)
            45, 48 -> add(Res.string.alert_fog)
        }
        val nearTerm = hourly.take(6)
        if (nearTerm.any { (it.windGusts ?: it.windSpeed ?: 0.0) >= 60.0 }) add(Res.string.alert_strong_wind)
        if (nearTerm.any { (it.precipitation ?: 0.0) >= 5.0 }) add(Res.string.alert_heavy_precipitation)
        if ((extras?.uvIndex ?: 0.0) >= 8.0) add(Res.string.alert_extreme_uv)
        if ((extras?.visibilityKm ?: Double.MAX_VALUE) <= 1.0) add(Res.string.alert_low_visibility)
    }.distinct()

    val context = LocalContext.current
    val alertLabels = alerts.map { stringResource(it) }
    LaunchedEffect(locationName, alertLabels.joinToString("|")) {
        com.freetime.geoweather.WeatherIndicatorHistory.update(context, locationName, alertLabels)
    }
    val history = remember(locationName, alertLabels) {
        com.freetime.geoweather.WeatherIndicatorHistory.recent(context, locationName)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    if (alerts.isNotEmpty()) {
        FreetimeGlassPanel(modifier = Modifier.fillMaxWidth(), depth = FreetimeGlassDepth.ELEVATED) {
            Row(verticalAlignment = Alignment.Top) {
                Text("⚠️", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(Res.string.weather_alerts_title), style = FreetimeDesign.typography.labelMedium, color = FreetimeDesign.palette.contentMuted)
                    alerts.forEach { alert ->
                        Text("• " + stringResource(alert), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(Res.string.data_based_alert_note), style = FreetimeDesign.typography.labelSmall, color = FreetimeDesign.palette.contentMuted)
                }
            }
        }
    }
    if (history.isNotEmpty()) {
        FreetimeGlassPanel(modifier = Modifier.fillMaxWidth(), depth = FreetimeGlassDepth.SUBTLE) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.alert_history_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                history.take(5).forEach { record ->
                    val state = if (record.endedAt == null) stringResource(Res.string.alert_active) else stringResource(Res.string.alert_ended)
                    Text("• " + record.label + " · " + state, style = FreetimeDesign.typography.bodySmall)
                }
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


fun moonPhaseFor(date: java.time.LocalDate): Pair<String, String> {
    val knownNewMoon = java.time.LocalDate.of(2000, 1, 6)
    val days = java.time.temporal.ChronoUnit.DAYS.between(knownNewMoon, date).toDouble()
    val age = ((days % 29.53058867) + 29.53058867) % 29.53058867
    return when {
        age < 1.85 -> "🌑" to "New Moon"
        age < 5.54 -> "🌒" to "Waxing Crescent"
        age < 9.23 -> "🌓" to "First Quarter"
        age < 12.92 -> "🌔" to "Waxing Gibbous"
        age < 16.61 -> "🌕" to "Full Moon"
        age < 20.30 -> "🌖" to "Waning Gibbous"
        age < 23.99 -> "🌗" to "Last Quarter"
        age < 27.68 -> "🌘" to "Waning Crescent"
        else -> "🌑" to "New Moon"
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastDetailScreen(
    locationName: String,
    hourlyForecasts: List<HourlyForecast>,
    dailyForecasts: List<DailyForecast>,
    initialIndex: Int,
    appSettings: AppSettings,
    onBack: () -> Unit
) {
    val tempUnit by appSettings.tempUnit.collectAsState()
    val windUnit by appSettings.windUnit.collectAsState()
    var index by remember { mutableIntStateOf(initialIndex) }
    val isHourly = hourlyForecasts.isNotEmpty()
    val maxIndex = (if (isHourly) hourlyForecasts.lastIndex else dailyForecasts.lastIndex).coerceAtLeast(0)
    index = index.coerceIn(0, maxIndex)
    val hourly = hourlyForecasts.getOrNull(index)
    val daily = dailyForecasts.getOrNull(index)
    val forecastCode = hourly?.code ?: daily?.code ?: 0
    val forecastIsDay = hourly?.let { hour ->
        val forecastDateTime = runCatching { java.time.LocalDateTime.parse(hour.time) }.getOrNull()
        val forecastTime = forecastDateTime?.toLocalTime()
            ?: runCatching { java.time.LocalTime.parse(hour.time.takeLast(5)) }.getOrNull()
        val matchingDay = forecastDateTime?.toLocalDate()?.let { date ->
            dailyForecasts.firstOrNull { runCatching { java.time.LocalDate.parse(it.date) }.getOrNull() == date }
        } ?: dailyForecasts.firstOrNull()
        val sunrise = runCatching { java.time.LocalTime.parse(matchingDay?.sunrise?.takeLast(5) ?: "07:00") }
            .getOrDefault(java.time.LocalTime.of(7, 0))
        val sunset = runCatching { java.time.LocalTime.parse(matchingDay?.sunset?.takeLast(5) ?: "19:00") }
            .getOrDefault(java.time.LocalTime.of(19, 0))
        forecastTime?.let { !it.isBefore(sunrise) && it.isBefore(sunset) } ?: true
    } ?: true
    val title = if (hourly != null) "$locationName · ${hourly.time.takeLast(5)}"
        else "$locationName · ${daily?.date ?: ""}"

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            FreetimeGlassTopBar(title = title, onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    if (index > 0) {
                        FreetimeGlassAction(onClick = { index-- }) {
                            Text("‹", style = FreetimeDesign.typography.headlineMedium)
                        }
                    } else Spacer(Modifier.width(52.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(WeatherIconMapper.getWeatherIcon(forecastCode, forecastIsDay)), null, Modifier.size(104.dp), tint = Color.Unspecified)
                        Text(
                            hourly?.let { formatTemp(it.temp.toDouble(), tempUnit) }
                                ?: daily?.let { "${formatTemp(it.maxTemp.toDouble(), tempUnit)} / ${formatTemp(it.minTemp.toDouble(), tempUnit)}" }
                                ?: "--",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(WeatherCodes.getStringResource(forecastCode)), style = FreetimeDesign.typography.titleLarge)
                    }
                    if (index < maxIndex) {
                        FreetimeGlassAction(onClick = { index++ }) {
                            Text("›", style = FreetimeDesign.typography.headlineMedium)
                        }
                    } else Spacer(Modifier.width(52.dp))
                }
            }
            item {
                FreetimeGlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        hourly?.let { hour ->
                            DetailRow(stringResource(Res.string.time_label) to hour.time.takeLast(5), stringResource(Res.string.temperature_label) to formatTemp(hour.temp.toDouble(), tempUnit), stringResource(Res.string.feels_like_label) to (hour.feelsLike?.let { formatTemp(it, tempUnit) } ?: "--"))
                            DetailRow(stringResource(Res.string.humidity_label) to (hour.humidity?.let { "$it%" } ?: "--"), stringResource(Res.string.rain_chance_label) to "${hour.precipProbability}%", stringResource(Res.string.precipitation_label) to (hour.precipitation?.let { "$it mm" } ?: "--"))
                            DetailRow(stringResource(Res.string.rain_label) to (hour.rain?.let { "$it mm" } ?: "--"), stringResource(Res.string.snow_label) to (hour.snowfall?.let { "$it cm" } ?: "--"), stringResource(Res.string.visibility_label) to (hour.visibilityKm?.let { "${(it * 10).roundToInt() / 10.0} km" } ?: "--"))
                            DetailRow(stringResource(Res.string.pressure_label) to (hour.pressure?.let { "${it.roundToInt()} hPa" } ?: "--"), stringResource(Res.string.cloud_base_label) to (hour.cloudBaseM?.let { "${it.roundToInt()} m" } ?: "--"), stringResource(Res.string.uv_label) to (hour.uvIndex?.let { "${(it * 10).roundToInt() / 10.0}" } ?: "--"))
                            DetailRow(stringResource(Res.string.wind_label) to formatWind(hour.windSpeed, null, windUnit), stringResource(Res.string.gusts_label) to formatWind(hour.windGusts, null, windUnit))
                        }
                        daily?.let { day ->
                            DetailRow("High" to formatTemp(day.maxTemp.toDouble(), tempUnit), "Low" to formatTemp(day.minTemp.toDouble(), tempUnit), stringResource(Res.string.rain_chance_label) to "${day.precipProbMax}%")
                            DetailRow(stringResource(Res.string.feels_high_label) to (day.feelsLikeMax?.let { formatTemp(it, tempUnit) } ?: "--"), stringResource(Res.string.feels_low_label) to (day.feelsLikeMin?.let { formatTemp(it, tempUnit) } ?: "--"), stringResource(Res.string.uv_max_label) to (day.uvMax?.let { "${(it * 10).roundToInt() / 10.0}" } ?: "--"))
                            DetailRow(stringResource(Res.string.sunrise_label) to day.sunrise.takeLast(5), stringResource(Res.string.sunset_label) to day.sunset.takeLast(5), stringResource(Res.string.sunshine_label) to (day.sunshineDuration?.let { "${(it / 3600.0 * 10).roundToInt() / 10.0} h" } ?: "--"))
                            DetailRow(stringResource(Res.string.precipitation_label) to "${day.precipSum} mm", stringResource(Res.string.rain_label) to (day.rainSum?.let { "$it mm" } ?: "--"), stringResource(Res.string.snow_label) to (day.snowfallSum?.let { "$it cm" } ?: "--"))
                            DetailRow(stringResource(Res.string.wet_hours_label) to (day.precipitationHours?.let { "${(it * 10).roundToInt() / 10.0} h" } ?: "--"), stringResource(Res.string.wind_max_label) to formatWind(day.windMax, null, windUnit), stringResource(Res.string.max_gusts_label) to formatWind(day.windGustMax, null, windUnit))
                        }
                    }
                }
            }
            
        }
    }
}

@Composable
private fun DetailRow(vararg values: Pair<String, String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        values.forEach { (label, value) ->
            WeatherDetailItem(label, value, modifier = Modifier.weight(1f))
        }
    }
}


private data class MoonPhaseDetails(val icon: String, val name: String, val illumination: Int, val daysToFull: Int, val daysToNew: Int)
private fun moonPhaseDetails(date: java.time.LocalDate): MoonPhaseDetails {
    val known = java.time.LocalDate.of(2000, 1, 6)
    val cycle = 29.53058867
    val age = ((java.time.temporal.ChronoUnit.DAYS.between(known, date).toDouble() % cycle) + cycle) % cycle
    val fraction = age / cycle
    val illumination = ((1 - kotlin.math.cos(2 * kotlin.math.PI * fraction)) / 2 * 100).roundToInt()
    val (icon, name) = when {
        fraction < .0625 || fraction >= .9375 -> "🌑" to "New Moon"
        fraction < .1875 -> "🌒" to "Waxing Crescent"
        fraction < .3125 -> "🌓" to "First Quarter"
        fraction < .4375 -> "🌔" to "Waxing Gibbous"
        fraction < .5625 -> "🌕" to "Full Moon"
        fraction < .6875 -> "🌖" to "Waning Gibbous"
        fraction < .8125 -> "🌗" to "Last Quarter"
        else -> "🌘" to "Waning Crescent"
    }
    val toFull = ((cycle / 2 - age + cycle) % cycle).roundToInt()
    val toNew = ((cycle - age) % cycle).roundToInt()
    return MoonPhaseDetails(icon, name, illumination, toFull, toNew)
}
