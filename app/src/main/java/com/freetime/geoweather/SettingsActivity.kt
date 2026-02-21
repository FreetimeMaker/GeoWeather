package com.freetime.geoweather

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class SettingsActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE)
        hideSystemUI()
        
        setContent {
            val darkTheme = isSystemInDarkTheme()
            GeoWeatherTheme(darkTheme = darkTheme) {
                SettingsScreen(onBack = { finish() })
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
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = remember { 
        context.getSharedPreferences("geo_weather_prefs", Context.MODE_PRIVATE) 
    }
    
    var darkModeEnabled by remember {
        mutableStateOf(sharedPreferences.getBoolean("dark_mode_enabled", false))
    }
    
    var useSystemTheme by remember {
        mutableStateOf(sharedPreferences.getBoolean("use_system_theme", true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(onClick = onBack) {
                Text("← Back")
            }
        }
        
        // Dark Mode Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Theme Settings",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                // Use System Theme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Follow System Theme")
                    Switch(
                        checked = useSystemTheme,
                        onCheckedChange = { enabled ->
                            useSystemTheme = enabled
                            sharedPreferences.edit()
                                .putBoolean("use_system_theme", enabled)
                                .apply()
                        }
                    )
                }
                
                // Force Dark Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Force Dark Mode")
                        Text(
                            text = "Override system setting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = darkModeEnabled,
                        enabled = !useSystemTheme,
                        onCheckedChange = { enabled ->
                            darkModeEnabled = enabled
                            sharedPreferences.edit()
                                .putBoolean("dark_mode_enabled", enabled)
                                .apply()
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}
