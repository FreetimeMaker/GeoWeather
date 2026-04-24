package io.github.freetimemaker.geoweather.data

import kotlinx.serialization.Serializable

@Serializable
data class LocationEntity(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val selected: Boolean = false,
    val isDefault: Boolean = false
)
