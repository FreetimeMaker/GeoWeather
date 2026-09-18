package com.freetime.geoweather

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.freetime.geoweather.data.DependencyManager
import kotlinx.coroutines.runBlocking

class WeatherTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        runCatching {
            val repository = DependencyManager.getRepository()
            val settings = DependencyManager.getAppSettings()
            val location = runBlocking { repository.getSelectedLocation() }
            if (location != null) {
                tile.label = location.name
                tile.subtitle = repository.getDisplayTemp(location, settings.tempUnit.value)
                tile.state = Tile.STATE_ACTIVE
            } else {
                tile.label = getString(R.string.app_name)
                tile.subtitle = getString(R.string.no_location_selected)
                tile.state = Tile.STATE_INACTIVE
            }
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }
}
