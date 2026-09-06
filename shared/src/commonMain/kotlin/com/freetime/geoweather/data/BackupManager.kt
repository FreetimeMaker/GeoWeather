package com.freetime.geoweather.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object BackupManager {
    fun exportLocations(locations: List<LocationEntity>): String {
        val payload = buildJsonObject {
            put("version", 1)
            put("locations", buildJsonArray {
                locations.forEach { location ->
                    add(buildJsonObject {
                        put("name", location.name)
                        put("latitude", location.latitude)
                        put("longitude", location.longitude)
                        put("selected", location.selected)
                        put("isDefault", location.isDefault)
                    })
                }
            })
        }
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), payload)
    }

    fun importLocations(raw: String): List<LocationEntity> {
        val root = Json.parseToJsonElement(raw).jsonObject
        return root["locations"]?.jsonArray?.mapNotNull { element ->
            val item = element.jsonObject
            val name = item["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val latitude = item["latitude"]?.jsonPrimitive?.double ?: return@mapNotNull null
            val longitude = item["longitude"]?.jsonPrimitive?.double ?: return@mapNotNull null
            LocationEntity(
                name = name,
                latitude = latitude,
                longitude = longitude,
                selected = item["selected"]?.jsonPrimitive?.booleanOrNull ?: false,
                isDefault = item["isDefault"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        } ?: emptyList()
    }
}
