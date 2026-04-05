package com.freetime.geoweather

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import com.freetime.geoweather.R
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import com.freetime.geoweather.freetimesdk.FreetimeSDKManager
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
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, notifications can be scheduled
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        
        // Initialize FreetimeSDK
        FreetimeSDKManager.initialize(this@MainActivity)
        
        // Check and request notification permission
        checkNotificationPermission()
        
        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = remember { sharedPreferences.getBoolean("use_system_theme", true) }
            val darkModeEnabled = remember { sharedPreferences.getBoolean("dark_mode_enabled", false) }
            
            val darkTheme = if (useSystemTheme) {
                isSystemInDarkTheme()
            } else {
                darkModeEnabled
            }
            
            GeoWeatherTheme(darkTheme = darkTheme) {
                MainScreen(
                    onOpenDetail = { name, lat, lon ->
                        // Mark this location as selected before opening detail
                        scope.launch(Dispatchers.IO) {
                            val db = LocationDatabase.getDatabase(context)
                            db.locationDao().deselectAllLocations()
                            val location = db.locationDao().findByCoordinates(lat, lon)
                            if (location != null) {
                                db.locationDao().updateLocation(location.copy(selected = true))
                            }
                        }
                        
                        val intent = Intent(this@MainActivity, WeatherDetailActivity::class.java).apply {
                            putExtra("name", name)
                            putExtra("lat", lat)
                            putExtra("lon", lon)
                        }
                        startActivity(intent)
                    },
                    onOpenDonate = {
                        val intent = Intent(this, DonateActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }
}

@Composable
fun MainScreen(
    onOpenDetail: (String, Double, Double) -> Unit,
    onOpenDonate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { LocationDatabase.getDatabase(context) }
    val sdkManager = remember { FreetimeSDKManager.getInstance(context) }

    val locations: List<LocationEntity> by db.locationDao()
        .getAllLocations()
        .observeAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }

    // Track app usage
    LaunchedEffect(Unit) {
        sdkManager.trackAppUsage("main_screen")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
        ) {
            items(locations) { loc ->
                ListItem(
                    headlineContent = { Text(loc.name) },
                    supportingContent = {
                        Text("Lat: ${loc.latitude}, Lon: ${loc.longitude}")
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                locationToDelete = loc
                                sdkManager.trackUserInteraction("location_delete_initiated", mapOf(
                                    "location_name" to loc.name
                                ))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.DelLoc),
                                tint = Color.Red
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sdkManager.trackUserInteraction("location_selected", mapOf(
                                "location_name" to loc.name,
                                "latitude" to loc.latitude,
                                "longitude" to loc.longitude
                            ))
                            onOpenDetail(loc.name, loc.latitude, loc.longitude)
                        }
                )
                HorizontalDivider()
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(onClick = { 
                sdkManager.trackUserInteraction("settings_opened")
                val intent = Intent(context, SettingsActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("⚙")
            }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(onClick = { 
                sdkManager.trackUserInteraction("search_dialog_opened")
                showAddDialog = true 
            }) {
                Text("🔍")
            }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(onClick = { 
                sdkManager.trackUserInteraction("donate_screen_opened")
                onOpenDonate() 
            }) {
                Text("♥")
            }
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, lat, lon ->
                scope.launch(Dispatchers.IO) {
                    // Deselect all other locations
                    db.locationDao().deselectAllLocations()
                    // Insert new location as selected
                    db.locationDao().insertLocation(LocationEntity(
                        name = name, 
                        latitude = lat, 
                        longitude = lon,
                        selected = true
                    ))
                    withContext(Dispatchers.Main) {
                        showAddDialog = false
                    }
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
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.locationDao().deleteLocation(location)
                            withContext(Dispatchers.Main) {
                                locationToDelete = null
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.DelTXT), color = Color.Red)
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
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.CityName)) },
                    placeholder = { Text("e.g. New York or 40.7128, -74.0060") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        loading = true
                        results = emptyList()

                        scope.launch(Dispatchers.IO) {
                            try {
                                // Check if input is coordinates (e.g., "40.7128, -74.0060")
                                val coordinatePattern = Regex("""^(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)$""")
                                val matchResult = coordinatePattern.matchEntire(query.trim())

                                val url = if (matchResult != null) {
                                    // Input is coordinates - use reverse geocoding
                                    val latitude = matchResult.groupValues[1].toDouble()
                                    val longitude = matchResult.groupValues[2].toDouble()
                                    "https://geocoding-api.open-meteo.com/v1/reverse?latitude=$latitude&longitude=$longitude&language=" +
                                            Locale.getDefault().language + "&format=json"
                                } else {
                                    // Input is a city name - use normal geocoding
                                    val queryParts = query.split(",").map { it.trim() }
                                    val cityName = queryParts[0]
                                    val adminOrCountry = if (queryParts.size > 1) queryParts[1] else ""

                                    var searchUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                                            URLEncoder.encode(cityName, "UTF-8")

                                    if (adminOrCountry.isNotEmpty()) {
                                        searchUrl += "&admin1=" + URLEncoder.encode(adminOrCountry, "UTF-8")
                                    }

                                    searchUrl += "&count=20&language=" + Locale.getDefault().language +
                                            "&format=json"
                                    searchUrl
                                }

                                val json = httpGet(url)
                                val obj = JSONObject(json)
                                val list = mutableListOf<Triple<String, Double, Double>>()

                                // Handle both search and reverse results
                                val arr = if (obj.has("results")) {
                                    obj.optJSONArray("results") ?: JSONArray()
                                } else if (obj.has("name")) {
                                    // Single result from reverse geocoding
                                    JSONArray().apply {
                                        put(obj)
                                    }
                                } else {
                                    JSONArray()
                                }

                                for (i in 0 until arr.length()) {
                                    val item = arr.getJSONObject(i)
                                    val name = item.optString("name", "Unknown")
                                    val lat = item.optDouble("latitude", 0.0)
                                    val lon = item.optDouble("longitude", 0.0)

                                    // Build a more informative display name
                                    var displayName = name
                                    if (item.has("admin1")) {
                                        displayName += ", " + item.getString("admin1")
                                    }
                                    if (item.has("country")) {
                                        displayName += ", " + item.getString("country")
                                    }

                                    list.add(Triple(displayName, lat, lon))
                                }

                                withContext(Dispatchers.Main) {
                                    results = list
                                    loading = false
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    loading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.SearchBTNTXT))
                }
                Text(
                    text = stringResource(R.string.ClickOnCityToAddTXT),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                if (loading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(results) { (name, lat, lon) ->
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text("Lat: $lat, Lon: $lon") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAdd(name, lat, lon)
                                        onDismiss()
                                    }
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