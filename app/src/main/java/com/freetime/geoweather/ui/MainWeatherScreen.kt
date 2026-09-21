package com.freetime.geoweather.ui
import com.freetime.design.FreetimeIconButton
import com.freetime.design.FreetimeScaffold
import com.freetime.design.FreetimeFloatingActionButton
import com.freetime.design.FreetimeSnackbarHost
import com.freetime.design.rememberFreetimeMessageHostState
import com.freetime.design.FreetimeTimePicker
import com.freetime.design.FreetimeSearchBar
import com.freetime.design.FreetimeAdaptiveNavigation
import com.freetime.design.FreetimeNavigationDestination
import com.freetime.design.FreetimeBottomSheet
import com.freetime.design.FreetimeButton
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
import androidx.compose.material.icons.filled.MoreVert
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
    var actionLocation by remember { mutableStateOf<LocationEntity?>(null) }
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
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val locationUnavailableMsg = stringResource(Res.string.current_location_unavailable)
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
                        FreetimeIconButton(
                            icon = Icons.Default.MoreVert,
                            contentDescription = null,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                actionLocation = loc
                            }
                        )
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

            FreetimeAdaptiveNavigation(
                destinations = adaptiveDestinations,
                selectedIndex = 0,
                onDestinationSelected = { index ->
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    when (index) {
                        0 -> if (!isLocating) openCurrentLocation()
                        1 -> onDonateClick()
                        2 -> onSettingsClick()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                // The weather content remains the primary surface; adaptive navigation
                // overlays its compact bottom bar or wide navigation rail.
            }

            FreetimeFloatingActionButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(Res.string.SearchBTNTXT),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showAddLocationDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 92.dp)
            )
        }
        }
    }

    FreetimeBottomSheet(
        visible = actionLocation != null,
        onDismissRequest = { actionLocation = null }
    ) {
        actionLocation?.let { loc ->
            FreetimeGlassTitle(loc.name)
            FreetimeButton(
                text = if (loc.notificationsEnabled) stringResource(Res.string.notification_disable) else stringResource(Res.string.notification_time_title),
                onClick = {
                    actionLocation = null
                    notificationLocation = loc
                },
                leadingIcon = if (loc.notificationsEnabled) Icons.Default.NotificationsOff else Icons.Default.Notifications
            )
            FreetimeButton(
                text = stringResource(Res.string.favorite_location),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.toggleDefaultLocation(loc)
                    actionLocation = null
                },
                leadingIcon = if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder
            )
            FreetimeButton(
                text = stringResource(Res.string.DelLoc),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    actionLocation = null
                    locationToDelete = loc
                },
                leadingIcon = Icons.Default.Delete
            )
        }
    }

    if (showAddLocationDialog) {
        FreetimeDialog(
            onDismissRequest = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() },
            title = stringResource(Res.string.search_title),
            actions = {
                FreetimeGlassAction(onClick = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
            }
        ) {
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
        var selectedTime by remember(location.id) {
            mutableStateOf(java.time.LocalTime.of(initialHour, initialMinute))
        }

        FreetimeDialog(
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
                    val time = "%02d:%02d".format(selectedTime.hour, selectedTime.minute)
                    viewModel.setLocationNotifications(location, true, time)
                    notificationLocation = null
                }) { Text(stringResource(Res.string.confirm)) }
            }
        ) {
            FreetimeTimePicker(
                value = selectedTime,
                onValueChange = { selectedTime = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

