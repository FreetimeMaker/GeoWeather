package com.freetime.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
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
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }
    var notificationLocation by remember { mutableStateOf<LocationEntity?>(null) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var addLocationQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val orderedLocations = locations
    var isLocating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentLocationName = stringResource(Res.string.current_location)
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
                title = stringResource(Res.string.app_name)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .geoWeatherGlassCapsule(interactive = false)
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { openCurrentLocation() }, enabled = !isLocating) {
                        if (isLocating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.MyLocation, contentDescription = currentLocationName)
                    }
                    IconButton(
                        onClick = { showAddLocationDialog = true },
                        modifier = Modifier.geoWeatherGlassCapsule(interactive = true)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.SearchBTNTXT))
                    }
                    IconButton(onClick = onDonateClick) {
                        Icon(Icons.Default.Favorite, contentDescription = stringResource(Res.string.donate_nav_desc))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.settings_nav_desc))
                    }
                }
            }
        }
    ) { padding ->
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
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                if (locations.size > 1) {
                    Text(
                        text = stringResource(Res.string.compare_locations),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .geoWeatherGlass(RoundedCornerShape(18.dp), interactive = false)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(orderedLocations, key = { "compare-${it.id}" }) { loc ->
                            Box(
                                modifier = Modifier
                                    .width(156.dp)
                                    .geoWeatherGlass(RoundedCornerShape(22.dp), interactive = true)
                                    .clickable {
                                        viewModel.selectLocation(loc)
                                        onLocationClick(loc)
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(loc.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(
                                        loc.currentTemp?.let { "${it.toInt()}°C" } ?: "--",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(
                                        loc.currentWindSpeed?.let { stringResource(Res.string.wind_value, it.toInt()) } ?: "--",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        loc.currentHumidity?.let { stringResource(Res.string.humidity_value, it) } ?: "--",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                items(orderedLocations, key = { it.id }) { loc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = true)
                            .clickable {
                                viewModel.selectLocation(loc)
                                onLocationClick(loc)
                            }
                            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loc.name, style = MaterialTheme.typography.titleMedium)
                            Text("${loc.latitude}, ${loc.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { notificationLocation = loc }) {
                            Icon(if (loc.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff, contentDescription = null)
                        }
                        IconButton(onClick = { viewModel.toggleDefaultLocation(loc) }) {
                            Icon(if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = stringResource(Res.string.favorite_location))
                        }
                        IconButton(onClick = { locationToDelete = loc }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.DelLoc), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                item {
                    GeoWeatherGlassAction(
                        onClick = onDonateClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .geoWeatherGlass(RoundedCornerShape(24.dp))
                    ) {
                        Text(
                            text = stringResource(Res.string.main_donation_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .geoWeatherGlass(RoundedCornerShape(32.dp), interactive = false),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            title = { Text(stringResource(Res.string.DelLoc)) },
            text = { Text(stringResource(Res.string.DelLocConAsk, location.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLocation(location)
                        locationToDelete = null
                    },
                    modifier = Modifier.geoWeatherGlass(RoundedCornerShape(20.dp))
                ) {
                    Text(stringResource(Res.string.DelTXT), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { locationToDelete = null },
                    modifier = Modifier.geoWeatherGlass(RoundedCornerShape(20.dp))
                ) {
                    Text(stringResource(Res.string.CancelTXT))
                }
            }
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