package com.freetime.geoweather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.freetime.geoweather.data.LocationDatabase
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.ui.LocationsViewModel
import com.freetime.geoweather.ui.theme.GeoWeatherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            // Permission granted
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        checkNotificationPermission()
        
        handleAuthCallback(intent)

        val viewModel = LocationsViewModel(application)
        viewModel.syncWithCloud()

        val sharedPrefs = getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
        val requireLogin = sharedPrefs.getBoolean("require_login", false)
        val authManager = AuthManager.getInstance(this)

        if (requireLogin && !authManager.isAuthenticated) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        
        val db = LocationDatabase.getDatabase(this)
        lifecycleScope.launch {
            val locations = withContext(Dispatchers.IO) {
                db.locationDao().getAllLocationsSync()
            }
            
            val defaultLoc = locations.find { it.isDefault }
            val targetLoc = defaultLoc ?: if (locations.size == 1) locations.first() else null
            
            if (targetLoc != null) {
                val intent = Intent(this@MainActivity, WeatherDetailActivity::class.java).apply {
                    putExtra("name", targetLoc.name)
                    putExtra("lat", targetLoc.latitude)
                    putExtra("lon", targetLoc.longitude)
                }
                startActivity(intent)
            }
        }
        
        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = sharedPreferences.collectAsState(key = "use_system_theme", defaultValue = true)
            val darkModeEnabled = sharedPreferences.collectAsState(key = "dark_mode_enabled", defaultValue = false)
            val dynamicColor = sharedPreferences.collectAsState(key = "dynamic_color", defaultValue = true)
            
            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value
            
            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value) {
                MainScreen(
                    onRequestLocationPermission = {
                        requestLocationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthCallback(intent)
    }

    private fun handleAuthCallback(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "geoweather" && data.host == "auth") {
            val token = data.getQueryParameter("token")
            val refreshToken = data.getQueryParameter("refreshToken")
            val id = data.getQueryParameter("id")
            val email = data.getQueryParameter("email")
            val name = data.getQueryParameter("name")
            val pic = data.getQueryParameter("picture") ?: data.getQueryParameter("profile_picture") ?: ""
            val tier = data.getQueryParameter("tier") ?: "free"

            if (token != null) {
                val authMgr = AuthManager.getInstance(this)
                authMgr.saveAuthData(token, refreshToken ?: "", id ?: "", email ?: "", name ?: "", tier, pic)
                Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                
                // Sync after login
                LocationsViewModel(application).syncWithCloud()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onRequestLocationPermission: () -> Unit,
    onOpenDetail: (String, Double, Double) -> Unit,
    onOpenDonate: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel = remember { LocationsViewModel(context.applicationContext as android.app.Application) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val locations: List<LocationEntity> by viewModel.locations.observeAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var locationToDelete by remember { mutableStateOf<LocationEntity?>(null) }
    var isLocating by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                onRequestLocationPermission()
                            } else {
                                isLocating = true
                                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val providers = locationManager.getProviders(true)
                                var bestLocation: Location? = null
                                for (provider in providers) {
                                    val l = try {
                                        locationManager.getLastKnownLocation(provider)
                                    } catch (e: SecurityException) {
                                        null
                                    }
                                    if (l != null && (bestLocation == null || l.accuracy < bestLocation.accuracy)) {
                                        bestLocation = l
                                    }
                                }

                                if (bestLocation != null) {
                                    isLocating = false
                                    onOpenDetail(context.getString(R.string.current_location), bestLocation.latitude, bestLocation.longitude)
                                } else {
                                    // Try request single update
                                    val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                        LocationManager.NETWORK_PROVIDER
                                    } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                        LocationManager.GPS_PROVIDER
                                    } else {
                                        null
                                    }

                                    if (provider != null) {
                                        try {
                                            locationManager.requestSingleUpdate(provider, object : LocationListener {
                                                override fun onLocationChanged(location: Location) {
                                                    isLocating = false
                                                    onOpenDetail(context.getString(R.string.current_location), location.latitude, location.longitude)
                                                }
                                                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                                                override fun onProviderEnabled(provider: String) {}
                                                override fun onProviderDisabled(provider: String) {}
                                            }, null)
                                        } catch (e: SecurityException) {
                                            isLocating = false
                                        }
                                    } else {
                                        isLocating = false
                                    }
                                }
                            }
                        },
                        enabled = !isLocating
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = stringResource(R.string.current_location))
                        }
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(context, AuthActivity::class.java))
                    }) {
                        val authMgr = remember { AuthManager.getInstance(context) }
                        val userInfo = authMgr.userInfo
                        if (userInfo?.profilePicture?.isNotEmpty() == true) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(userInfo.profilePicture)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.account_nav_desc),
                                modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            )
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.account_nav_desc))
                        }
                    }
                    IconButton(onClick = onOpenDonate) {
                        Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.donate_nav_desc), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_nav_desc))
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val authManager = remember { AuthManager.getInstance(context) }
            val isAuthenticated = authManager.isAuthenticated
            
            if (!isAuthenticated) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Cloud Sync verfügbar", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Melde dich an, um deine Standorte zu sichern.", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { context.startActivity(Intent(context, AuthActivity::class.java)) }) {
                            Text("Login")
                        }
                    }
                }
            }

            if (locations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_locations_msg),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(locations) { loc ->
                        val sharedPrefs = LocalContext.current.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
                        val tempUnit = sharedPrefs.getString("temp_unit", "celsius") ?: "celsius"
                        
                        ListItem(
                            leadingContent = {
                                loc.currentWeatherCode?.let { code ->
                                    Icon(
                                        painter = painterResource(id = WeatherIconMapper.getIcon(code, loc.provider, loc.isDay)),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color.Unspecified
                                    )
                                }
                            },
                            headlineContent = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(loc.name)
                                    loc.currentTemp?.let { temp ->
                                        val displayTemp = if (tempUnit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()
                                        val suffix = if (tempUnit == "fahrenheit") "°F" else "°C"
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "$displayTemp$suffix",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            supportingContent = { Text(stringResource(R.string.coordinates_label, loc.latitude, loc.longitude)) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        viewModel.setDefaultLocation(loc, !loc.isDefault)
                                    }) {
                                        Icon(
                                            if (loc.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = stringResource(if (loc.isDefault) R.string.remove_default else R.string.set_as_default),
                                            tint = if (loc.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { locationToDelete = loc }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.DelLoc), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectLocation(loc)
                                    onOpenDetail(loc.name, loc.latitude, loc.longitude)
                                }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLocationDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onAdd = { name, lat, lon ->
                viewModel.addLocation(name, lat, lon)
                showAddDialog = false
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
                    viewModel.deleteLocation(location)
                    locationToDelete = null
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
    viewModel: LocationsViewModel,
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
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        loading = true
                        results = emptyList()
                        scope.launch {
                            results = viewModel.search(query)
                            loading = false
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
                                supportingContent = { Text(stringResource(R.string.coordinates_label, lat, lon)) },
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
