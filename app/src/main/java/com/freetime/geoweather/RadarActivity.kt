package com.freetime.geoweather

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freetime.geoweather.R
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class RadarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        val lat = intent.getDoubleExtra("lat", 0.0)
        val lon = intent.getDoubleExtra("lon", 0.0)
        val url = "https://www.windy.com/?$lat,$lon,8"

        setContent {
            val sharedPreferences = remember { getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) }
            val useSystemTheme = sharedPreferences.collectAsState(key = "use_system_theme", defaultValue = true)
            val darkModeEnabled = sharedPreferences.collectAsState(key = "dark_mode_enabled", defaultValue = false)
            val dynamicColor = sharedPreferences.collectAsState(key = "dynamic_color", defaultValue = true)
            val oledBlack = sharedPreferences.collectAsState(key = "oled_black", defaultValue = false)
            val disablePrivateView = sharedPreferences.getBoolean("disable_private_view", false)
            val openExternalBrowser = sharedPreferences.getBoolean("open_external_browser", false)

            val darkTheme = if (useSystemTheme.value) isSystemInDarkTheme() else darkModeEnabled.value

            GeoWeatherTheme(darkTheme = darkTheme, dynamicColor = dynamicColor.value, oledBlack = oledBlack.value) {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text(stringResource(R.string.radar_title)) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_nav_desc))
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    AndroidView(
                        factory = { ctx ->
                            PrivacyWebView(ctx).apply {
                                this.disablePrivateView = disablePrivateView
                                this.openLinksInExternalBrowser = openExternalBrowser
                                settings.javaScriptEnabled = true
                                loadUrl(url)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
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
}
