package io.github.freetimemaker.geoweather.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import io.github.freetimemaker.geoweather.data.LocationEntity
import geoweather.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LocationsViewModel,
    api: io.github.freetimemaker.geoweather.network.WeatherApi,
    onOpenDetail: (LocationEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDonate: () -> Unit,
    onGetCurrentLocation: () -> Unit
) {
    val locations by viewModel.locations.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showAddDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    IconButton(onClick = onGetCurrentLocation) {
                        Icon(Icons.Default.MyLocation, contentDescription = stringResource(Res.string.current_location))
                    }
                    IconButton(onClick = onOpenDonate) {
                        Icon(Icons.Default.Favorite, contentDescription = stringResource(Res.string.donate_nav_desc), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.settings_nav_desc))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.SearchBTNTXT)) }
            )
        }
    ) { innerPadding ->
        if (locations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.no_locations_msg),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) {
                items(locations) { loc ->
                    ListItem(
                        headlineContent = { Text(loc.name) },
                        supportingContent = { 
                            val latStr = loc.latitude.toString().take(5) // Einfache Formatierung für KMP
                            val lonStr = loc.longitude.toString().take(5)
                            Text("Lat: $latStr, Lon: $lonStr")
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.setAsDefault(loc) }) {
                                    Icon(
                                        if (loc.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = stringResource(if (loc.isDefault) Res.string.remove_default else Res.string.set_as_default),
                                        tint = if (loc.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { locationToDelete = loc }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.DelLoc), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectLocation(loc)
                                onOpenDetail(loc)
                            }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    if (showAddDialog) {
        // We'll need to implement a cross-platform AddLocationDialog
        // For now, let's just have a placeholder or a simple one if possible
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, lat, lon ->
                viewModel.addLocation(name, lat, lon)
                showAddDialog = false
            },
            api = api
        )
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

@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double) -> Unit,
    api: io.github.freetimemaker.geoweather.network.WeatherApi
) {
    val viewModel = remember { SearchViewModel(api) }
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.SearchForCity)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    label = { Text(stringResource(Res.string.CityName)) },
                    placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (isSearching) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(results) { result ->
                        ListItem(
                            headlineContent = { Text(result.name) },
                            supportingContent = { Text("${result.admin1 ?: ""}, ${result.country ?: ""}") },
                            modifier = Modifier.clickable {
                                onAdd(result.name, result.latitude, result.longitude)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.CancelTXT)) }
        }
    )
}
