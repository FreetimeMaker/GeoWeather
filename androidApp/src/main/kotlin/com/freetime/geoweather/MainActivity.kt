package com.freetime.geoweather

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.freetime.geoweather.data.RoomLocationDaoAdapter
import com.freetime.geoweather.data.getDatabase
import com.freetime.geoweather.data.getDatabaseBuilder
import com.freetime.geoweather.data.initContext
import com.freetime.geoweather.network.WeatherApi
import com.freetime.geoweather.ui.App
import com.russhwolf.settings.SharedPreferencesSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initContext(this)
        val db = getDatabase(getDatabaseBuilder())
        val api = WeatherApi()
        
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val settings = SharedPreferencesSettings(sharedPreferences)
        val settingsManager = SettingsManager(settings)
        val locationService = createLocationService()
        val locationDao = RoomLocationDaoAdapter(db.locationDao())
        
        setContent {
            App(locationDao, api, settingsManager, locationService)
        }
    }
}
