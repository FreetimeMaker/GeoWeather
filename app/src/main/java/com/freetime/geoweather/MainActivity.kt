package com.freetime.geoweather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import android.content.Intent
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoWeatherTheme {
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
}

@Composable
fun MainScreen(
    onOpenDetail: (String, Double, Double) -> Unit,
    onOpenDonate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { LocationDatabase.getDatabase(context) }

    // LiveData → Compose State
    val locations: List<LocationEntity> by db.locationDao()
        .getAllLocations()
        .observeAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startLocationUpdates(context) { lat, lon ->
                scope.launch(Dispatchers.IO) {
                    val name = reverseGeocode(lat, lon)
                    db.locationDao().insertLocation(LocationEntity(name = name, latitude = lat, longitude = lon))
                    withContext(Dispatchers.Main) {
                        onOpenDetail(name, lat, lon)
                    }
                }
            }
        } else {
            Toast.makeText(context, "GPS-Berechtigung verweigert", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    if (fine == PackageManager.PERMISSION_GRANTED) {
                        startLocationUpdates(context) { lat, lon ->
                            scope.launch(Dispatchers.IO) {
                                val name = reverseGeocode(lat, lon)
                                db.locationDao().insertLocation(LocationEntity(name = name, latitude = lat, longitude = lon))
                                withContext(Dispatchers.Main) {
                                    onOpenDetail(name, lat, lon)
                                }
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Text("+")
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
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(locations) { loc ->
                ListItem(
                    headlineContent = { Text(loc.name) },
                    supportingContent = {
                        Text("Lat: ${loc.latitude}, Lon: ${loc.longitude}")
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
}

private fun startLocationUpdates(
    context: Context,
    onLocation: (Double, Double) -> Unit
) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
        lm.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000,
            10f,
            object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    onLocation(loc.latitude, loc.longitude)
                    lm.removeUpdates(this)
                }
            },
            Looper.getMainLooper()
        )
    }
}

private fun reverseGeocode(lat: Double, lon: Double): String {
    return try {
        val url = "https://geocoding-api.open-meteo.com/v1/reverse?latitude=$lat&longitude=$lon"
        val json = httpGet(url)
        val obj = JSONObject(json)
        val results = obj.optJSONArray("results")
        if (results != null && results.length() > 0) {
            val item = results.getJSONObject(0)
            val name = item.optString("name", "")
            val admin1 = item.optString("admin1", "")
            val country = item.optString("country", "")
            if (admin1.isNotEmpty()) "$name, $admin1, $country"
            else "$name, $country"
        } else "Unknown Location"
    } catch (e: Exception) {
        "Error: ${e.message}"
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
        title = { Text("Search for a City") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("City name") },
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
                    Text("Search")
                }
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}