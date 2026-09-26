package com.freetime.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.freetime.geoweather.data.LocationEntity
import com.freetime.design.liquidGlass
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.getCurrentCoordinates
import com.freetime.geoweather.getDetectedLocationName
import com.freetime.geoweather.WeatherIconMapper
import com.freetime.geoweather.LocationAlertPreferences
import kotlinx.coroutines.delay
import com.freetime.geoweather.R as Res
import kotlinx.coroutines.launch

private data class WeatherNavigationDestination(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWeatherScreen(
    viewModel: WeatherViewModel,
    onLocationClick: (LocationEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onDonateClick: () -> Unit,
    onCurrentLocationClick: (String, Double, Double) -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }
    var notificationLocation by remember { mutableStateOf<LocationEntity?>(null) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var addLocationQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val orderedLocations = locations
    val listState = rememberLazyListState()
    val navigationCompact by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var isLocating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentLocationName = stringResource(Res.string.current_location)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val locationUnavailableMsg = stringResource(Res.string.current_location_unavailable)
    var travelStartOffset by remember { mutableIntStateOf(0) }
    var travelEndOffset by remember { mutableIntStateOf(6) }
    val adaptiveDestinations = listOf(
        WeatherNavigationDestination(currentLocationName, Icons.Default.MyLocation),
        WeatherNavigationDestination(stringResource(Res.string.donate_nav_desc), Icons.Default.Favorite),
        WeatherNavigationDestination(stringResource(Res.string.settings_nav_desc), Icons.Default.Settings)
    )

    fun openCurrentLocation() {
        scope.launch {
            isLocating = true
            try {
                val coords = getCurrentCoordinates()
                if (coords != null) {
                    val detectedLocationName = getDetectedLocationName(coords.first, coords.second)
                        ?.takeIf { it.isNotBlank() }
                        ?: currentLocationName
                    onCurrentLocationClick(detectedLocationName, coords.first, coords.second)
                } else {
                    snackbarHostState.showSnackbar(locationUnavailableMsg)
                }
            } finally {
                isLocating = false
            }
        }
    }

    LaunchedEffect(locations.map { it.id }) {
        if (locations.isNotEmpty()) {
            // Render cached Room data immediately. Refresh shortly after first paint
            // so app startup/navigation never waits on network requests.
            delay(1_500)
            viewModel.refreshAllLocations()
            while (true) {
                delay(10 * 60 * 1000L)
                viewModel.refreshAllLocations()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.app_name))
                        if (navigationCompact) locations.firstOrNull()?.currentTemp?.let { Text("${it.toInt()}°", style = MaterialTheme.typography.labelMedium) }
                    }
                },
                modifier = Modifier.liquidGlass(interactive = false)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Box(Modifier.fillMaxSize().padding(scaffoldPadding)) {
            val padding = PaddingValues(0.dp)

        if (locations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineSmall); Text(stringResource(Res.string.no_locations_msg), textAlign = TextAlign.Center) } }
        } else {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(0.42f).fillMaxHeight().padding(start = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = stringResource(Res.string.compare_locations))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(orderedLocations, key = { "landscape-" + it.id }) { loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liquidGlass(RoundedCornerShape(24.dp), interactive = true)
                                        .clickable {
                                            viewModel.selectLocation(loc)
                                            onLocationClick(loc)
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(loc.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                        Text(
                                            text = loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    IconButton(onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.toggleDefaultLocation(loc)
                        }) { Icon(if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = stringResource(Res.string.favorite_location)) }
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(0.58f).fillMaxHeight().padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val featured = orderedLocations.firstOrNull { it.isDefault } ?: orderedLocations.firstOrNull()
                        featured?.let { loc ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(RoundedCornerShape(30.dp), interactive = true)
                                    .clickable {
                                        viewModel.selectLocation(loc)
                                        onLocationClick(loc)
                                    }
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(loc.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--",
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                    Text(
                                        text = loc.currentWeatherCode?.let { code -> stringResource(com.freetime.geoweather.WeatherCodes.getStringResource(code)) }.orEmpty(),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(loc.currentWindSpeed?.let { speed -> stringResource(Res.string.wind_value, speed.toInt()) } ?: "--")
                                        Text(loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--")
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = onDonateClick,
                            modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(24.dp))
                        ) {
                            Text(stringResource(Res.string.main_donation_hint))
                        }
                    }
                }
            } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (locations.size > 1) {
                    val comparable = orderedLocations.filter { it.currentTemp != null }
                    if (comparable.size >= 2) {
                        val minTemp = comparable.minOf { it.currentTemp!! }
                        val maxTemp = comparable.maxOf { it.currentTemp!! }
                        val windiest = comparable.maxByOrNull { it.currentWindSpeed ?: 0.0 }
                        val humid = comparable.maxByOrNull { it.currentHumidity ?: 0 }
                        val warmest = comparable.maxByOrNull { it.currentTemp ?: Double.NEGATIVE_INFINITY }
                        val coolest = comparable.minByOrNull { it.currentTemp ?: Double.POSITIVE_INFINITY }
                        val calmest = comparable.minByOrNull { it.currentWindSpeed ?: Double.POSITIVE_INFINITY }
                        item(key = "comparison-summary") {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .fillMaxWidth()
                                    .liquidGlass(RoundedCornerShape(28.dp), interactive = false)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(Res.string.compare_summary_title), style = MaterialTheme.typography.titleMedium)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(Res.string.compare_temperature_spread), style = MaterialTheme.typography.labelSmall)
                                            Text(minTemp.toInt().toString() + "–" + maxTemp.toInt() + "°C", style = MaterialTheme.typography.titleMedium)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(Res.string.compare_windiest), style = MaterialTheme.typography.labelSmall)
                                            Text(windiest?.name ?: "--", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(Res.string.compare_most_humid), style = MaterialTheme.typography.labelSmall)
                                            Text(humid?.name ?: "--", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "comparison-details") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(comparable, key = { "compare-" + it.id }) { loc ->
                                    Card(modifier = Modifier.width(180.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(loc.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                            Text(loc.currentTemp?.let { it.toInt().toString() + "°C" } ?: "--", style = MaterialTheme.typography.headlineMedium)
                                            Text(loc.currentHumidity?.let { stringResource(Res.string.humidity_value, it) } ?: "--", style = MaterialTheme.typography.labelMedium)
                                            Text(loc.currentWindSpeed?.let { stringResource(Res.string.wind_value, it.toInt()) } ?: "--", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "comparison-highlights") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Text(stringResource(Res.string.warmest_label), style = MaterialTheme.typography.labelSmall)
                                        Text(warmest?.name ?: "--", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Text(stringResource(Res.string.coolest_label), style = MaterialTheme.typography.labelSmall)
                                        Text(coolest?.name ?: "--", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Text(stringResource(Res.string.calmest_label), style = MaterialTheme.typography.labelSmall)
                                        Text(calmest?.name ?: "--", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    item(key = "travel-mode-title") {
                        Text(
                            text = stringResource(Res.string.travel_planner_title),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    item(key = "travel-dates") {
                        val availableDates = orderedLocations
                            .map { viewModel.getDailyForecasts(it).map { day -> day.date } }
                            .filter { it.isNotEmpty() }
                            .maxByOrNull { it.size }
                            .orEmpty()
                        if (availableDates.isNotEmpty()) {
                            travelStartOffset = travelStartOffset.coerceIn(0, availableDates.lastIndex)
                            travelEndOffset = travelEndOffset.coerceIn(travelStartOffset, availableDates.lastIndex)
                            Card(modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(Res.string.trip_dates_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        stringResource(Res.string.trip_dates_range, availableDates[travelStartOffset], availableDates[travelEndOffset]),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(stringResource(Res.string.trip_start), style = MaterialTheme.typography.labelMedium)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(availableDates.indices.toList(), key = { "trip-start-" + it }) { index ->
                                            Card(modifier = Modifier.clickable {
                                                travelStartOffset = index
                                                if (travelEndOffset < index) travelEndOffset = index
                                            }) {
                                                Text(
                                                    availableDates[index].takeLast(5) + if (index == travelStartOffset) " ✓" else "",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(stringResource(Res.string.trip_end), style = MaterialTheme.typography.labelMedium)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items((travelStartOffset..availableDates.lastIndex).toList(), key = { "trip-end-" + it }) { index ->
                                            Card(modifier = Modifier.clickable { travelEndOffset = index }) {
                                                Text(
                                                    availableDates[index].takeLast(5) + if (index == travelEndOffset) " ✓" else "",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "travel-mode") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(orderedLocations, key = { "travel-" + it.id }) { loc ->
                                val days = remember(loc.weatherData) { viewModel.getDailyForecasts(loc).drop(travelStartOffset).take((travelEndOffset - travelStartOffset + 1).coerceAtLeast(1)) }
                                val low = days.minOfOrNull { it.minTemp }
                                val high = days.maxOfOrNull { it.maxTemp }
                                val rain = days.maxOfOrNull { it.precipProbMax } ?: 0
                                val packItems = buildList {
                                    if ((low ?: 20) <= 8) add(context.getString(Res.string.pack_warm_layers))
                                    if (rain >= 45) add(context.getString(Res.string.pack_rain_jacket))
                                    if ((high ?: 0) >= 24) add(context.getString(Res.string.pack_sun_protection))
                                    if ((days.maxOfOrNull { it.uvMax ?: 0.0 } ?: 0.0) >= 6.0) add(context.getString(Res.string.pack_spf))
                                    if ((days.maxOfOrNull { it.windGustMax ?: it.windMax } ?: 0.0) >= 45.0) add(context.getString(Res.string.pack_windproof))
                                    if ((days.maxOfOrNull { it.snowfallSum ?: 0.0 } ?: 0.0) > 0.0) add(context.getString(Res.string.pack_winter_shoes))
                                    if (isEmpty()) add(context.getString(Res.string.pack_everyday))
                                }
                                val packHint = context.getString(Res.string.pack_prefix, packItems.joinToString(", "))
                                Card(
                                    modifier = Modifier.width(210.dp).clickable { onLocationClick(loc) }
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(loc.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                        Text(
                                            if (low != null && high != null) context.getString(Res.string.trip_temperature_range, low, high) else context.getString(Res.string.forecast_unavailable),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(context.getString(Res.string.rain_risk_up_to, rain), style = MaterialTheme.typography.labelMedium)
                                        Text(packHint, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                    if (orderedLocations.size >= 2) {
                        item(key = "travel-compare") {
                            val compared = orderedLocations.take(4).map { loc ->
                                val days = viewModel.getDailyForecasts(loc).take(7)
                                Triple(loc, days.minOfOrNull { it.minTemp }, days.maxOfOrNull { it.maxTemp })
                            }
                            Card(modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(Res.string.trip_compare_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    compared.forEach { (loc, low, high) ->
                                        val days = viewModel.getDailyForecasts(loc).take(7)
                                        val rain = days.maxOfOrNull { it.precipProbMax } ?: 0
                                        val range = if (low != null && high != null) low.toString() + "–" + high + "°C" else "--"
                                        Text(
                                            stringResource(Res.string.trip_compare_row, loc.name, range, rain),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "location-overview-title") {
                        Text(
                            text = stringResource(Res.string.location_overview_title),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    item(key = "location-overview") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(orderedLocations, key = { "overview-" + it.id }) { loc ->
                                val ageMinutes = ((System.currentTimeMillis() - loc.lastUpdated).coerceAtLeast(0L) / 60_000L).toInt()
                                Box(
                                    modifier = Modifier
                                        .width(184.dp)
                                        .clickable { viewModel.selectLocation(loc); onLocationClick(loc) }
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(loc.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.weight(1f))
                                            if (loc.isDefault) Image(imageVector = Icons.Default.Favorite, contentDescription = stringResource(Res.string.favorite_location), modifier = Modifier.size(16.dp))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            loc.currentWeatherCode?.let { code ->
                                                Image(
                                                    painter = painterResource(WeatherIconMapper.getWeatherIcon(code)),
                                                    contentDescription = stringResource(com.freetime.geoweather.WeatherCodes.getStringResource(code)),
                                                    modifier = Modifier.size(40.dp)
                                                )
                                            }
                                            Text(loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--", style = MaterialTheme.typography.titleLarge)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(loc.currentWindSpeed?.let { speed -> stringResource(Res.string.wind_value, speed.toInt()) } ?: "--", style = MaterialTheme.typography.labelSmall)
                                            Text(loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--", style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (loc.lastUpdated > 0L) Text(stringResource(Res.string.updated_age_short, ageMinutes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "saved-locations-title") {
                    Text(
                        text = stringResource(Res.string.compare_locations),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
                items(orderedLocations, key = { it.id }) { loc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clickable {
                                viewModel.selectLocation(loc)
                                onLocationClick(loc)
                            }
                            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        loc.currentWeatherCode?.let { code ->
                            Image(
                                painter = painterResource(WeatherIconMapper.getWeatherIcon(code)),
                                contentDescription = stringResource(com.freetime.geoweather.WeatherCodes.getStringResource(code)),
                                modifier = Modifier.size(46.dp).padding(end = 8.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loc.name, style = MaterialTheme.typography.titleMedium)
                            Text(loc.currentTemp?.let { it.toInt().toString() + "°C" } ?: "--", style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                notificationLocation = loc
                        }) { Icon(if (loc.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff, contentDescription = stringResource(Res.string.notification_time_title)) }
                        IconButton(onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleDefaultLocation(loc)
                        }) { Icon(if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = stringResource(Res.string.favorite_location)) }
                        IconButton(onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                locationToDelete = loc
                        }) { Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.DelLoc)) }
                    }
                }
                item(key = "donate") {
                    Button(
                        onClick = onDonateClick,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = stringResource(Res.string.main_donation_hint), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            }

            val onNavigationSelected: (Int) -> Unit = { index ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                when (index) {
                    0 -> if (!isLocating) openCurrentLocation()
                    1 -> onDonateClick()
                    2 -> onSettingsClick()
                }
            }
            if (configuration.screenWidthDp >= 720) {
                NavigationRail(modifier = Modifier.align(Alignment.CenterStart).liquidGlass(interactive = false)) {
                    adaptiveDestinations.forEachIndexed { index, destination ->
                        NavigationRailItem(
                            selected = index == 0,
                            onClick = { onNavigationSelected(index) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            } else {
                GeoWeatherBottomNavigation(
                    destinations = adaptiveDestinations,
                    selectedIndex = 0,
                    onDestinationSelected = onNavigationSelected,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }

            val addLocationAction = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showAddLocationDialog = true
            }
            val addLocationFabModifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 92.dp)

            if (isLandscape) {
                ExtendedFloatingActionButton(text = { Text(stringResource(Res.string.SearchBTNTXT)) }, icon = { Icon(Icons.Default.Add, contentDescription = null) }, onClick = addLocationAction, modifier = addLocationFabModifier)
            } else {
                FloatingActionButton(onClick = addLocationAction, modifier = addLocationFabModifier) { Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.SearchBTNTXT)) }
            }
        }
        }
    }

    if (showAddLocationDialog) {
        AlertDialog(
            onDismissRequest = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() },
            title = { Text(stringResource(Res.string.search_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addLocationQuery,
                        onValueChange = { addLocationQuery = it; viewModel.searchCity(it.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                        singleLine = true
                    )
                    if (isSearching) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else if (addLocationQuery.isNotBlank()) {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(searchResults, key = { "add-${it.latitude},${it.longitude}" }) { city ->
                                Column(
                                    modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp), interactive = true)
                                        .clickable { viewModel.addLocation(city); showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(city.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${city.latitude}, ${city.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
            }
        )
    }

    locationToDelete?.let { location ->
        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            title = { Text(stringResource(Res.string.DelLoc)) },
            text = { Text(stringResource(Res.string.DelLocConAsk, location.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteLocation(location); locationToDelete = null }) {
                    Text(stringResource(Res.string.DelTXT))
                }
            },
            dismissButton = {
                TextButton(onClick = { locationToDelete = null }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
            }
        )
    }

    notificationLocation?.let { location ->
        val parts = location.notificationTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        var selectedTime by remember(location.id) {
            mutableStateOf(java.time.LocalTime.of(initialHour, initialMinute))
        }
        val globalTempThreshold by DependencyManager.getAppSettings().tempThreshold.collectAsState()
        val globalWindThreshold by DependencyManager.getAppSettings().windThreshold.collectAsState()
        var locationTempThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.tempThreshold(context, location.id, globalTempThreshold)) }
        var locationWindThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.windThreshold(context, location.id, globalWindThreshold)) }
        var locationRainThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.rainProbability(context, location.id)) }
        var locationGustThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.windGustThreshold(context, location.id)) }
        var locationUvThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.uvThreshold(context, location.id)) }
        var locationFrostThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.frostThreshold(context, location.id)) }

        AlertDialog(
            onDismissRequest = { notificationLocation = null },
            title = { Text(stringResource(Res.string.notification_time_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val timePickerState = rememberTimePickerState(initialHour = selectedTime.hour, initialMinute = selectedTime.minute, is24Hour = true)
                    TimeInput(state = timePickerState, modifier = Modifier.fillMaxWidth())
                    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                        selectedTime = java.time.LocalTime.of(timePickerState.hour, timePickerState.minute)
                    }
                    Text(stringResource(Res.string.location_alert_thresholds), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AlertThresholdRow(stringResource(Res.string.location_temp_threshold, locationTempThreshold), { locationTempThreshold = (locationTempThreshold - 1).coerceAtLeast(1) }, { locationTempThreshold = (locationTempThreshold + 1).coerceAtMost(20) })
                    AlertThresholdRow(stringResource(Res.string.location_wind_threshold, locationWindThreshold), { locationWindThreshold = (locationWindThreshold - 1).coerceAtLeast(1) }, { locationWindThreshold = (locationWindThreshold + 1).coerceAtMost(100) })
                    AlertThresholdRow(stringResource(Res.string.location_rain_threshold, locationRainThreshold), { locationRainThreshold = (locationRainThreshold - 5).coerceAtLeast(10) }, { locationRainThreshold = (locationRainThreshold + 5).coerceAtMost(100) })
                    AlertThresholdRow(stringResource(Res.string.location_gust_threshold, locationGustThreshold), { locationGustThreshold = (locationGustThreshold - 5).coerceAtLeast(10) }, { locationGustThreshold = (locationGustThreshold + 5).coerceAtMost(150) })
                    AlertThresholdRow(stringResource(Res.string.location_uv_threshold, locationUvThreshold), { locationUvThreshold = (locationUvThreshold - 1).coerceAtLeast(1) }, { locationUvThreshold = (locationUvThreshold + 1).coerceAtMost(15) })
                    AlertThresholdRow(stringResource(Res.string.location_frost_threshold, locationFrostThreshold), { locationFrostThreshold = (locationFrostThreshold - 1).coerceAtLeast(-20) }, { locationFrostThreshold = (locationFrostThreshold + 1).coerceAtMost(10) })
                }
            },
            confirmButton = {
                if (location.notificationsEnabled) {
                    Button(onClick = {
                        viewModel.setLocationNotifications(location, false, location.notificationTime)
                        notificationLocation = null
                    }) { Text(stringResource(Res.string.notification_disable)) }
                    Spacer(Modifier.width(8.dp))
                }
                Button(onClick = { notificationLocation = null }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val time = "%02d:%02d".format(selectedTime.hour, selectedTime.minute)
                    LocationAlertPreferences.setTempThreshold(context, location.id, locationTempThreshold)
                    LocationAlertPreferences.setWindThreshold(context, location.id, locationWindThreshold)
                    LocationAlertPreferences.setRainProbability(context, location.id, locationRainThreshold)
                    LocationAlertPreferences.setWindGustThreshold(context, location.id, locationGustThreshold)
                    LocationAlertPreferences.setUvThreshold(context, location.id, locationUvThreshold)
                    LocationAlertPreferences.setFrostThreshold(context, location.id, locationFrostThreshold)
                    viewModel.setLocationNotifications(location, true, time)
                    notificationLocation = null
                }) { Text(stringResource(Res.string.confirm)) }
            }
        )
    }
}



@Composable
private fun AlertThresholdRow(label: String, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onDecrease) { Text("−") }
        Spacer(Modifier.width(6.dp))
        Button(onClick = onIncrease) { Text("+") }
    }
}

@Composable
private fun GeoWeatherBottomNavigation(
    destinations: List<WeatherNavigationDestination>,
    selectedIndex: Int,
    onDestinationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        destinations.forEachIndexed { index, destination ->
            val selected = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDestinationSelected(index) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Image(
                    imageVector = destination.icon,
                    contentDescription = destination.label,
                    modifier = Modifier.size(24.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
