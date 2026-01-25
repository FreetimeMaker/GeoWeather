package com.freetime.geoweather

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.ui.AddLocationDialog
import com.freetime.geoweather.ui.LocationItem
import com.freetime.geoweather.ui.LocationsViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var viewModel: LocationsViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Check if locations are already saved; only request GPS on the very first start (no locations)
        LocationDatabase.databaseWriteExecutor.execute {
            val count = LocationDatabase.getDatabase(application).locationDao().getCount()
            if (count == 0 && isFirstStart()) {
                runOnUiThread { requestLocationPermissionAndStart() }
            } else if (isFirstStart()) {
                // If locations are already present, mark as done so it doesn't run again
                setFirstStartDone()
            }
        }

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {
        val viewModel: LocationsViewModel = viewModel()
        val locations by viewModel.locations.collectAsState(initial = emptyList())
        var showAddDialog by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(locations) { location ->
                    LocationItem(
                        location = location,
                        onItemClick = { loc ->
                            val intent = Intent(this@MainActivity, WeatherDetailActivity::class.java).apply {
                                putExtra("location_name", loc.name)
                                putExtra("latitude", loc.latitude)
                                putExtra("longitude", loc.longitude)
                            }
                            startActivity(intent)
                        },
                        onDeleteClick = { loc ->
                            viewModel.deleteLocation(loc)
                        }
                    )
                }
            }

            // FABs
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        startActivity(Intent(this@MainActivity, DonateActivity::class.java))
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.donation),
                        contentDescription = "Donate"
                    )
                }

                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_input_add),
                        contentDescription = "Add location"
                    )
                }
            }

            if (showAddDialog) {
                AddLocationDialog(
                    onDismiss = { showAddDialog = false },
                    onLocationSelected = { location ->
                        viewModel.addLocation(location.name, location.latitude, location.longitude)
                        Toast.makeText(
                            this@MainActivity,
                            "${location.name} added",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        val lat = location.latitude
        val lon = location.longitude

        // Reverse-Geocoding
        val displayName = reverseGeocode(lat, lon)

        // On first start, automatically save
        if (isFirstStart()) {
            viewModel.addLocation(displayName, lat, lon)
            setFirstStartDone()
        }

        // Open detail page
        val intent = Intent(this, WeatherDetailActivity::class.java).apply {
            putExtra("location_name", displayName)
            putExtra("latitude", lat)
            putExtra("longitude", lon)
        }
        startActivity(intent)
    }

    private fun requestLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startLocationUpdates()
        }
    }

    private fun isFirstStart(): Boolean {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("first_start", true)
    }

    private fun setFirstStartDone() {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("first_start", false)
            .apply()
    }

    private fun startLocationUpdates() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000, 10f, this
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 10000, 50f, this
                )
            }
        } catch (e: SecurityException) {
            Log.e("Location", "Permission error: ${e.message}")
        }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val url = "https://geocoding-api.open-meteo.com/v1/reverse?latitude=$lat&longitude=$lon"
            val json = httpGet(url, "GeoWeatherApp")
            val obj = JSONObject(json)

            val results = obj.optJSONArray("results")
            if (results != null && results.length() > 0) {
                val item = results.getJSONObject(0)

                val name = item.optString("name", "")
                val admin1 = item.optString("admin1", "")
                val country = item.optString("country", "")

                if (admin1.isNotEmpty()) {
                    "$name, $admin1, $country"
                } else {
                    "$name, $country"
                }
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            Log.e("ReverseGeocode", "Error: ${e.message}")
            "Unknown Location"
        }
    }

    private fun httpGet(urlString: String, userAgent: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", userAgent)
        connection.connectTimeout = 12000
        connection.readTimeout = 12000

        return connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
}