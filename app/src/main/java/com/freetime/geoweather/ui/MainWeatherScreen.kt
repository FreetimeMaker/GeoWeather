package com.freetime.geoweather.ui
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeText
import com.freetime.design.FreetimeScaffold
import com.freetime.design.FreetimeFloatingActionButton
import com.freetime.design.FreetimeExtendedFloatingActionButton
import com.freetime.design.FreetimeSnackbarHost
import com.freetime.design.rememberFreetimeMessageHostState
import com.freetime.design.FreetimeTimePicker
import com.freetime.design.FreetimeSearchBar
import com.freetime.design.FreetimeNavigationRail
import com.freetime.design.FreetimeNavigationDestination
import com.freetime.design.FreetimeEmptyState
import com.freetime.design.FreetimeInfoCard
import com.freetime.design.FreetimeSectionHeader
import com.freetime.design.FreetimeDialog
import com.freetime.design.FreetimeCard
import com.freetime.design.FreetimeSnackbar
import com.freetime.design.rememberFreetimeCompactNavigation
import com.freetime.design.FreetimeDesign
import com.freetime.design.FreetimeGlassPullRefreshIndicator
import com.freetime.design.FreetimeGlassSkeleton
import com.freetime.design.FreetimeGlassPanel
import com.freetime.design.FreetimeGlassTitle
import com.freetime.design.freetimeWideGlass

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
import com.freetime.design.freetimeGlass
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeTextField
import com.freetime.design.FreetimeGlassAction
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.getCurrentCoordinates
import com.freetime.geoweather.getDetectedLocationName
import com.freetime.geoweather.WeatherIconMapper
import com.freetime.geoweather.LocationAlertPreferences
import kotlinx.coroutines.delay
import com.freetime.geoweather.R as Res
import kotlinx.coroutines.launch
import com.freetime.design.FreetimeProgressIndicator

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
    val navigationCompact = rememberFreetimeCompactNavigation(listState)
    var isLocating by remember { mutableStateOf(false) }
    val snackbarHostState = rememberFreetimeMessageHostState()
    val scope = rememberCoroutineScope()
    val currentLocationName = stringResource(Res.string.current_location)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val locationUnavailableMsg = stringResource(Res.string.current_location_unavailable)
    var travelStartOffset by remember { mutableIntStateOf(0) }
    var travelEndOffset by remember { mutableIntStateOf(6) }
    val adaptiveDestinations = listOf(
        FreetimeNavigationDestination(currentLocationName, Icons.Default.MyLocation),
        FreetimeNavigationDestination(stringResource(Res.string.donate_nav_desc), Icons.Default.Favorite),
        FreetimeNavigationDestination(stringResource(Res.string.settings_nav_desc), Icons.Default.Settings)
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
                    snackbarHostState.show(locationUnavailableMsg)
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

    FreetimeScaffold(
        topBar = {
            FreetimeGlassTopBar(
                title = stringResource(Res.string.app_name),
                compact = navigationCompact,
                subtitle = if (navigationCompact) locations.firstOrNull()?.currentTemp?.let { "${it.toInt()}°" } else null
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            FreetimeSnackbarHost(
                state = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)
            )
            val padding = PaddingValues(0.dp)

        if (locations.isEmpty()) {
            FreetimeEmptyState(
                title = stringResource(Res.string.app_name),
                message = stringResource(Res.string.no_locations_msg),
                modifier = Modifier.fillMaxSize().padding(padding)
            )
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
                        FreetimeSectionHeader(title = stringResource(Res.string.compare_locations))
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(orderedLocations, key = { "landscape-" + it.id }) { loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .freetimeGlass(RoundedCornerShape(24.dp), interactive = true)
                                        .clickable {
                                            viewModel.selectLocation(loc)
                                            onLocationClick(loc)
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        FreetimeText(loc.name, style = FreetimeDesign.typography.titleMedium, maxLines = 1)
                                        FreetimeText(
                                            text = loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--",
                                            style = FreetimeDesign.typography.titleLarge
                                        )
                                        FreetimeText(
                                            text = loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--",
                                            style = FreetimeDesign.typography.labelSmall
                                        )
                                    }
                                    FreetimeIconButton(
                                        icon = if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = stringResource(Res.string.favorite_location),
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.toggleDefaultLocation(loc)
                                        }
                                    )
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
                                    .freetimeGlass(RoundedCornerShape(30.dp), interactive = true)
                                    .clickable {
                                        viewModel.selectLocation(loc)
                                        onLocationClick(loc)
                                    }
                                    .padding(20.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FreetimeText(loc.name, style = FreetimeDesign.typography.titleLarge)
                                    FreetimeText(
                                        text = loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--",
                                        style = FreetimeDesign.typography.headlineLarge
                                    )
                                    FreetimeText(
                                        text = loc.currentWeatherCode?.let { code -> stringResource(com.freetime.geoweather.WeatherCodes.getStringResource(code)) }.orEmpty(),
                                        style = FreetimeDesign.typography.bodyLarge
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        FreetimeText(loc.currentWindSpeed?.let { speed -> stringResource(Res.string.wind_value, speed.toInt()) } ?: "--")
                                        FreetimeText(loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--")
                                    }
                                }
                            }
                        }
                        FreetimeGlassAction(
                            onClick = onDonateClick,
                            modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(24.dp))
                        ) {
                            FreetimeText(stringResource(Res.string.main_donation_hint))
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
                                    .freetimeGlass(RoundedCornerShape(28.dp), interactive = false)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FreetimeText(stringResource(Res.string.compare_summary_title), style = FreetimeDesign.typography.titleMedium)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            FreetimeText(stringResource(Res.string.compare_temperature_spread), style = FreetimeDesign.typography.labelSmall)
                                            FreetimeText(minTemp.toInt().toString() + "–" + maxTemp.toInt() + "°C", style = FreetimeDesign.typography.titleMedium)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            FreetimeText(stringResource(Res.string.compare_windiest), style = FreetimeDesign.typography.labelSmall)
                                            FreetimeText(windiest?.name ?: "--", style = FreetimeDesign.typography.labelLarge, maxLines = 1)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            FreetimeText(stringResource(Res.string.compare_most_humid), style = FreetimeDesign.typography.labelSmall)
                                            FreetimeText(humid?.name ?: "--", style = FreetimeDesign.typography.labelLarge, maxLines = 1)
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
                                    FreetimeCard(modifier = Modifier.width(180.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            FreetimeText(loc.name, style = FreetimeDesign.typography.titleMedium, maxLines = 1)
                                            FreetimeText(loc.currentTemp?.let { it.toInt().toString() + "°C" } ?: "--", style = FreetimeDesign.typography.headlineMedium)
                                            FreetimeText(loc.currentHumidity?.let { stringResource(Res.string.humidity_value, it) } ?: "--", style = FreetimeDesign.typography.labelMedium)
                                            FreetimeText(loc.currentWindSpeed?.let { stringResource(Res.string.wind_value, it.toInt()) } ?: "--", style = FreetimeDesign.typography.labelMedium)
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
                                FreetimeCard(modifier = Modifier.weight(1f)) {
                                    Column {
                                        FreetimeText(stringResource(Res.string.warmest_label), style = FreetimeDesign.typography.labelSmall)
                                        FreetimeText(warmest?.name ?: "--", style = FreetimeDesign.typography.labelLarge, maxLines = 1)
                                    }
                                }
                                FreetimeCard(modifier = Modifier.weight(1f)) {
                                    Column {
                                        FreetimeText(stringResource(Res.string.coolest_label), style = FreetimeDesign.typography.labelSmall)
                                        FreetimeText(coolest?.name ?: "--", style = FreetimeDesign.typography.labelLarge, maxLines = 1)
                                    }
                                }
                                FreetimeCard(modifier = Modifier.weight(1f)) {
                                    Column {
                                        FreetimeText(stringResource(Res.string.calmest_label), style = FreetimeDesign.typography.labelSmall)
                                        FreetimeText(calmest?.name ?: "--", style = FreetimeDesign.typography.labelLarge, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    item(key = "travel-mode-title") {
                        FreetimeGlassTitle(
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
                            FreetimeGlassPanel(modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FreetimeText(stringResource(Res.string.trip_dates_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    FreetimeText(
                                        stringResource(Res.string.trip_dates_range, availableDates[travelStartOffset], availableDates[travelEndOffset]),
                                        style = FreetimeDesign.typography.bodyMedium
                                    )
                                    FreetimeText(stringResource(Res.string.trip_start), style = FreetimeDesign.typography.labelMedium)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(availableDates.indices.toList(), key = { "trip-start-" + it }) { index ->
                                            FreetimeCard(modifier = Modifier.clickable {
                                                travelStartOffset = index
                                                if (travelEndOffset < index) travelEndOffset = index
                                            }) {
                                                FreetimeText(
                                                    availableDates[index].takeLast(5) + if (index == travelStartOffset) " ✓" else "",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                    FreetimeText(stringResource(Res.string.trip_end), style = FreetimeDesign.typography.labelMedium)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items((travelStartOffset..availableDates.lastIndex).toList(), key = { "trip-end-" + it }) { index ->
                                            FreetimeCard(modifier = Modifier.clickable { travelEndOffset = index }) {
                                                FreetimeText(
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
                                FreetimeCard(
                                    modifier = Modifier.width(210.dp).clickable { onLocationClick(loc) }
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        FreetimeText(loc.name, style = FreetimeDesign.typography.titleMedium, maxLines = 1)
                                        FreetimeText(
                                            if (low != null && high != null) context.getString(Res.string.trip_temperature_range, low, high) else context.getString(Res.string.forecast_unavailable),
                                            style = FreetimeDesign.typography.bodyMedium
                                        )
                                        FreetimeText(context.getString(Res.string.rain_risk_up_to, rain), style = FreetimeDesign.typography.labelMedium)
                                        FreetimeText(packHint, style = FreetimeDesign.typography.labelLarge)
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
                            FreetimeGlassPanel(modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FreetimeText(stringResource(Res.string.trip_compare_title), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    compared.forEach { (loc, low, high) ->
                                        val days = viewModel.getDailyForecasts(loc).take(7)
                                        val rain = days.maxOfOrNull { it.precipProbMax } ?: 0
                                        val range = if (low != null && high != null) low.toString() + "–" + high + "°C" else "--"
                                        FreetimeText(
                                            stringResource(Res.string.trip_compare_row, loc.name, range, rain),
                                            style = FreetimeDesign.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "location-overview-title") {
                        FreetimeGlassTitle(
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
                                            FreetimeText(loc.name, style = FreetimeDesign.typography.labelLarge, maxLines = 1, modifier = Modifier.weight(1f))
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
                                            FreetimeText(loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--", style = FreetimeDesign.typography.titleLarge)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FreetimeText(loc.currentWindSpeed?.let { speed -> stringResource(Res.string.wind_value, speed.toInt()) } ?: "--", style = FreetimeDesign.typography.labelSmall)
                                            FreetimeText(loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--", style = FreetimeDesign.typography.labelSmall)
                                        }
                                        if (loc.lastUpdated > 0L) FreetimeText(stringResource(Res.string.updated_age_short, ageMinutes), style = FreetimeDesign.typography.labelSmall, color = FreetimeDesign.palette.contentMuted)
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "saved-locations-title") {
                    FreetimeText(
                        text = stringResource(Res.string.compare_locations),
                        style = FreetimeDesign.typography.titleMedium,
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
                            FreetimeText(loc.name, style = FreetimeDesign.typography.titleMedium)
                            FreetimeText(loc.currentTemp?.let { it.toInt().toString() + "°C" } ?: "--", style = FreetimeDesign.typography.bodyMedium)
                        }
                        FreetimeIconButton(
                            icon = if (loc.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = stringResource(Res.string.notification_time_title),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                notificationLocation = loc
                            }
                        )
                        FreetimeIconButton(
                            icon = if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(Res.string.favorite_location),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.toggleDefaultLocation(loc)
                            }
                        )
                        FreetimeIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.DelLoc),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                locationToDelete = loc
                            }
                        )
                    }
                }
                item(key = "donate") {
                    FreetimeGlassAction(
                        onClick = onDonateClick,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        FreetimeText(text = stringResource(Res.string.main_donation_hint), style = FreetimeDesign.typography.labelMedium)
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
                FreetimeNavigationRail(
                    destinations = adaptiveDestinations,
                    selectedIndex = 0,
                    onDestinationSelected = onNavigationSelected,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
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
                FreetimeExtendedFloatingActionButton(
                    text = stringResource(Res.string.SearchBTNTXT),
                    icon = Icons.Default.Add,
                    onClick = addLocationAction,
                    modifier = addLocationFabModifier
                )
            } else {
                FreetimeFloatingActionButton(
                    icon = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.SearchBTNTXT),
                    onClick = addLocationAction,
                    modifier = addLocationFabModifier
                )
            }
        }
        }
    }

    if (showAddLocationDialog) {
        FreetimeDialog(
            onDismissRequest = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() },
            title = stringResource(Res.string.search_title),
            content = {
            FreetimeSearchBar(
                value = addLocationQuery,
                onValueChange = { addLocationQuery = it; viewModel.searchCity(it.trim()) },
                suggestions = searchResults.map { it.name },
                onSuggestionSelected = { selected ->
                    searchResults.firstOrNull { it.name == selected }?.let { city ->
                        viewModel.addLocation(city)
                        showAddLocationDialog = false
                        addLocationQuery = ""
                        viewModel.clearSearch()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.search_placeholder)
            )
            if (isSearching) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { FreetimeProgressIndicator() }
            } else if (addLocationQuery.isNotBlank()) {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchResults, key = { "add-${it.latitude},${it.longitude}" }) { city ->
                        Column(
                            modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(22.dp), interactive = true)
                                .clickable { viewModel.addLocation(city); showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            FreetimeText(city.name, style = FreetimeDesign.typography.titleMedium)
                            FreetimeText("${city.latitude}, ${city.longitude}", style = FreetimeDesign.typography.bodySmall, color = FreetimeDesign.palette.contentMuted)
                        }
                    }
                }
            }
            },
            actions = {
                FreetimeGlassAction(onClick = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() }) {
                    FreetimeText(stringResource(Res.string.CancelTXT))
                }
            }
        )
    }

    locationToDelete?.let { location ->
        FreetimeDialog(
            title = stringResource(Res.string.DelLoc),
            text = stringResource(Res.string.DelLocConAsk, location.name),
            confirmText = stringResource(Res.string.DelTXT),
            onConfirm = {
                viewModel.deleteLocation(location)
                locationToDelete = null
            },
            dismissText = stringResource(Res.string.CancelTXT),
            onDismissRequest = { locationToDelete = null }
        )
    }

    notificationLocation?.let { location ->
        val parts = location.notificationTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        var selectedTime by remember(location.id) {
            mutableStateOf(java.time.LocalTime.of(initialHour, initialMinute))
        }
        val globalTempThreshold by viewModel.appSettings.tempThreshold.collectAsState()
        val globalWindThreshold by viewModel.appSettings.windThreshold.collectAsState()
        var locationTempThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.tempThreshold(context, location.id, globalTempThreshold)) }
        var locationWindThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.windThreshold(context, location.id, globalWindThreshold)) }
        var locationRainThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.rainProbability(context, location.id)) }
        var locationGustThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.windGustThreshold(context, location.id)) }
        var locationUvThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.uvThreshold(context, location.id)) }
        var locationFrostThreshold by remember(location.id) { mutableIntStateOf(LocationAlertPreferences.frostThreshold(context, location.id)) }

        FreetimeDialog(
            onDismissRequest = { notificationLocation = null },
            title = stringResource(Res.string.notification_time_title),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FreetimeTimePicker(
                        value = selectedTime,
                        onValueChange = { selectedTime = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FreetimeText(stringResource(Res.string.location_alert_thresholds), style = FreetimeDesign.typography.titleMedium, fontWeight = FontWeight.Bold)
                    AlertThresholdRow(stringResource(Res.string.location_temp_threshold, locationTempThreshold), { locationTempThreshold = (locationTempThreshold - 1).coerceAtLeast(1) }, { locationTempThreshold = (locationTempThreshold + 1).coerceAtMost(20) })
                    AlertThresholdRow(stringResource(Res.string.location_wind_threshold, locationWindThreshold), { locationWindThreshold = (locationWindThreshold - 1).coerceAtLeast(1) }, { locationWindThreshold = (locationWindThreshold + 1).coerceAtMost(100) })
                    AlertThresholdRow(stringResource(Res.string.location_rain_threshold, locationRainThreshold), { locationRainThreshold = (locationRainThreshold - 5).coerceAtLeast(10) }, { locationRainThreshold = (locationRainThreshold + 5).coerceAtMost(100) })
                    AlertThresholdRow(stringResource(Res.string.location_gust_threshold, locationGustThreshold), { locationGustThreshold = (locationGustThreshold - 5).coerceAtLeast(10) }, { locationGustThreshold = (locationGustThreshold + 5).coerceAtMost(150) })
                    AlertThresholdRow(stringResource(Res.string.location_uv_threshold, locationUvThreshold), { locationUvThreshold = (locationUvThreshold - 1).coerceAtLeast(1) }, { locationUvThreshold = (locationUvThreshold + 1).coerceAtMost(15) })
                    AlertThresholdRow(stringResource(Res.string.location_frost_threshold, locationFrostThreshold), { locationFrostThreshold = (locationFrostThreshold - 1).coerceAtLeast(-20) }, { locationFrostThreshold = (locationFrostThreshold + 1).coerceAtMost(10) })
                }
            },
            actions = {
                if (location.notificationsEnabled) {
                    FreetimeGlassAction(onClick = {
                        viewModel.setLocationNotifications(location, false, location.notificationTime)
                        notificationLocation = null
                    }) { FreetimeText(stringResource(Res.string.notification_disable)) }
                    Spacer(Modifier.width(8.dp))
                }
                FreetimeGlassAction(onClick = { notificationLocation = null }) {
                    FreetimeText(stringResource(Res.string.CancelTXT))
                }
                Spacer(Modifier.width(8.dp))
                FreetimeGlassAction(onClick = {
                    val time = "%02d:%02d".format(selectedTime.hour, selectedTime.minute)
                    LocationAlertPreferences.setTempThreshold(context, location.id, locationTempThreshold)
                    LocationAlertPreferences.setWindThreshold(context, location.id, locationWindThreshold)
                    LocationAlertPreferences.setRainProbability(context, location.id, locationRainThreshold)
                    LocationAlertPreferences.setWindGustThreshold(context, location.id, locationGustThreshold)
                    LocationAlertPreferences.setUvThreshold(context, location.id, locationUvThreshold)
                    LocationAlertPreferences.setFrostThreshold(context, location.id, locationFrostThreshold)
                    viewModel.setLocationNotifications(location, true, time)
                    notificationLocation = null
                }) { FreetimeText(stringResource(Res.string.confirm)) }
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
        FreetimeText(label, modifier = Modifier.weight(1f), style = FreetimeDesign.typography.bodyMedium)
        FreetimeGlassAction(onClick = onDecrease) { FreetimeText("−") }
        Spacer(Modifier.width(6.dp))
        FreetimeGlassAction(onClick = onIncrease) { FreetimeText("+") }
    }
}

@Composable
private fun GeoWeatherBottomNavigation(
    destinations: List<FreetimeNavigationDestination>,
    selectedIndex: Int,
    onDestinationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .freetimeGlass(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
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
                        if (selected) FreetimeDesign.palette.primary else FreetimeDesign.palette.contentMuted
                    )
                )
                FreetimeText(
                    text = destination.label,
                    style = FreetimeDesign.typography.labelMedium,
                    color = if (selected) FreetimeDesign.palette.primary else FreetimeDesign.palette.contentMuted
                )
            }
        }
    }
}
