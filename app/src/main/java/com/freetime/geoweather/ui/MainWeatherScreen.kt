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
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import com.freetime.geoweather.ui.glass.geoWeatherGlassCapsule
import com.freetime.geoweather.ui.glass.GeoWeatherGlassTopBar
import com.freetime.geoweather.ui.glass.GeoWeatherGlassTextField
import com.freetime.geoweather.ui.glass.GeoWeatherGlassAction
import com.freetime.geoweather.ui.glass.GeoWeatherGlassIconAction
import com.freetime.geoweather.ui.glass.GeoWeatherGlassDialog
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.getCurrentCoordinates
import com.freetime.geoweather.getDetectedLocationName
import com.freetime.geoweather.WeatherIconMapper
import kotlinx.coroutines.delay
import com.freetime.geoweather.R as Res
import kotlinx.coroutines.launch

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
    val navigationCompact by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 120 } }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GeoWeatherGlassTopBar(
                title = stringResource(Res.string.app_name),
                compact = navigationCompact,
                compactSubtitle = if (navigationCompact) locations.firstOrNull()?.currentTemp?.let { "${it.toInt()}°" } else null
            )
        } 
    ) { padding ->
        Box(Modifier.fillMaxSize()) {

        if (locations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.no_locations_msg),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(24.dp)
                        .geoWeatherGlass(RoundedCornerShape(24.dp), interactive = false)
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                )
            }
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
                        Text(
                            text = stringResource(Res.string.compare_locations),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .geoWeatherGlass(RoundedCornerShape(18.dp), interactive = false)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(orderedLocations, key = { "landscape-" + it.id }) { loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .geoWeatherGlass(RoundedCornerShape(24.dp), interactive = true)
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
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        Text(
                                            text = loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--",
                                            style = MaterialTheme.typography.labelSmall
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
                                    .geoWeatherGlass(RoundedCornerShape(30.dp), interactive = true)
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
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(loc.currentWindSpeed?.let { speed -> stringResource(Res.string.wind_value, speed.toInt()) } ?: "--")
                                        Text(loc.currentHumidity?.let { humidity -> stringResource(Res.string.humidity_value, humidity) } ?: "--")
                                    }
                                }
                            }
                        }
                        GeoWeatherGlassAction(
                            onClick = onDonateClick,
                            modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(24.dp))
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
                                    .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false)
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
                                            Text(windiest?.name ?: "--", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(stringResource(Res.string.compare_most_humid), style = MaterialTheme.typography.labelSmall)
                                            Text(humid?.name ?: "--", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "location-overview-title") {
                        Text(
                            text = stringResource(Res.string.location_overview_title),
                            style = MaterialTheme.typography.titleMedium,
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
                                        .geoWeatherGlass(RoundedCornerShape(24.dp), interactive = true)
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
                            .geoWeatherGlass(RoundedCornerShape(26.dp), interactive = true)
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
                        GeoWeatherGlassIconAction(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); notificationLocation = loc }) {
                            Icon(if (loc.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff, contentDescription = null)
                        }
                        GeoWeatherGlassIconAction(onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); viewModel.toggleDefaultLocation(loc) }) {
                            Icon(if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = stringResource(Res.string.favorite_location))
                        }
                        GeoWeatherGlassIconAction(onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); locationToDelete = loc }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.DelLoc), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                item(key = "donate") {
                    GeoWeatherGlassAction(
                        onClick = onDonateClick,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(text = stringResource(Res.string.main_donation_hint), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .wrapContentWidth()
                    .geoWeatherGlassCapsule(interactive = true)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); openCurrentLocation() }, contentAlignment = Alignment.Center) {
                    if (isLocating) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.MyLocation, contentDescription = currentLocationName)
                }
                AnimatedVisibility(
                    visible = !navigationCompact,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(42.dp).clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); showAddLocationDialog = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.SearchBTNTXT))
                        }
                        Box(Modifier.size(42.dp).clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onDonateClick() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Favorite, contentDescription = stringResource(Res.string.donate_nav_desc))
                        }
                    }
                }
                Box(Modifier.size(42.dp).clickable { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSettingsClick() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.settings_nav_desc))
                }
            }
        }
    }

    if (showAddLocationDialog) {
        GeoWeatherGlassDialog(
            onDismissRequest = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() },
            title = stringResource(Res.string.search_title),
            actions = {
                GeoWeatherGlassAction(onClick = { showAddLocationDialog = false; addLocationQuery = ""; viewModel.clearSearch() }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
            }
        ) {
            GeoWeatherGlassTextField(
                value = addLocationQuery,
                onValueChange = { addLocationQuery = it; viewModel.searchCity(it.trim()) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringResource(Res.string.search_placeholder)
            )
            if (isSearching) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (addLocationQuery.isNotBlank()) {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(searchResults, key = { "add-${it.latitude},${it.longitude}" }) { city ->
                        Column(
                            modifier = Modifier.fillMaxWidth().geoWeatherGlass(RoundedCornerShape(22.dp), interactive = true)
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
    }

    locationToDelete?.let { location ->
        GeoWeatherGlassDialog(
            onDismissRequest = { locationToDelete = null },
            title = stringResource(Res.string.DelLoc),
            actions = {
                GeoWeatherGlassAction(onClick = { locationToDelete = null }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
                Spacer(Modifier.width(8.dp))
                GeoWeatherGlassAction(onClick = {
                    viewModel.deleteLocation(location)
                    locationToDelete = null
                }) {
                    Text(stringResource(Res.string.DelTXT), color = MaterialTheme.colorScheme.error)
                }
            }
        ) {
            Text(stringResource(Res.string.DelLocConAsk, location.name))
        }
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
                    GeoWeatherGlassAction(onClick = {
                        viewModel.setLocationNotifications(location, false, location.notificationTime)
                        notificationLocation = null
                    }) { Text(stringResource(Res.string.notification_disable)) }
                    Spacer(Modifier.width(8.dp))
                }
                GeoWeatherGlassAction(onClick = { notificationLocation = null }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
                Spacer(Modifier.width(8.dp))
                GeoWeatherGlassAction(onClick = {
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
                Text(":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 10.dp))
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
        GeoWeatherGlassAction(onClick = onIncrease) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
        Box(
            modifier = Modifier
                .geoWeatherGlass(RoundedCornerShape(22.dp), interactive = false)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("%02d".format(value), style = MaterialTheme.typography.headlineMedium)
        }
        GeoWeatherGlassAction(onClick = onDecrease) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
    }
}