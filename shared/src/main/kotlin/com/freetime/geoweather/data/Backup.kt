package com.freetime.geoweather.data

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.freetime.geoweather.getAndroidAppContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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

private var createLauncher: ActivityResultLauncher<String>? = null
private var openLauncher: ActivityResultLauncher<Array<String>>? = null
private var createContinuation: Continuation<Boolean>? = null
private var createContent: String? = null
private var openContinuation: Continuation<String?>? = null

fun registerFilePickers(
    createDocument: ActivityResultLauncher<String>,
    openDocument: ActivityResultLauncher<Array<String>>
) {
    createLauncher = createDocument
    openLauncher = openDocument
}

fun onCreateDocumentResult(uri: Uri?) {
    val cont = createContinuation
    createContinuation = null
    val content = createContent
    createContent = null
    if (cont == null) return
    if (uri == null || content == null) {
        cont.resume(false)
        return
    }
    try {
        getAndroidAppContext()?.contentResolver?.openOutputStream(uri)?.use {
            it.write(content.toByteArray())
        }
        cont.resume(true)
    } catch (_: Exception) {
        cont.resume(false)
    }
}

fun onOpenDocumentResult(uri: Uri?) {
    val cont = openContinuation
    openContinuation = null
    if (cont == null) return
    if (uri == null) {
        cont.resume(null)
        return
    }
    try {
        val text = getAndroidAppContext()?.contentResolver
            ?.openInputStream(uri)?.bufferedReader()?.readText()
        cont.resume(text)
    } catch (_: Exception) {
        cont.resume(null)
    }
}

suspend fun saveTextFile(fileName: String, mimeType: String, content: String): Boolean =
    suspendCoroutine { cont ->
        val launcher = createLauncher
        if (launcher == null) {
            cont.resume(false)
            return@suspendCoroutine
        }
        createContinuation = cont
        createContent = content
        launcher.launch(fileName)
    }

suspend fun loadTextFile(mimeTypes: Array<String>): String? =
    suspendCoroutine { cont ->
        val launcher = openLauncher
        if (launcher == null) {
            cont.resume(null)
            return@suspendCoroutine
        }
        openContinuation = cont
        launcher.launch(mimeTypes)
    }

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
        } catch (_: Exception) {
            null
        }
    }
}
