package com.freetime.geoweather

import android.Manifest
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
                        val intent = Intent(this, WeatherDetailActivity::class.java).apply {
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

    val locations: List<LocationEntity> by db.locationDao()
        .getAllLocations()
        .observeAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }

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
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }) {
                Text("⚙")
            }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("🔍")
            }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(onClick = onOpenDonate) {
                Text("♥")
            }
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, lat, lon ->
                scope.launch(Dispatchers.IO) {
                    db.locationDao().insertLocation(LocationEntity(name = name, latitude = lat, longitude = lon))
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
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        loading = true
                        results = emptyList()
                        val url = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                                URLEncoder.encode(query, "UTF-8") +
                                "&count=20&language=" + Locale.getDefault().language +
                                "&format=json"
                        scope.launch(Dispatchers.IO) {
                            try {
                                val json = httpGet(url)
                                val obj = JSONObject(json)
                                val arr = obj.optJSONArray("results") ?: JSONArray()
                                val list = mutableListOf<Triple<String, Double, Double>>()
                                for (i in 0 until arr.length()) {
                                    val item = arr.getJSONObject(i)
                                    val name = item.getString("name")
                                    val lat = item.getDouble("latitude")
                                    val lon = item.getDouble("longitude")
                                    list.add(Triple(name, lat, lon))
                                }
                                withContext(Dispatchers.Main) {
                                    results = list
                                    loading = false
                                }
                            } catch (e: Exception) {
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