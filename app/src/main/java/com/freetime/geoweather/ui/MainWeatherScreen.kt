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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.freetime.geoweather.ui.glass.geoWeatherLiquidGlass
import com.freetime.geoweather.ui.glass.LocalGeoWeatherBackdrop
import com.freetime.geoweather.ui.glass.geoWeatherGlass
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.getCurrentCoordinates
import com.freetime.geoweather.getDetectedLocationName
import com.freetime.geoweather.AppwriteData
import com.freetime.geoweather.GeoWeatherAccount
import com.freetime.geoweather.SubscriptionPlan
import com.freetime.geoweather.SubscriptionPlans
import kotlinx.coroutines.delay
import com.freetime.geoweather.R as Res
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWeatherScreen(
    viewModel: WeatherViewModel,
    onAddLocationClick: () -> Unit,
    onLocationClick: (LocationEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onDonateClick: () -> Unit,
    onCurrentLocationClick: (String, Double, Double) -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    val context = LocalContext.current
    var account by remember { mutableStateOf<GeoWeatherAccount?>(null) }
    var plan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    LaunchedEffect(Unit) {
        account = runCatching { AppwriteData.account(context) }.getOrNull()
        val plans = SubscriptionPlans.load()
        plan = account?.subscription?.lowercase()?.let { plans[it] }
    }
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }
    var notificationLocation by remember { mutableStateOf<LocationEntity?>(null) }
    val orderedLocations = locations
    var isLocating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentLocationName = stringResource(Res.string.current_location)
    val locationUnavailableMsg = stringResource(Res.string.current_location_unavailable)
    val backdrop = LocalGeoWeatherBackdrop.current
    val navigationShape = RoundedCornerShape(50)

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
            CenterAlignedTopAppBar(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    .geoWeatherGlass(RoundedCornerShape(28.dp), interactive = false),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(Res.string.app_name))
                        account?.let {
                            Text(
                                stringResource(Res.string.plan_badge, it.subscription.uppercase()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    // Floating pill: leave visible space around all edges, especially the bottom.
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
                    .height(68.dp)
                    .geoWeatherLiquidGlass(backdrop, navigationShape, interactive = true),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { openCurrentLocation() },
                    enabled = !isLocating,
                    icon = {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = currentLocationName)
                        }
                    },
                    label = { Text(currentLocationName) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onDonateClick,
                    icon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = stringResource(Res.string.donate_nav_desc)
                        )
                    },
                    label = { Text(stringResource(Res.string.donate_nav_desc)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.settings_nav_desc)
                        )
                    },
                    label = { Text(stringResource(Res.string.settings_nav_desc)) }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .geoWeatherGlass(RoundedCornerShape(50)),
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                onClick = {
                    val activePlan = plan
                    val activeAccount = account
                    if (activePlan != null && locations.size >= activePlan.maxLocations && activeAccount != null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(
                                    Res.string.location_limit_reached,
                                    activeAccount.subscription.uppercase(),
                                    activePlan.maxLocations
                                )
                            )
                        }
                    } else {
                        onAddLocationClick()
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.SearchBTNTXT)) }
            )
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
                            Card(
                                modifier = Modifier
                                    .width(156.dp)
                                    .geoWeatherGlass(RoundedCornerShape(22.dp), interactive = false),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    ListItem(
                        headlineContent = { Text(loc.name) },
                        supportingContent = { Text("${loc.latitude}, ${loc.longitude}") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    if (plan?.notifications == false) {
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(Res.string.notifications_plan_required)) }
                                    } else {
                                        notificationLocation = loc
                                    }
                                }) {
                                    Icon(
                                        if (loc.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (loc.notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleDefaultLocation(loc) }) {
                                    Icon(
                                        if (loc.isDefault) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = stringResource(Res.string.favorite_location),
                                        tint = if (loc.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column {
                                    IconButton(onClick = { viewModel.moveLocation(loc, -1) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                                    }
                                    IconButton(onClick = { viewModel.moveLocation(loc, 1) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                    }
                                }
                                IconButton(onClick = { locationToDelete = loc }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(Res.string.DelLoc),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .geoWeatherGlass(RoundedCornerShape(22.dp), interactive = false)
                            .clickable {
                                viewModel.selectLocation(loc)
                                onLocationClick(loc)
                            }
                    )
                }
                item {
                    TextButton(
                        onClick = onDonateClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .geoWeatherGlass(RoundedCornerShape(24.dp))
                    ) {
                        Text(
                            text = stringResource(Res.string.main_donation_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
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
        val timeState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { notificationLocation = null },
            modifier = Modifier.padding(horizontal = 24.dp).geoWeatherGlass(RoundedCornerShape(32.dp), interactive = false),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            title = { Text(stringResource(Res.string.notification_time_title)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val time = "%02d:%02d".format(timeState.hour, timeState.minute)
                        viewModel.setLocationNotifications(location, true, time)
                        notificationLocation = null
                    },
                    modifier = Modifier.geoWeatherGlass(RoundedCornerShape(20.dp))
                ) { Text(stringResource(Res.string.confirm)) }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (location.notificationsEnabled) {
                        TextButton(
                            onClick = {
                                viewModel.setLocationNotifications(location, false, location.notificationTime)
                                notificationLocation = null
                            },
                            modifier = Modifier.geoWeatherGlass(RoundedCornerShape(20.dp))
                        ) { Text(stringResource(Res.string.notification_disable)) }
                    }
                    TextButton(
                        onClick = { notificationLocation = null },
                        modifier = Modifier.geoWeatherGlass(RoundedCornerShape(20.dp))
                    ) { Text(stringResource(Res.string.CancelTXT)) }
                }
            }
        )
    }
}
