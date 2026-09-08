package com.freetime.geoweather.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

const val BACKUP_FILE_NAME = "geoweather_backup.json"
const val BACKUP_MIME_TYPE = "application/json"

/**
 * Opens the platform file picker to save [content] as [fileName].
 * @return true when the file was written, false on cancel or error.
 */
expect suspend fun saveTextFile(fileName: String, mimeType: String, content: String): Boolean

/**
 * Opens the platform file picker to load a text file.
 * @return the file content, or null on cancel or error.
 */
expect suspend fun loadTextFile(mimeTypes: Array<String>): String?

/**
 * Serializes locations into the v2.3.0 backup format:
 * `{"locations": [{"name","lat","lon","notif_enabled","notif_time",
 * "alert_enabled","alert_interval","is_default"}], "version": 1}`
 * so old backups stay readable and new ones stay compatible.
 */
fun exportLocationsJson(locations: List<LocationEntity>): String {
    val json = Json { prettyPrint = true }
    return json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("locations", buildJsonArray {
                locations.forEach { loc ->
                    addJsonObject {
                        put("name", loc.name)
                        put("lat", loc.latitude)
                        put("lon", loc.longitude)
                        put("notif_enabled", loc.notificationsEnabled)
                        put("notif_time", loc.notificationTime)
                        put("alert_enabled", loc.changeAlertsEnabled)
                        put("alert_interval", loc.changeAlertInterval)
                        put("is_default", loc.isDefault)
                    }
                }
            })
            put("version", 1)
        }
    )
}

/**
 * Parses a backup created by [exportLocationsJson] (or v2.3.0).
 * Malformed entries are skipped, a missing `locations` array throws.
 */
fun parseLocationsBackup(content: String): List<LocationEntity> {
    val root = Json.parseToJsonElement(content).jsonObject
    val array = root["locations"]?.jsonArray
        ?: throw IllegalArgumentException("Backup contains no locations array")
    return array.mapNotNull { element ->
        try {
            val obj = element.jsonObject
            LocationEntity(
                name = obj["name"]!!.jsonPrimitive.content,
                latitude = obj["lat"]!!.jsonPrimitive.double,
                longitude = obj["lon"]!!.jsonPrimitive.double,
                notificationsEnabled = obj["notif_enabled"]?.jsonPrimitive?.booleanOrNull ?: false,
                notificationTime = obj["notif_time"]?.jsonPrimitive?.contentOrNull ?: "08:00",
                changeAlertsEnabled = obj["alert_enabled"]?.jsonPrimitive?.booleanOrNull ?: false,
                changeAlertInterval = obj["alert_interval"]?.jsonPrimitive?.contentOrNull ?: "3",
                isDefault = obj["is_default"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            null
        }
    }
}
