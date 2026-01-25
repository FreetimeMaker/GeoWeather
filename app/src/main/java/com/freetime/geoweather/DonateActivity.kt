package com.freetime.geoweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.freetime.geoweather.ui.theme.GeoWeatherTheme

class DonateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoWeatherTheme {
                DonateScreen(onBack = { finish() })
            }
        }
    }
}
