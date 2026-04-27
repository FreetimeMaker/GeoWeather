package io.github.freetimemaker.geoweather

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import io.github.freetimemaker.geoweather.data.RoomLocationDaoAdapter
import io.github.freetimemaker.geoweather.data.getDatabase
import io.github.freetimemaker.geoweather.data.getDatabaseBuilder
import io.github.freetimemaker.geoweather.data.initContext
import io.github.freetimemaker.geoweather.network.WeatherApi
import io.github.freetimemaker.geoweather.ui.App
import com.russhwolf.settings.SharedPreferencesSettings

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initContext(this)

        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

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
