package com.freetime.geoweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.DependencyManager
import com.freetime.geoweather.ui.WeatherViewModel
import com.freetime.geoweather.ui.glass.GeoWeatherGlassRoot
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

abstract class GeoWeatherActivity : ComponentActivity() {
    protected val repository get() = DependencyManager.getRepository()
    protected val appSettings: AppSettings get() = DependencyManager.getAppSettings()
    protected val weatherViewModel by lazy { WeatherViewModel(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setGeoWeatherContent(content: @Composable () -> Unit) {
        setContent {
            GeoWeatherTheme {
                GeoWeatherGlassRoot {
                    Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
                        Box(Modifier.fillMaxSize()) { content() }
                    }
                }
            }
        }
    }
}
