package com.freetime.geoweather.ui
import com.freetime.design.FreetimeIconButton
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
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeGlassAction
import com.freetime.design.FreetimeTextField
import com.freetime.design.freetimeGlassCapsule
import com.freetime.design.FreetimeGlassNavigationBar
import com.freetime.design.FreetimeGlassTitle
import com.freetime.design.freetimeWideGlass
import com.freetime.design.freetimeGlass

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.freetime.design.freetimeGlass
import com.freetime.design.freetimeGlassCapsule
import com.freetime.design.FreetimeGlassTopBar
import com.freetime.design.FreetimeTextField
import com.freetime.design.FreetimeGlassAction
import com.freetime.geoweather.ui.glass.GeoWeatherGlassIconAction
import com.freetime.geoweather.ui.glass.GeoWeatherGlassDialog
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.getCurrentCoordinates
import com.freetime.geoweather.getDetectedLocationName
import com.freetime.geoweather.WeatherIconMapper
import kotlinx.coroutines.delay
import com.freetime.geoweather.R as Res
import kotlinx.coroutines.launch
import com.freetime.design.FreetimeProgressIndicator

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
    val navigationCompact = rememberFreetimeCompactNavigation(listState)
    var isLocating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentLocationName = stringResource(Res.string.current_location)
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val locationUnavailableMsg = stringResource(Res.string.current_location_unavailable)

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
        containerColor = Color.Transparent,
        snackbarHost = { snackbarHostState.currentSnackbarData?.let { data -> FreetimeSnackbar(message = data.visuals.message) } },
        topBar = {
            FreetimeGlassTopBar(
                title = stringResource(Res.string.app_name),
                compact = navigationCompact,
                subtitle = if (navigationCompact) locations.firstOrNull()?.currentTemp?.let { "${it.toInt()}°" } else null
            )
        } 
    ) { padding ->
        Box(Modifier.fillMaxSize()) {

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
                                        Text(loc.name, style = FreetimeDesign.typography.titleMedium, maxLines = 1)
                                        Text(
                                            text = loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--",
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        Text(
                                            text = loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--",
                                            style = FreetimeDesign.typography.labelSmall
                                        )
                                    }
                                    GeoWeatherGlassIconAction(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.toggleDefaultLocation(loc)
                                        }
                                    ) {
                                        Icon(
                                            if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = stringResource(Res.string.favorite_location)
                                        )
                                    }
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
                                    Text(loc.name, style = MaterialTheme.typography.headlineSmall)
                                    Text(
                                        text = loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--",
                                        style = MaterialTheme.typography.displaySmall
                                    )
                                    Text(
                                        text = loc.currentWeatherCode?.let { code -> stringResource(com.freetime.geoweather.WeatherCodes.getStringResource(code)) }.orEmpty(),
                                        style = FreetimeDesign.typography.bodyLarge
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(loc.currentWindSpeed?.let { speed -> stringResource(Res.string.wind_value, speed.toInt()) } ?: "--")
                                        Text(loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--")
                                    }
                                }
                            }
                        }
                        FreetimeGlassAction(
                            onClick = onDonateClick,
                            modifier = Modifier.fillMaxWidth().freetimeGlass(RoundedCornerShape(24.dp))
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
                        item(key = "comparison-summary") {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .fillMaxWidth()
                                    .freetimeGlass(RoundedCornerShape(28.dp), interactive = false)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(Res.string.compare_summary_title), style = FreetimeDesign.typography.titleMedium)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(Res.string.compare_temperature_spread), style = FreetimeDesign.typography.labelSmall)
                                            Text(minTemp.toInt().toString() + "–" + maxTemp.toInt() + "°C", style = FreetimeDesign.typography.titleMedium)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(Res.string.compare_windiest), style = FreetimeDesign.typography.labelSmall)
                                            Text(windiest?.name ?: "--", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(Res.string.compare_most_humid), style = FreetimeDesign.typography.labelSmall)
                                            Text(humid?.name ?: "--", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        }
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
                                        .freetimeGlass(RoundedCornerShape(24.dp), interactive = true)
                                        .clickable { viewModel.selectLocation(loc); onLocationClick(loc) }
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(loc.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.weight(1f))
                                            if (loc.isDefault) Icon(Icons.Default.Favorite, contentDescription = stringResource(Res.string.favorite_location), modifier = Modifier.size(16.dp))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            loc.currentWeatherCode?.let { code ->
                                                Image(
                                                    painter = painterResource(WeatherIconMapper.getWeatherIcon(code)),
                                                    contentDescription = stringResource(com.freetime.geoweather.WeatherCodes.getStringResource(code)),
                                                    modifier = Modifier.size(40.dp)
                                                )
                                            }
                                            Text(loc.currentTemp?.let { temp -> temp.toInt().toString() + "°C" } ?: "--", style = MaterialTheme.typography.headlineSmall)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(loc.currentWindSpeed?.let { speed -> stringResource(Res.string.wind_value, speed.toInt()) } ?: "--", style = FreetimeDesign.typography.labelSmall)
                                            Text(loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--", style = FreetimeDesign.typography.labelSmall)
                                        }
                                        if (loc.lastUpdated > 0L) Text(stringResource(Res.string.updated_age_short, ageMinutes), style = FreetimeDesign.typography.labelSmall, color = FreetimeDesign.palette.contentMuted)
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "saved-locations-title") {
                    Text(
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
                            .freetimeGlass(RoundedCornerShape(26.dp), interactive = true)
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
                            Text(loc.name, style = FreetimeDesign.typography.titleMedium)
                            Text(loc.currentTemp?.let { it.toInt().toString() + "°C" } ?: "--", style = FreetimeDesign.typography.bodyMedium)
                        }
                        FreetimeIconButton(icon = if (loc.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff, contentDescription = null, onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); notificationLocation = loc })
                        FreetimeIconButton(icon = if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = stringResource(Res.string.favorite_location), onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.toggleDefaultLocation(loc) })
                        FreetimeIconButton(icon = Icons.Default.Delete, contentDescription = stringResource(Res.string.DelLoc), onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); locationToDelete = loc })
                    }
                }
                item(key = "donate") {
                    FreetimeGlassAction(
                        onClick = onDonateClick,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = stringResource(Res.string.main_donation_hint), style = FreetimeDesign.typography.labelMedium)
                    }
                }
            }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FreetimeGlassNavigationBar {
                    GeoWeatherGlassIconAction(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            openCurrentLocation()
                        },
                        modifier = Modifier.size(if (navigationCompact) 48.dp else 52.dp)
                    ) {
                        if (isLocating) {
                            FreetimeProgressIndicator(Modifier.size(19.dp))
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = currentLocationName)
                        }
                    }

                    AnimatedVisibility(
                        visible = !navigationCompact,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            GeoWeatherGlassIconAction(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDonateClick()
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = stringResource(Res.string.donate_nav_desc))
                            }
                            GeoWeatherGlassIconAction(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSettingsClick()
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.settings_nav_desc))
                            }
                        }
                    }
                }

                // Like SimpMusic's Search FAB: the primary creation action is its own
                // circular glass surface instead of being buried inside the capsule.
                GeoWeatherGlassIconAction(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAddLocationDialog = true
                    },
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.SearchBTNTXT))
                }
            }
        }
        }
    }

    if (showAddLocationDialog) {
        GeoWeatherGlassDialog(
            onDismissRequest = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() },
            title = stringResource(Res.string.search_title),
            actions = {
                FreetimeGlassAction(onClick = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
            }
        ) {
            FreetimeTextField(
                value = addLocationQuery,
                onValueChange = { addLocationQuery = it; viewModel.searchCity(it.trim()) },
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
                            Text(city.name, style = FreetimeDesign.typography.titleMedium)
                            Text("${city.latitude}, ${city.longitude}", style = FreetimeDesign.typography.bodySmall, color = FreetimeDesign.palette.contentMuted)
                        }
                    }
                }
            }
        }
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
        var selectedHour by remember(location.id) { mutableIntStateOf(initialHour) }
        var selectedMinute by remember(location.id) { mutableIntStateOf(initialMinute) }

        GeoWeatherGlassDialog(
            onDismissRequest = { notificationLocation = null },
            title = stringResource(Res.string.notification_time_title),
            actions = {
                if (location.notificationsEnabled) {
                    FreetimeGlassAction(onClick = {
                        viewModel.setLocationNotifications(location, false, location.notificationTime)
                        notificationLocation = null
                    }) { Text(stringResource(Res.string.notification_disable)) }
                    Spacer(Modifier.width(8.dp))
                }
                FreetimeGlassAction(onClick = { notificationLocation = null }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
                Spacer(Modifier.width(8.dp))
                FreetimeGlassAction(onClick = {
                    val time = "%02d:%02d".format(selectedHour, selectedMinute)
                    viewModel.setLocationNotifications(location, true, time)
                    notificationLocation = null
                }) { Text(stringResource(Res.string.confirm)) }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTimePart(
                    value = selectedHour,
                    onDecrease = { selectedHour = (selectedHour + 23) % 24 },
                    onIncrease = { selectedHour = (selectedHour + 1) % 24 }
                )
                Text(":", style = FreetimeDesign.typography.headlineLarge, modifier = Modifier.padding(horizontal = 10.dp))
                GlassTimePart(
                    value = selectedMinute,
                    onDecrease = { selectedMinute = (selectedMinute + 55) % 60 },
                    onIncrease = { selectedMinute = (selectedMinute + 5) % 60 }
                )
            }
        }
    }
}

@Composable
private fun GlassTimePart(
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FreetimeGlassAction(onClick = onIncrease) {
            Text("+", style = FreetimeDesign.typography.titleLarge)
        }
        Box(
            modifier = Modifier
                .freetimeGlass(RoundedCornerShape(22.dp), interactive = false)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("%02d".format(value), style = FreetimeDesign.typography.headlineMedium)
        }
        FreetimeGlassAction(onClick = onDecrease) {
            Text("−", style = FreetimeDesign.typography.titleLarge)
        }
    }
}