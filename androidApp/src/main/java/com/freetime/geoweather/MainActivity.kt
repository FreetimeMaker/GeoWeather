package com.freetime.geoweather

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.freetime.geoweather.data.DependencyManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            WeatherApp(
                database = DependencyManager.getDatabase(),
                appSettings = DependencyManager.getAppSettings()
            )
        }
    }
}
