package com.freetime.geoweather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()
        
        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = sharedPreferences.collectAsState(key = "use_system_theme", defaultValue = true)
            val darkModeEnabled = sharedPreferences.collectAsState(key = "dark_mode_enabled", defaultValue = false)
            val dynamicColor = sharedPreferences.collectAsState(key = "dynamic_color", defaultValue = true)
            
            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value
            
            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value) {
                MainScreen(
                    onOpenDetail = { name, lat, lon ->
                        val intent = Intent(this@MainActivity, WeatherDetailActivity::class.java).apply {
                            putExtra("name", name)
                            putExtra("lat", lat)
                            putExtra("lon", lon)
                        }
                        startActivity(intent)
                    },
                    onOpenDonate = {
                        startActivity(Intent(this, DonateActivity::class.java))
                    }
                )
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenDetail: (String, Double, Double) -> Unit,
    onOpenDonate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { LocationDatabase.getDatabase(context) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val locations: List<LocationEntity> by db.locationDao()
        .getAllLocations()
        .observeAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("GeoWeather") },
                actions = {
                    IconButton(onClick = onOpenDonate) {
                        Icon(Icons.Default.Favorite, contentDescription = "Donate", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.SearchBTNTXT)) }
            )
        }
    ) { innerPadding ->
        if (locations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text = "Noch keine Standorte hinzugefügt.\nKlicke auf das Plus-Icon unten.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding
            ) {
                items(locations) { loc ->
                    ListItem(
                        headlineContent = { Text(loc.name) },
                        supportingContent = { Text("Lat: ${loc.latitude}, Lon: ${loc.longitude}") },
                        trailingContent = {
                            IconButton(onClick = { locationToDelete = loc }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.DelLoc), tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch(Dispatchers.IO) {
                                    db.locationDao().deselectAllLocations()
                                    db.locationDao().updateLocation(loc.copy(selected = true))
                                }
                                onOpenDetail(loc.name, loc.latitude, loc.longitude)
                            }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, lat, lon ->
                scope.launch(Dispatchers.IO) {
                    db.locationDao().deselectAllLocations()
                    db.locationDao().insertLocation(LocationEntity(name = name, latitude = lat, longitude = lon, selected = true))
                    withContext(Dispatchers.Main) { showAddDialog = false }
                }
            }
        )
    }

    locationToDelete?.let { location ->
        AlertDialog(
            onDismissRequest = { locationToDelete = null },
            title = { Text(stringResource(R.string.DelLoc)) },
            text = { Text(String.format(stringResource(R.string.DelLocConAsk), location.name)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        db.locationDao().deleteLocation(location)
                        withContext(Dispatchers.Main) { locationToDelete = null }
                    }
                }) {
                    Text(stringResource(R.string.DelTXT), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { locationToDelete = null }) {
                    Text(stringResource(R.string.CancelTXT))
                }
            }
        )
    }
}

private fun httpGet(urlString: String): String {
    val url = URL(urlString)
    val c = url.openConnection() as HttpURLConnection
    c.setRequestProperty("User-Agent", "GeoWeatherApp")
    c.connectTimeout = 12000
    c.readTimeout = 12000
    BufferedReader(InputStreamReader(c.inputStream, StandardCharsets.UTF_8)).use { reader ->
        val sb = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) sb.append(line)
        return sb.toString()
    }
}

@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Triple<String, Double, Double>>()) }
    var loading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.SearchForCity)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.CityName)) },
                    placeholder = { Text("z.B. Berlin oder 52.52, 13.40") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        loading = true
                        results = emptyList()
                        scope.launch(Dispatchers.IO) {
                            try {
                                val coordinatePattern = Regex("""^(-?\\d+\\.?\\d*)\\s*,\\s*(-?\\d+\\.?\\d*)$""")
                                val matchResult = coordinatePattern.matchEntire(query.trim())
                                val url = if (matchResult != null) {
                                    val latitude = matchResult.groupValues[1].toDouble()
                                    val longitude = matchResult.groupValues[2].toDouble()
                                    "https://geocoding-api.open-meteo.com/v1/reverse?latitude=$latitude&longitude=$longitude&language=" + Locale.getDefault().language + "&format=json"
                                } else {
                                    "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(query, "UTF-8") + "&count=20&language=" + Locale.getDefault().language + "&format=json"
                                }
                                val json = httpGet(url)
                                val obj = JSONObject(json)
                                val list = mutableListOf<Triple<String, Double, Double>>()
                                val arr = if (obj.has("results")) obj.optJSONArray("results") ?: JSONArray()
                                          else if (obj.has("name")) JSONArray().apply { put(obj) }
                                          else JSONArray()
                                for (i in 0 until arr.length()) {
                                    val item = arr.getJSONObject(i)
                                    val name = item.optString("name", "Unknown")
                                    val lat = item.optDouble("latitude", 0.0)
                                    val lon = item.optDouble("longitude", 0.0)
                                    var displayName = name
                                    if (item.has("admin1")) displayName += ", " + item.getString("admin1")
                                    if (item.has("country")) displayName += ", " + item.getString("country")
                                    list.add(Triple(displayName, lat, lon))
                                }
                                withContext(Dispatchers.Main) { results = list; loading = false }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { loading = false }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.SearchBTNTXT))
                }
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (results.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(results) { (name, lat, lon) ->
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text("Lat: $lat, Lon: $lon") },
                                modifier = Modifier.clickable { onAdd(name, lat, lon) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.CancelTXT)) }
        }
    )
}
