package com.freetime.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.getCurrentCoordinates
import geoweather.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWeatherScreen(
    viewModel: WeatherViewModel,
    onAddLocationClick: () -> Unit,
    onLocationClick: (LocationEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onDonateClick: () -> Unit,
    onRadarClick: () -> Unit,
    onCurrentLocationClick: (String, Double, Double) -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentLocationName = stringResource(Res.string.current_location)
    val locationUnavailableMsg = stringResource(Res.string.current_location_unavailable)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLocating = true
                                try {
                                    val coords = getCurrentCoordinates()
                                    if (coords != null) {
                                        onCurrentLocationClick(currentLocationName, coords.first, coords.second)
                                    } else {
                                        snackbarHostState.showSnackbar(locationUnavailableMsg)
                                    }
                                } finally {
                                    isLocating = false
                                }
                            }
                        },
                        enabled = !isLocating
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = currentLocationName)
                        }
                    }
                    IconButton(onClick = onRadarClick) {
                        Icon(Icons.Default.Public, contentDescription = "Radar")
                    }
                    IconButton(onClick = onDonateClick) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = stringResource(Res.string.donate_nav_desc),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.settings_nav_desc))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddLocationClick,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding
            ) {
                items(locations, key = { it.id }) { loc ->
                    ListItem(
                        headlineContent = { Text(loc.name) },
                        supportingContent = {
                            Text("${loc.latitude}, ${loc.longitude}")
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.toggleLocationNotifications(loc) }) {
                                    Icon(
                                        if (loc.notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (loc.notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.toggleDefaultLocation(loc) }) {
                                    Icon(
                                        if (loc.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = stringResource(
                                            if (loc.isDefault) Res.string.remove_default else Res.string.set_as_default
                                        ),
                                        tint = if (loc.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectLocation(loc)
                                onLocationClick(loc)
                            }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                item {
                    TextButton(
                        onClick = onDonateClick,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
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

    locationToDelete?.let { location ->
        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            title = { Text(stringResource(Res.string.DelLoc)) },
            text = { Text(stringResource(Res.string.DelLocConAsk, location.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLocation(location)
                    locationToDelete = null
                }) {
                    Text(stringResource(Res.string.DelTXT), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { locationToDelete = null }) {
                    Text(stringResource(Res.string.CancelTXT))
                }
            }
        )
    }
}
