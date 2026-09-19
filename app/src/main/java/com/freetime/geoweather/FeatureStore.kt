package com.freetime.geoweather

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class TripPlan(val name: String, val locationNames: List<String>, val startDate: String = "", val endDate: String = "")
@Serializable data class LocationGroup(val name: String, val locationNames: List<String>)
@Serializable data class WidgetPreferences(val locationName: String? = null, val mode: String = "current", val forecastHours: Int = 5, val transparent: Boolean = false)
@Serializable data class SyncV3Location(val syncId: String, val name: String, val latitude: Double, val longitude: Double, val updatedAt: Long, val deletedAt: Long? = null)

class FeatureStore(context: Context) {
    private val prefs = context.getSharedPreferences("geoweather_features", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun trips(): List<TripPlan> = decode("trips")
    fun saveTrips(value: List<TripPlan>) = encode("trips", value)
    fun groups(): List<LocationGroup> = decode("groups")
    fun saveGroups(value: List<LocationGroup>) = encode("groups", value)
    fun widgetPreferences(): WidgetPreferences = runCatching { json.decodeFromString<WidgetPreferences>(prefs.getString("widget", null) ?: "{}") }.getOrDefault(WidgetPreferences())
    fun saveWidgetPreferences(value: WidgetPreferences) { prefs.edit().putString("widget", json.encodeToString(value)).apply() }
    fun morningBriefingEnabled(): Boolean = prefs.getBoolean("morning_briefing", false)
    fun eveningBriefingEnabled(): Boolean = prefs.getBoolean("evening_briefing", false)
    fun setBriefings(morning: Boolean, evening: Boolean) { prefs.edit().putBoolean("morning_briefing", morning).putBoolean("evening_briefing", evening).apply() }
    fun featureEnabled(name: String, default: Boolean = true): Boolean = prefs.getBoolean("flag_$name", default)
    fun setFeatureEnabled(name: String, enabled: Boolean) { prefs.edit().putBoolean("flag_$name", enabled).apply() }

    private inline fun <reified T> decode(key: String): List<T> = runCatching {
        json.decodeFromString<List<T>>(prefs.getString(key, "[]") ?: "[]")
    }.getOrDefault(emptyList())
    private inline fun <reified T> encode(key: String, value: List<T>) { prefs.edit().putString(key, json.encodeToString(value)).apply() }
}
