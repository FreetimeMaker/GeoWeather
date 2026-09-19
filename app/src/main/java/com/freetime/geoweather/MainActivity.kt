package com.freetime.geoweather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.data.onCreateDocumentResult
import com.freetime.geoweather.data.onOpenDocumentResult
import com.freetime.geoweather.data.registerFilePickers
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.freetime.geoweather.ui.glass.LocalGeoWeatherBackdrop
import com.freetime.geoweather.ui.glass.geoWeatherBackdropSource
import com.freetime.geoweather.ui.glass.rememberGeoWeatherBackdrop

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }
    private val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkNotificationPermission()
        checkLocationPermission()

        val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json"), ::onCreateDocumentResult)
        val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument(), ::onOpenDocumentResult)
        registerFilePickers(createDocument, openDocument)

        onBackPressedDispatcher.addCallback(this) {
            if (systemBackHandler?.invoke() != true) finish()
        }

        setContent {
            val backdrop = rememberGeoWeatherBackdrop()
            CompositionLocalProvider(LocalGeoWeatherBackdrop provides backdrop) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .geoWeatherBackdropSource(backdrop)
                ) {
                    var authState by remember { mutableStateOf<Boolean?>(null) }
                    LaunchedEffect(Unit) {
                        authState = AppwriteAuth.hasSession(this@MainActivity)
                    }
                    when (authState) {
                        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        false -> AuthScreen(onAuthenticated = { authState = true })
                        true -> WeatherApp(
                            database = DependencyManager.getDatabase(),
                            appSettings = DependencyManager.getAppSettings()
                        )
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}
