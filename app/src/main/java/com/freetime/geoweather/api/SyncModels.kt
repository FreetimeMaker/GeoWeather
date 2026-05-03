package com.freetime.geoweather.api

import kotlinx.serialization.Serializable

@Serializable
data class LocationSyncRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val notificationsEnabled: Boolean = false,
    val notificationTime: String = "08:00",
    val changeAlertsEnabled: Boolean = false,
    val changeAlertInterval: String = "3",
    val isDefault: Boolean = false
)

@Serializable
data class LocationSyncResponse(
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class LocationsSyncListRequest(
    val locations: List<LocationSyncRequest>
)
