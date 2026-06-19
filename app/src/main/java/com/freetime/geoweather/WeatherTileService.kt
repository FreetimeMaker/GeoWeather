package com.freetime.geoweather

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.freetime.geoweather.data.LocationDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class WeatherTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    private fun updateTile() {
        val db = LocationDatabase.getDatabase(this)
        serviceScope.launch(Dispatchers.IO) {
            val location = db.locationDao().getSelectedLocation() ?: db.locationDao().getDefaultLocation()
            val sharedPreferences = getSharedPreferences("geo_weather_prefs", MODE_PRIVATE)
            val tempUnit = sharedPreferences.getString("temp_unit", "celsius") ?: "celsius"

            if (location?.weatherData != null) {
                try {
                    val obj = JSONObject(location.weatherData!!)
                    val current = obj.optJSONObject("current") ?: obj.optJSONObject("current_weather")
                    val temp = current?.optDouble("temperature_2m") ?: current?.optDouble("temperature") ?: 0.0
                    val code = current?.optInt("weather_code") ?: current?.optInt("weathercode") ?: 0

                    val displayTemp = if (tempUnit == "fahrenheit") (temp * 9/5 + 32).toInt() else temp.toInt()
                    val suffix = if (tempUnit == "fahrenheit") "°F" else "°C"

                    launch(Dispatchers.Main) {
                        qsTile?.apply {
                            label = "${location.name}: $displayTemp$suffix"
                            subtitle = WeatherCodes.getDescription(code, this@WeatherTileService)
                            state = Tile.STATE_ACTIVE
                            updateTile()
                        }
                    }
                } catch (e: Exception) {
                    setInactiveState()
                }
            } else {
                setInactiveState()
            }
        }
    }

    private fun setInactiveState() {
        CoroutineScope(Dispatchers.Main).launch {
            qsTile?.apply {
                state = Tile.STATE_INACTIVE
                label = getString(R.string.app_name)
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        // Open the app when clicked
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }
}
