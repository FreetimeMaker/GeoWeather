package com.freetime.geoweather

import android.content.Context
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.services.TablesDB
import com.freetime.geoweather.data.AppSettings
import com.freetime.geoweather.data.LocationEntity
import com.freetime.geoweather.data.WeatherRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
private data class SyncedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notificationsEnabled: Boolean,
    val notificationTime: String,
    val changeAlertsEnabled: Boolean,
    val changeAlertInterval: String,
    val selected: Boolean,
    val isDefault: Boolean,
    val sortOrder: Int
)

@Serializable
private data class SyncPayload(
    val tempUnit: String,
    val windUnit: String,
    val pressureUnit: String,
    val useSystemTheme: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val dynamicColor: Boolean = true,
    val oledBlack: Boolean = false,
    val persistentNotif: Boolean,
    val tempThreshold: Int,
    val windThreshold: Int,
    val disablePrivateView: Boolean = false,
    val openExternalBrowser: Boolean,
    val weatherAnimations: String,
    val locations: List<SyncedLocation>
)

object AppwriteSync {
    private const val DATABASE_ID = "geoweather"
    private const val TABLE_ID = "geoweather_sync"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun push(context: Context, repository: WeatherRepository, settings: AppSettings) {
        val user = AppwriteAuth.currentUser(context)
        val payload = SyncPayload(
            tempUnit = settings.tempUnit.value,
            windUnit = settings.windUnit.value,
            pressureUnit = settings.pressureUnit.value,
            useSystemTheme = settings.useSystemTheme.value,
            darkModeEnabled = settings.darkModeEnabled.value,
            dynamicColor = settings.dynamicColor.value,
            oledBlack = settings.oledBlack.value,
            persistentNotif = settings.persistentNotif.value,
            tempThreshold = settings.tempThreshold.value,
            windThreshold = settings.windThreshold.value,
            disablePrivateView = settings.disablePrivateView.value,
            openExternalBrowser = settings.openExternalBrowser.value,
            weatherAnimations = settings.weatherAnimations.value,
            locations = repository.getAllLocationsSync().map {
                SyncedLocation(
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    notificationsEnabled = it.notificationsEnabled,
                    notificationTime = it.notificationTime,
                    changeAlertsEnabled = it.changeAlertsEnabled,
                    changeAlertInterval = it.changeAlertInterval,
                    selected = it.selected,
                    isDefault = it.isDefault,
                    sortOrder = it.sortOrder
                )
            }
        )
        val service = TablesDB(AppwriteAuth.client(context))
        val existing = service.listRows(
            databaseId = DATABASE_ID,
            tableId = TABLE_ID,
            queries = listOf(Query.equal("user_id", user.id), Query.limit(1))
        ).rows.firstOrNull()
        val data = mapOf(
            "user_id" to user.id,
            "payload" to json.encodeToString(payload),
            "updated_at" to Instant.now().toString()
        )
        if (existing == null) {
            service.createRow(
                databaseId = DATABASE_ID,
                tableId = TABLE_ID,
                rowId = ID.unique(),
                data = data,
                permissions = listOf(
                    Permission.read(Role.user(user.id)),
                    Permission.update(Role.user(user.id)),
                    Permission.delete(Role.user(user.id))
                )
            )
        } else {
            service.updateRow(DATABASE_ID, TABLE_ID, existing.id, data)
        }
    }

    suspend fun pull(context: Context, repository: WeatherRepository, settings: AppSettings): Boolean {
        val user = AppwriteAuth.currentUser(context)
        val row = TablesDB(AppwriteAuth.client(context)).listRows(
            databaseId = DATABASE_ID,
            tableId = TABLE_ID,
            queries = listOf(Query.equal("user_id", user.id), Query.limit(1))
        ).rows.firstOrNull() ?: return false
        val payloadText = row.data["payload"]?.toString() ?: return false
        val payload = json.decodeFromString<SyncPayload>(payloadText)

        settings.setTempUnit(payload.tempUnit)
        settings.setWindUnit(payload.windUnit)
        settings.setPressureUnit(payload.pressureUnit)
        settings.setUseSystemTheme(payload.useSystemTheme)
        settings.setDarkModeEnabled(payload.darkModeEnabled)
        settings.setDynamicColor(payload.dynamicColor)
        settings.setOledBlack(payload.oledBlack)
        settings.setPersistentNotif(payload.persistentNotif)
        settings.setTempThreshold(payload.tempThreshold)
        settings.setWindThreshold(payload.windThreshold)
        settings.setDisablePrivateView(payload.disablePrivateView)
        settings.setOpenExternalBrowser(payload.openExternalBrowser)
        settings.setWeatherAnimations(payload.weatherAnimations)

        repository.importBackupLocations(payload.locations.map {
            LocationEntity(
                name = it.name,
                latitude = it.latitude,
                longitude = it.longitude,
                notificationsEnabled = it.notificationsEnabled,
                notificationTime = it.notificationTime,
                changeAlertsEnabled = it.changeAlertsEnabled,
                changeAlertInterval = it.changeAlertInterval,
                selected = it.selected,
                isDefault = it.isDefault,
                sortOrder = it.sortOrder
            )
        })
        return true
    }
}
