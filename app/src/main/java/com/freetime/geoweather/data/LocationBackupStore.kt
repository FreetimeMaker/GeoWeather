package com.freetime.geoweather.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Keeps a backup-friendly copy of user-created locations outside the Room database.
 *
 * The Room database also stores cached weather and forecast history, so backing up the
 * complete database would restore stale/rebuildable API data. This store contains only
 * durable location configuration and is included in Android Auto Backup/device transfer.
 */
object LocationBackupStore {
    private const val PREFS = "location_backup"
    private const val KEY_LOCATIONS = "locations"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    internal data class BackupLocation(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val notificationsEnabled: Boolean,
        val notificationTime: String,
        val changeAlertsEnabled: Boolean,
        val changeAlertInterval: String,
        val selected: Boolean,
        val isDefault: Boolean,
        val sortOrder: Int,
        val offlinePackEnabled: Boolean
    )

    suspend fun restoreIfNeeded(context: Context, dao: LocationDao) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCATIONS, null) ?: return
        val existing = dao.getAllLocationsSync()
        val missing = locationsToRestore(raw, existing)
            .map {
                LocationEntity(
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    weatherData = null,
                    lastUpdated = 0,
                    notificationsEnabled = it.notificationsEnabled,
                    notificationTime = it.notificationTime,
                    changeAlertsEnabled = it.changeAlertsEnabled,
                    changeAlertInterval = it.changeAlertInterval,
                    selected = it.selected,
                    isDefault = it.isDefault,
                    sortOrder = it.sortOrder,
                    offlinePackEnabled = it.offlinePackEnabled
                )
            }
        if (missing.isNotEmpty()) dao.insertLocations(missing)
    }

    internal fun locationsToRestore(raw: String, existing: List<LocationEntity>): List<BackupLocation> {
        val saved = runCatching { json.decodeFromString<List<BackupLocation>>(raw) }.getOrNull()
            ?: return emptyList()
        val existingKeys = existing.map { coordinateKey(it.latitude, it.longitude) }.toHashSet()
        val seen = existingKeys.toMutableSet()
        return saved.filter { seen.add(coordinateKey(it.latitude, it.longitude)) }
    }

    internal fun encodeLocations(locations: List<LocationEntity>): String = json.encodeToString(locations.map {
        BackupLocation(
            name = it.name,
            latitude = it.latitude,
            longitude = it.longitude,
            notificationsEnabled = it.notificationsEnabled,
            notificationTime = it.notificationTime,
            changeAlertsEnabled = it.changeAlertsEnabled,
            changeAlertInterval = it.changeAlertInterval,
            selected = it.selected,
            isDefault = it.isDefault,
            sortOrder = it.sortOrder,
            offlinePackEnabled = it.offlinePackEnabled
        )
    })

    suspend fun sync(context: Context, dao: LocationDao) {
        val locations = dao.getAllLocationsSync()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCATIONS, encodeLocations(locations))
            .apply()
    }

    private fun coordinateKey(latitude: Double, longitude: Double) =
        "$latitude,$longitude"
}
